package com.flowable.wrapper.controller;

import com.flowable.wrapper.cerbos.service.CerbosService;
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
import org.springframework.http.HttpStatus;
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
    
    private static final String USER_ID_HEADER = "X-User-Id";
    
    private final TaskService taskService;
    private final CerbosService cerbosService;
    
    /**
     * Extract and validate user ID from HTTP request header
     * @param request HTTP request
     * @return validated user ID
     * @throws RuntimeException if user ID is missing or invalid
     */
    private String validateAndExtractUserId(HttpServletRequest request) {
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId == null || userId.trim().isEmpty()) {
            log.error("Missing or empty user ID in request header: {}", USER_ID_HEADER);
            throw new RuntimeException("Missing or invalid user ID");
        }
        return userId.trim();
    }
    
    @GetMapping("/queue/{queueName}")
    @Operation(summary = "Get tasks by queue", 
              description = "Retrieve all open tasks from a specific queue")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access to queue"),
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
        
        String userId = validateAndExtractUserId(httpRequest);
        log.info("Getting tasks for queue: {} in business app: {}, unassignedOnly: {} by user: {}", 
                queueName, businessAppName, unassignedOnly, userId);
        
        // Authorization check for queue access
        boolean isAuthorized = cerbosService.canAccessQueue(userId, businessAppName, queueName);
        if (!isAuthorized) {
            log.warn("User {} unauthorized to access queue: {} in business app: {}", userId, queueName, businessAppName);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<QueueTaskResponse> tasks = taskService.getTasksByQueue(queueName, unassignedOnly);
        
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/my-tasks")
    @Operation(summary = "Get my tasks", 
              description = "Retrieve all tasks assigned to the current user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access to business app")
    })
    public ResponseEntity<List<QueueTaskResponse>> getMyTasks(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            HttpServletRequest httpRequest) {
        
        String userId = validateAndExtractUserId(httpRequest);
        log.info("Getting tasks for user: {} in business app: {}", userId, businessAppName);
        
        // For my-tasks, we use a generic view_task authorization with business app context
        // This checks if user has any task-related roles in this business app
        boolean isAuthorized = cerbosService.isAuthorized(userId, CerbosService.ACTION_VIEW_TASK, businessAppName, businessAppName, null);
        if (!isAuthorized) {
            log.warn("User {} unauthorized to view tasks in business app: {}", userId, businessAppName);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<QueueTaskResponse> tasks = taskService.getTasksByAssignee(userId);
        
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/{taskId}")
    @Operation(summary = "Get task details", 
              description = "Retrieve detailed task information including form data")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task details retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access to task"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskDetailResponse> getTaskDetails(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId,
            HttpServletRequest httpRequest) throws WorkflowException {
        
        String userId = validateAndExtractUserId(httpRequest);
        log.info("Getting task details for task: {} in business app: {} by user: {}", 
                taskId, businessAppName, userId);
        
        // Generic task authorization - let Cerbos policies determine access patterns
        boolean isAuthorized = cerbosService.isAuthorizedForTask(userId, CerbosService.ACTION_VIEW_TASK, businessAppName, taskId);
        if (!isAuthorized) {
            log.warn("User {} unauthorized to view task: {} in business app: {}", userId, taskId, businessAppName);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        TaskDetailResponse taskDetails = taskService.getTaskDetails(taskId);
        return ResponseEntity.ok(taskDetails);
    }
    
    @PostMapping("/{taskId}/claim")
    @Operation(summary = "Claim a task", 
              description = "Claim an unassigned task")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task claimed successfully"),
        @ApiResponse(responseCode = "400", description = "Task already assigned"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to claim task"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<QueueTaskResponse> claimTask(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId,
            HttpServletRequest httpRequest) throws WorkflowException {
        
        String userId = validateAndExtractUserId(httpRequest);
        log.info("User {} claiming task: {} in business app: {}", userId, taskId, businessAppName);
        
        // Generic task authorization - policies determine business rules (Four-Eyes, queues, etc.)
        boolean isAuthorized = cerbosService.isAuthorizedForTask(userId, CerbosService.ACTION_CLAIM_TASK, businessAppName, taskId);
        if (!isAuthorized) {
            log.warn("User {} unauthorized to claim task: {} in business app: {}", userId, taskId, businessAppName);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        QueueTaskResponse task = taskService.claimTask(taskId, userId);
        return ResponseEntity.ok(task);
    }
    
    @PostMapping("/{taskId}/complete")
    @Operation(summary = "Complete a task", 
              description = "Complete a task with optional variables")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "User not authorized to complete this task"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskCompletionResponse> completeTask(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId,
            @Valid @RequestBody(required = false) CompleteTaskRequest request,
            HttpServletRequest httpRequest) throws WorkflowException {
        
        String userId = validateAndExtractUserId(httpRequest);
        log.info("Completing task: {} in business app: {} by user: {}", taskId, businessAppName, userId);
        
        // Generic task authorization - policies determine business rules (Four-Eyes, assignment, etc.)
        boolean isAuthorized = cerbosService.isAuthorizedForTask(userId, CerbosService.ACTION_COMPLETE_TASK, businessAppName, taskId);
        if (!isAuthorized) {
            log.warn("User {} unauthorized to complete task: {} in business app: {}", userId, taskId, businessAppName);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        TaskCompletionResponse response = taskService.completeTask(taskId, userId, request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{taskId}/unclaim")
    @Operation(summary = "Unclaim a task", 
              description = "Release a claimed task back to the queue")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Task unclaimed successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized to unclaim task"),
        @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<QueueTaskResponse> unclaimTask(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId,
            HttpServletRequest httpRequest) throws WorkflowException {
        
        String userId = validateAndExtractUserId(httpRequest);
        log.info("Unclaiming task: {} in business app: {} by user: {}", taskId, businessAppName, userId);
        
        // Generic task authorization - policies determine ownership and unclaim rules
        boolean isAuthorized = cerbosService.isAuthorizedForTask(userId, CerbosService.ACTION_UNCLAIM_TASK, businessAppName, taskId);
        if (!isAuthorized) {
            log.warn("User {} unauthorized to unclaim task: {} in business app: {}", userId, taskId, businessAppName);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        QueueTaskResponse task = taskService.unclaimTask(taskId);
        return ResponseEntity.ok(task);
    }
    
    @GetMapping("/queue/{queueName}/next")
    @Operation(summary = "Get next available task from queue", 
              description = "Retrieve the next unassigned task from a specific queue (highest priority, oldest first)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Next task retrieved successfully"),
        @ApiResponse(responseCode = "204", description = "No available tasks in queue"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access to queue"),
        @ApiResponse(responseCode = "404", description = "Queue not found")
    })
    public ResponseEntity<QueueTaskResponse> getNextTaskFromQueue(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            @Parameter(description = "Queue name", required = true)
            @PathVariable String queueName,
            HttpServletRequest httpRequest) {
        
        String userId = validateAndExtractUserId(httpRequest);
        log.info("Getting next available task from queue: {} in business app: {} by user: {}", 
                queueName, businessAppName, userId);
        
        // Authorization check for queue access (similar to view_queue)
        boolean isAuthorized = cerbosService.canAccessQueue(userId, businessAppName, queueName);
        if (!isAuthorized) {
            log.warn("User {} unauthorized to access queue: {} in business app: {}", userId, queueName, businessAppName);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        QueueTaskResponse nextTask = taskService.getNextTaskFromQueue(queueName);
        
        if (nextTask == null) {
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.ok(nextTask);
    }
}