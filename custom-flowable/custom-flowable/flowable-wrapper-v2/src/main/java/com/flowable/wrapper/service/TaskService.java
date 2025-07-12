package com.flowable.wrapper.service;

import com.flowable.wrapper.dto.request.CompleteTaskRequest;
import com.flowable.wrapper.dto.response.QueueTaskResponse;
import com.flowable.wrapper.dto.response.TaskCompletionResponse;
import com.flowable.wrapper.dto.response.TaskDetailResponse;
import com.flowable.wrapper.exception.WorkflowException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskService {
    
    private final org.flowable.engine.TaskService flowableTaskService;
    private final RuntimeService runtimeService;
    private final QueueTaskService queueTaskService;
    
    /**
     * Get tasks by queue name
     */
    public List<QueueTaskResponse> getTasksByQueue(String queueName, boolean unassignedOnly) {
        return queueTaskService.getTasksByQueue(queueName, unassignedOnly);
    }
    
    /**
     * Get tasks by assignee
     */
    public List<QueueTaskResponse> getTasksByAssignee(String userId) {
        return queueTaskService.getTasksByAssignee(userId);
    }
    
    /**
     * Claim a task
     */
    public QueueTaskResponse claimTask(String taskId, String userId) throws WorkflowException {
        // Check if task exists in queue_tasks
        QueueTaskResponse queueTask = queueTaskService.getQueueTask(taskId);
        
        if (queueTask.getAssignee() != null) {
            throw new WorkflowException("TASK_ALREADY_ASSIGNED", 
                "Task is already assigned to: " + queueTask.getAssignee());
        }
        
        // Claim in Flowable
        try {
            flowableTaskService.claim(taskId, userId);
        } catch (Exception e) {
            throw new WorkflowException("CLAIM_FAILED", 
                "Failed to claim task: " + e.getMessage(), e);
        }
        
        // Update queue_tasks table
        return queueTaskService.claimTask(taskId, userId);
    }
    
    /**
     * Complete a task
     */
    public TaskCompletionResponse completeTask(String taskId, CompleteTaskRequest request) throws WorkflowException {
        // Get task from queue_tasks
        QueueTaskResponse queueTask = queueTaskService.getQueueTask(taskId);
        
        // Verify task is assigned
        if (queueTask.getAssignee() == null) {
            throw new WorkflowException("TASK_NOT_ASSIGNED", 
                "Task must be claimed before completion");
        }
        
        // Verify user is authorized (if userId provided)
        if (request != null && request.getUserId() != null && 
            !request.getUserId().equals(queueTask.getAssignee())) {
            throw new WorkflowException("UNAUTHORIZED", 
                "User " + request.getUserId() + " is not authorized to complete this task");
        }
        
        String processInstanceId = queueTask.getProcessInstanceId();
        Map<String, Object> variables = request != null && request.getVariables() != null ? 
            request.getVariables() : new HashMap<>();
        
        // Complete in Flowable
        try {
            flowableTaskService.complete(taskId, variables);
        } catch (Exception e) {
            throw new WorkflowException("COMPLETE_FAILED", 
                "Failed to complete task: " + e.getMessage(), e);
        }
        
        // Update queue_tasks table
        Instant completedAt = Instant.now();
        queueTaskService.completeTask(taskId);
        
        log.info("Task {} completed by user {}", taskId, queueTask.getAssignee());
        
        // Check if process is still active and populate next task
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
                
        TaskCompletionResponse response = TaskCompletionResponse.builder()
                .taskId(taskId)
                .taskName(queueTask.getTaskName())
                .processInstanceId(processInstanceId)
                .completedAt(completedAt)
                .completedBy(queueTask.getAssignee())
                .processActive(processInstance != null)
                .build();
                
        if (processInstance != null) {
            // Process is still active, populate next tasks in queue
            queueTaskService.populateQueueTasksForProcessInstance(
                processInstanceId, 
                queueTask.getProcessDefinitionKey()
            );
            
            // Get the next task info
            List<QueueTaskResponse> nextTasks = queueTaskService.getTasksByProcessInstance(processInstanceId);
            if (!nextTasks.isEmpty()) {
                QueueTaskResponse nextTask = nextTasks.get(0);
                response.setNextTaskId(nextTask.getTaskId());
                response.setNextTaskName(nextTask.getTaskName());
                response.setNextTaskQueue(nextTask.getQueueName());
            }
        }
        
        return response;
    }
    
    /**
     * Unclaim a task
     */
    public QueueTaskResponse unclaimTask(String taskId) throws WorkflowException {
        // Check if task exists
        QueueTaskResponse queueTask = queueTaskService.getQueueTask(taskId);
        
        // Unclaim in Flowable
        try {
            flowableTaskService.unclaim(taskId);
        } catch (Exception e) {
            throw new WorkflowException("UNCLAIM_FAILED", 
                "Failed to unclaim task: " + e.getMessage(), e);
        }
        
        // Update queue_tasks table
        return queueTaskService.unclaimTask(taskId);
    }
    
    /**
     * Get next available task from queue
     */
    public QueueTaskResponse getNextTaskFromQueue(String queueName) {
        return queueTaskService.getNextTaskFromQueue(queueName);
    }
    
    /**
     * Get detailed task information including form data
     */
    public TaskDetailResponse getTaskDetails(String taskId) throws WorkflowException {
        // Get task from queue_tasks
        QueueTaskResponse queueTask = queueTaskService.getQueueTask(taskId);
        
        // Get the Flowable task
        Task flowableTask = flowableTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
                
        if (flowableTask == null) {
            throw new com.flowable.wrapper.exception.ResourceNotFoundException("Task", taskId);
        }
        
        // Build response
        TaskDetailResponse response = TaskDetailResponse.builder()
                .taskId(queueTask.getTaskId())
                .processInstanceId(queueTask.getProcessInstanceId())
                .processDefinitionKey(queueTask.getProcessDefinitionKey())
                .taskDefinitionKey(queueTask.getTaskDefinitionKey())
                .taskName(queueTask.getTaskName())
                .queueName(queueTask.getQueueName())
                .assignee(queueTask.getAssignee())
                .status(queueTask.getStatus())
                .priority(queueTask.getPriority())
                .createdAt(queueTask.getCreatedAt())
                .claimedAt(queueTask.getClaimedAt())
                .taskData(queueTask.getTaskData())
                .businessKey(queueTask.getBusinessKey())
                .formKey(flowableTask.getFormKey())
                .description(flowableTask.getDescription())
                .build();
                
        // Set due date if available
        if (flowableTask.getDueDate() != null) {
            response.setDueDate(flowableTask.getDueDate().toInstant());
        }
        
        // Get task variables (form data)
        Map<String, Object> taskVariables = flowableTaskService.getVariables(taskId);
        response.setFormData(taskVariables);
        
        // Get process variables
        Map<String, Object> processVariables = runtimeService.getVariables(queueTask.getProcessInstanceId());
        response.setProcessVariables(processVariables);
        
        return response;
    }
}