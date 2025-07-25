package com.flowable.wrapper.cerbos.builder;

import com.flowable.wrapper.cerbos.dto.CerbosResourceContext;
import com.flowable.wrapper.entity.BusinessApplication;
import com.flowable.wrapper.entity.QueueTask;
import com.flowable.wrapper.entity.WorkflowMetadata;
import com.flowable.wrapper.repository.BusinessApplicationRepository;
import com.flowable.wrapper.repository.QueueTaskRepository;
import com.flowable.wrapper.repository.WorkflowMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds Cerbos resource context from process and task data
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CerbosResourceBuilder {
    
    private final RuntimeService flowableRuntimeService;
    private final QueueTaskRepository queueTaskRepository;
    private final WorkflowMetadataRepository workflowMetadataRepository;
    private final BusinessApplicationRepository businessApplicationRepository;
    
    /**
     * Build resource context for existing process instance (with runtime data)
     */
    public CerbosResourceContext buildResourceContext(String processInstanceId, 
                                                     String businessAppName, 
                                                     String taskId) {
        log.debug("Building Cerbos resource context for processInstance: {}, businessApp: {}, task: {}", 
                processInstanceId, businessAppName, taskId);
        
        try {
            // Get process definition key from process instance
            String processDefinitionKey = getProcessDefinitionKey(processInstanceId);
            
            return buildResourceContextInternal(processDefinitionKey, businessAppName, processInstanceId, taskId);
            
        } catch (Exception e) {
            log.error("Failed to build Cerbos resource context: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to build Cerbos resource context", e);
        }
    }
    
    /**
     * Build resource context for process creation (no process instance yet)
     */
    public CerbosResourceContext buildResourceContextForCreation(String processDefinitionKey, 
                                                               String businessAppName) {
        log.debug("Building Cerbos resource context for process creation: processKey={}, businessApp={}", 
                processDefinitionKey, businessAppName);
        
        try {
            return buildResourceContextInternal(processDefinitionKey, businessAppName, null, null);
            
        } catch (Exception e) {
            log.error("Failed to build Cerbos resource context for creation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to build Cerbos resource context for creation", e);
        }
    }
    
    /**
     * Build generic task resource context for any business application
     * This method extracts process context from the task and builds appropriate resource context
     * 
     * @param businessAppName Business application name
     * @param taskId Task ID for context building
     * @return Generic task resource context
     */
    public CerbosResourceContext buildTaskResourceContext(String businessAppName, String taskId) {
        log.debug("Building generic task resource context for businessApp: {}, task: {}", businessAppName, taskId);
        
        try {
            // Get task information to extract process context
            Optional<QueueTask> queueTaskOpt = queueTaskRepository.findByTaskId(taskId);
            if (queueTaskOpt.isEmpty()) {
                log.warn("Queue task not found for taskId: {}", taskId);
                throw new RuntimeException("Task not found: " + taskId);
            }
            
            QueueTask queueTask = queueTaskOpt.get();
            String processInstanceId = queueTask.getProcessInstanceId();
            
            // Get process definition key from the process instance
            String processDefinitionKey = getProcessDefinitionKey(processInstanceId);
            
            // Build resource context with task-specific information
            return buildResourceContextInternal(processDefinitionKey, businessAppName, processInstanceId, taskId);
            
        } catch (Exception e) {
            log.error("Failed to build task resource context for businessApp: {}, task: {}", 
                    businessAppName, taskId, e);
            throw new RuntimeException("Failed to build task resource context", e);
        }
    }
    
    /**
     * Build queue resource context within the workflow context
     * Queue access is part of the same workflow policy, not a separate resource
     * 
     * @param businessAppName Business application name
     * @param queueName Queue name for authorization
     * @return Workflow resource context for queue access
     */
    public CerbosResourceContext buildQueueResourceContextForWorkflow(String businessAppName, String queueName) {
        log.debug("Building queue resource context within workflow for businessApp: {}, queue: {}", 
                businessAppName, queueName);
        
        // Dynamically determine the process definition key from repository
        // This will throw RuntimeException if business app or workflow not found
        String processDefinitionKey = determineProcessDefinitionKeyForBusinessApp(businessAppName);
        
        // Build workflow resource context for queue access
        return buildResourceContextForQueueAccess(processDefinitionKey, businessAppName, queueName);
    }
    
    /**
     * Internal method to build resource context for queue access
     */
    private CerbosResourceContext buildResourceContextForQueueAccess(String processDefinitionKey,
                                                                   String businessAppName, 
                                                                   String queueName) {
        
        // Build resource kind for Cerbos policy targeting - same as workflow
        String resourceKind = businessAppName + "::" + processDefinitionKey;
        
        // Build resource ID for queue access
        String resourceId = businessAppName + "::" + queueName;
        
        // Get business app metadata
        Map<String, Object> businessAppMetadata = getBusinessAppMetadata(businessAppName);
        
        // Get workflow metadata (without adding queue-specific data)
        Map<String, Object> workflowMetadata = getWorkflowMetadata(processDefinitionKey, businessAppName);
        
        // Build resource context for queue access
        CerbosResourceContext.CerbosResourceAttributes attributes = CerbosResourceContext.CerbosResourceAttributes.builder()
                .businessApp(businessAppName)
                .businessAppMetadata(businessAppMetadata)
                .processDefinitionKey(processDefinitionKey)
                .processInstanceId(null) // No specific process instance for queue access
                .currentTask(null) // No current task for queue access
                .processVariables(new HashMap<>()) // No process variables for queue access
                .taskStates(new HashMap<>()) // No task states for queue access
                .workflowMetadata(workflowMetadata) // Workflow metadata without queue-specific data
                .currentQueue(queueName) // Set the specific queue being accessed
                .build();
        
        CerbosResourceContext resourceContext = CerbosResourceContext.builder()
                .kind(resourceKind)
                .id(resourceId)
                .attr(attributes)
                .build();
        
        log.debug("Built queue resource context: kind={}, id={}, queue={}", resourceKind, resourceId, queueName);
        return resourceContext;
    }
    
    /**
     * Internal method to build resource context
     */
    private CerbosResourceContext buildResourceContextInternal(String processDefinitionKey,
                                                             String businessAppName, 
                                                             String processInstanceId, 
                                                             String taskId) {
        
        // Build resource kind for Cerbos policy targeting
        String resourceKind = businessAppName + "::" + processDefinitionKey;
        
        // Build resource ID for correlation/logging - use processInstance if available
        String resourceId = processInstanceId != null ? processInstanceId : processDefinitionKey;
        
        // Get business app metadata
        Map<String, Object> businessAppMetadata = getBusinessAppMetadata(businessAppName);
        
        // Get current task context (if taskId provided)
        CerbosResourceContext.CurrentTaskContext currentTask = null;
        if (taskId != null) {
            currentTask = buildCurrentTaskContext(taskId);
        }
        
        // Get process variables from Flowable (only if process instance exists)
        Map<String, Object> processVariables = processInstanceId != null ? 
            getProcessVariables(processInstanceId) : new HashMap<>();
        
        // Get all task states for this process instance (only if process instance exists)
        Map<String, CerbosResourceContext.TaskStateInfo> taskStates = processInstanceId != null ? 
            buildTaskStates(processInstanceId) : new HashMap<>();
        
        // Get workflow metadata (task-to-queue mappings, etc.)
        Map<String, Object> workflowMetadata = getWorkflowMetadata(processDefinitionKey, businessAppName);
        
        // Build the complete resource context
        CerbosResourceContext.CerbosResourceAttributes attributes = CerbosResourceContext.CerbosResourceAttributes.builder()
                .businessApp(businessAppName)
                .businessAppMetadata(businessAppMetadata)
                .processDefinitionKey(processDefinitionKey)
                .processInstanceId(processInstanceId)
                .currentTask(currentTask)
                .processVariables(processVariables)
                .taskStates(taskStates)
                .workflowMetadata(workflowMetadata)
                .build();
        
        CerbosResourceContext resourceContext = CerbosResourceContext.builder()
                .kind(resourceKind)
                .id(resourceId)
                .attr(attributes)
                .build();
        
        log.debug("Built Cerbos resource context: kind={}, id={}", resourceKind, resourceId);
        return resourceContext;
    }
    
    /**
     * Get process definition key from process instance
     */
    private String getProcessDefinitionKey(String processInstanceId) {
        try {
            return flowableRuntimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult()
                    .getProcessDefinitionKey();
        } catch (Exception e) {
            log.error("Failed to get process definition key for processInstance: {}", processInstanceId, e);
            throw new RuntimeException("Process instance not found: " + processInstanceId);
        }
    }
    
    /**
     * Build current task context from task ID
     */
    private CerbosResourceContext.CurrentTaskContext buildCurrentTaskContext(String taskId) {
        Optional<QueueTask> queueTaskOpt = queueTaskRepository.findByTaskId(taskId);
        
        if (queueTaskOpt.isEmpty()) {
            log.warn("Queue task not found for taskId: {}", taskId);
            return null;
        }
        
        QueueTask queueTask = queueTaskOpt.get();
        
        return CerbosResourceContext.CurrentTaskContext.builder()
                .taskDefinitionKey(queueTask.getTaskDefinitionKey())
                .taskId(queueTask.getTaskId())
                .queue(queueTask.getQueueName())
                .assignee(queueTask.getAssignee())
                .status(queueTask.getStatus().toString())
                .build();
    }
    
    /**
     * Get process variables from Flowable runtime
     */
    private Map<String, Object> getProcessVariables(String processInstanceId) {
        try {
            Map<String, Object> variables = flowableRuntimeService.getVariables(processInstanceId);
            log.debug("Retrieved {} process variables for processInstance: {}", 
                    variables.size(), processInstanceId);
            return variables != null ? variables : new HashMap<>();
        } catch (Exception e) {
            log.error("Failed to get process variables for processInstance: {}", processInstanceId, e);
            return new HashMap<>();
        }
    }
    
    /**
     * Build task states map for all tasks in this process instance
     */
    private Map<String, CerbosResourceContext.TaskStateInfo> buildTaskStates(String processInstanceId) {
        Map<String, CerbosResourceContext.TaskStateInfo> taskStates = new HashMap<>();
        
        try {
            List<QueueTask> queueTasks = queueTaskRepository.findByProcessInstanceId(processInstanceId);
            
            for (QueueTask task : queueTasks) {
                CerbosResourceContext.TaskStateInfo taskState = CerbosResourceContext.TaskStateInfo.builder()
                        .assignee(task.getAssignee())
                        .status(task.getStatus().toString())
                        .createdAt(task.getCreatedAt() != null ? task.getCreatedAt().toString() : null)
                        .completedAt(task.getCompletedAt() != null ? task.getCompletedAt().toString() : null)
                        .build();
                
                taskStates.put(task.getTaskDefinitionKey(), taskState);
            }
            
            log.debug("Built task states for {} tasks in processInstance: {}", 
                    taskStates.size(), processInstanceId);
            
        } catch (Exception e) {
            log.error("Failed to build task states for processInstance: {}", processInstanceId, e);
        }
        
        return taskStates;
    }
    
    /**
     * Dynamically determine the process definition key for a business application
     * This method looks up the active workflow for the given business app from repository
     * Fails hard if business app or workflow not found - no defaults allowed
     */
    private String determineProcessDefinitionKeyForBusinessApp(String businessAppName) {
        // Get business application - must exist
        Optional<BusinessApplication> businessAppOpt = businessApplicationRepository.findByBusinessAppName(businessAppName);
        if (businessAppOpt.isEmpty()) {
            log.error("Business application not found in repository: {}", businessAppName);
            throw new RuntimeException("Business application not found: " + businessAppName);
        }
        
        Long businessAppId = businessAppOpt.get().getId();
        
        // Get the active workflow metadata for this business app - must exist
        List<WorkflowMetadata> activeWorkflows = workflowMetadataRepository.findByBusinessApplicationIdAndActiveTrue(businessAppId);
        
        if (activeWorkflows.isEmpty()) {
            log.error("No active workflows found for business app: {}", businessAppName);
            throw new RuntimeException("No active workflows found for business app: " + businessAppName);
        }
        
        // Return the first active workflow (could be enhanced for multiple workflows)
        String processDefinitionKey = activeWorkflows.get(0).getProcessDefinitionKey();
        log.debug("Found process definition key: {} for business app: {}", processDefinitionKey, businessAppName);
        
        return processDefinitionKey;
    }
    
    /**
     * Get business application metadata for Cerbos policies
     */
    private Map<String, Object> getBusinessAppMetadata(String businessAppName) {
        try {
            // Get business application
            Optional<BusinessApplication> businessAppOpt = businessApplicationRepository.findByBusinessAppName(businessAppName);
            if (businessAppOpt.isEmpty()) {
                log.warn("Business application not found: {}", businessAppName);
                return new HashMap<>();
            }
            
            BusinessApplication businessApp = businessAppOpt.get();
            Map<String, Object> metadata = new HashMap<>();
            
            // Add business app basic info
            metadata.put("id", businessApp.getId());
            metadata.put("name", businessApp.getBusinessAppName());
            metadata.put("description", businessApp.getDescription());
            metadata.put("isActive", businessApp.getIsActive());
            
            // Add business app metadata (JSONB content)
            if (businessApp.getMetadata() != null) {
                metadata.putAll(businessApp.getMetadata());
            }
            
            log.debug("Retrieved business app metadata for: {}, keys: {}", businessAppName, metadata.keySet());
            return metadata;
            
        } catch (Exception e) {
            log.error("Failed to get business app metadata for: {}", businessAppName, e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get workflow metadata (task-to-queue mappings and configuration)
     */
    private Map<String, Object> getWorkflowMetadata(String processDefinitionKey, String businessAppName) {
        try {
            // Get business application to get the ID
            Optional<BusinessApplication> businessAppOpt = businessApplicationRepository.findByBusinessAppName(businessAppName);
            if (businessAppOpt.isEmpty()) {
                log.warn("Business application not found: {}", businessAppName);
                return new HashMap<>();
            }
            
            Long businessAppId = businessAppOpt.get().getId();
            
            // Get workflow metadata
            Optional<WorkflowMetadata> workflowOpt = workflowMetadataRepository.findByBusinessApplicationIdAndActiveTrue(businessAppId)
                    .stream()
                    .filter(wm -> wm.getProcessDefinitionKey().equals(processDefinitionKey))
                    .findFirst();
            if (workflowOpt.isEmpty()) {
                log.warn("Workflow metadata not found for process: {} in business app: {}", processDefinitionKey, businessAppName);
                return new HashMap<>();
            }
            
            WorkflowMetadata workflow = workflowOpt.get();
            Map<String, Object> metadata = new HashMap<>();
            
            // Add workflow basic info
            metadata.put("id", workflow.getId());
            metadata.put("processDefinitionKey", workflow.getProcessDefinitionKey());
            metadata.put("version", workflow.getVersion());
            metadata.put("isActive", workflow.getActive());
            metadata.put("taskQueueMappings", workflow.getTaskQueueMappings());
            
            // Add workflow metadata (JSONB content)
            if (workflow.getMetadata() != null) {
                metadata.putAll(workflow.getMetadata());
            }
            
            log.debug("Retrieved workflow metadata for process: {} in business app: {}, keys: {}", 
                    processDefinitionKey, businessAppName, metadata.keySet());
            return metadata;
            
        } catch (Exception e) {
            log.error("Failed to get workflow metadata for process: {} in business app: {}", 
                    processDefinitionKey, businessAppName, e);
            return new HashMap<>();
        }
    }
}