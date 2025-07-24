package com.flowable.wrapper.controller;

import com.flowable.wrapper.dto.request.CompleteTaskRequest;
import com.flowable.wrapper.dto.response.QueueTaskResponse;
import com.flowable.wrapper.dto.response.TaskCompletionResponse;
import com.flowable.wrapper.dto.response.TaskDetailResponse;
import com.flowable.wrapper.exception.WorkflowException;
import com.flowable.wrapper.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
@RequestMapping("/api/{businessAppName}/tasks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tasks", description = "APIs for task management and queue operations")
public class TaskController {
    
    private final TaskService taskService;
    
    @GetMapping("/queue/{queueName}")
    @Operation(summary = "Get tasks by queue", 
              description = "Retrieve all open tasks from a specific queue")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Queue not found")
    })
    public ResponseEntity<List<QueueTaskResponse>> getTasksByQueue(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            @Parameter(description = "Queue name", required = true)
            @PathVariable String queueName,
            @Parameter(description = "Include only unassigned tasks")
            @RequestParam(required = false, defaultValue = "false") boolean unassignedOnly,
            HttpServletRequest httpRequest) {
        
        String userId = httpRequest.getHeader("X-User-Id");
        log.info("Getting tasks for queue: {} in business app: {}, unassignedOnly: {} by user: {}", 
                queueName, businessAppName, unassignedOnly, userId);
        List<QueueTaskResponse> tasks = taskService.getTasksByQueue(queueName, unassignedOnly);
        
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/my-tasks")
    @Operation(summary = "Get my tasks", 
              description = "Retrieve all tasks assigned to the current user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully")
    })
    public ResponseEntity<List<QueueTaskResponse>> getMyTasks(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            HttpServletRequest httpRequest) {
        
        String userId = httpRequest.getHeader("X-User-Id");
        log.info("Getting tasks for user: {} in business app: {}", userId, businessAppName);
        List<QueueTaskResponse> tasks = taskService.getTasksByAssignee(userId);
        
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/{taskId}")
    @Operation(summary = "Get task details", 
              description = "Retrieve detailed task information including form data")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task details retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskDetailResponse> getTaskDetails(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId,
            HttpServletRequest httpRequest) throws WorkflowException {
        
        String userId = httpRequest.getHeader("X-User-Id");
        log.info("Getting task details for task: {} in business app: {} by user: {}", 
                taskId, businessAppName, userId);
        TaskDetailResponse taskDetails = taskService.getTaskDetails(taskId);
        
        return ResponseEntity.ok(taskDetails);
    }
    
    @PostMapping("/{taskId}/claim")
    @Operation(summary = "Claim a task", 
              description = "Claim an unassigned task")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task claimed successfully"),
        @ApiResponse(responseCode = "400", description = "Task already assigned"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<QueueTaskResponse> claimTask(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId,
            HttpServletRequest httpRequest) throws WorkflowException {
        
        String userId = httpRequest.getHeader("X-User-Id");
        log.info("User {} claiming task: {} in business app: {}", userId, taskId, businessAppName);
        QueueTaskResponse task = taskService.claimTask(taskId, userId);
        
        return ResponseEntity.ok(task);
    }
    
    @PostMapping("/{taskId}/complete")
    @Operation(summary = "Complete a task", 
              description = "Complete a task with optional variables")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Task not found"),
        @ApiResponse(responseCode = "403", description = "User not authorized to complete this task")
    })
    public ResponseEntity<TaskCompletionResponse> completeTask(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId,
            @Valid @RequestBody(required = false) CompleteTaskRequest request,
            HttpServletRequest httpRequest) throws WorkflowException {
        
        String userId = httpRequest.getHeader("X-User-Id");
        log.info("Completing task: {} in business app: {} by user: {}", taskId, businessAppName, userId);
        TaskCompletionResponse response = taskService.completeTask(taskId, request);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{taskId}/unclaim")
    @Operation(summary = "Unclaim a task", 
              description = "Release a claimed task back to the queue")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task unclaimed successfully"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<QueueTaskResponse> unclaimTask(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId,
            HttpServletRequest httpRequest) throws WorkflowException {
        
        String userId = httpRequest.getHeader("X-User-Id");
        log.info("Unclaiming task: {} in business app: {} by user: {}", taskId, businessAppName, userId);
        QueueTaskResponse task = taskService.unclaimTask(taskId);
        
        return ResponseEntity.ok(task);
    }
    
    @GetMapping("/queue/{queueName}/next")
    @Operation(summary = "Get next available task from queue", 
              description = "Retrieve the next unassigned task from a specific queue (highest priority, oldest first)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Next task retrieved successfully"),
        @ApiResponse(responseCode = "204", description = "No available tasks in queue"),
        @ApiResponse(responseCode = "404", description = "Queue not found")
    })
    public ResponseEntity<QueueTaskResponse> getNextTaskFromQueue(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            @Parameter(description = "Queue name", required = true)
            @PathVariable String queueName,
            HttpServletRequest httpRequest) {
        
        String userId = httpRequest.getHeader("X-User-Id");
        log.info("Getting next available task from queue: {} in business app: {} by user: {}", 
                queueName, businessAppName, userId);
        QueueTaskResponse nextTask = taskService.getNextTaskFromQueue(queueName);
        
        if (nextTask == null) {
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.ok(nextTask);
    }
}