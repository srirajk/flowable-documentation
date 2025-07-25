package com.flowable.wrapper.cerbos.service;

import com.flowable.wrapper.cerbos.builder.CerbosResourceBuilder;
import com.flowable.wrapper.cerbos.dto.CerbosRequest;
import com.flowable.wrapper.cerbos.dto.CerbosResourceContext;
import com.flowable.wrapper.dto.user.UserWorkflowRoleResponse;
import com.flowable.wrapper.dto.user.WorkflowRoleResponse;
import com.flowable.wrapper.dto.request.StartProcessRequest;
import com.flowable.wrapper.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import dev.cerbos.sdk.CerbosBlockingClient;
import dev.cerbos.sdk.CheckResult;
import dev.cerbos.sdk.CerbosException;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.Resource;
import dev.cerbos.sdk.builders.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for Cerbos authorization integration
 * Handles building authorization requests and making authorization decisions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CerbosService {
    
    // Cerbos Action Constants
    public static final String ACTION_CLAIM_TASK = "claim_task";
    public static final String ACTION_COMPLETE_TASK = "complete_task";
    public static final String ACTION_UNCLAIM_TASK = "unclaim_task";
    public static final String ACTION_VIEW_TASK = "view_task";
    public static final String ACTION_VIEW_QUEUE = "view_queue";
    public static final String ACTION_START_WORKFLOW = "start_workflow_instance";
    public static final String ACTION_READ_WORKFLOW = "read_workflow_instance";
    
    private final CerbosResourceBuilder resourceBuilder;
    private final UserManagementService userManagementService;
    private final CerbosBlockingClient cerbosClient;
    
    /**
     * Main authorization check method for existing process instances
     * 
     * @param userId User performing the action
     * @param action Action being performed (claim, complete, view, etc.)
     * @param processInstanceId Process instance ID
     * @param businessAppName Business application name
     * @param taskId Task ID (optional, for task-specific operations)
     * @return true if authorized, false otherwise
     */
    public boolean isAuthorized(String userId, 
                               String action, 
                               String processInstanceId, 
                               String businessAppName, 
                               String taskId) {
        
        log.debug("Checking authorization: user={}, action={}, processInstance={}, businessApp={}, task={}", 
                userId, action, processInstanceId, businessAppName, taskId);
        
        try {
            // Build complete Cerbos request for existing process
            CerbosRequest cerbosRequest = buildCerbosRequestForProcess(userId, action, processInstanceId, businessAppName, taskId);
            
            // Log the request for debugging (remove in production)
            logCerbosRequest(cerbosRequest);
            
            // Make authorization decision using Cerbos SDK
            boolean authorized = makeAuthorizationDecision(userId, action, cerbosRequest.getResource());
            
            log.debug("Authorization result: user={}, action={}, authorized={}", userId, action, authorized);
            return authorized;
            
        } catch (Exception e) {
            log.error("Authorization check failed: user={}, action={}, error={}", userId, action, e.getMessage(), e);
            // Fail closed - deny access on error
            return false;
        }
    }
    
    /**
     * Authorization check for process creation (no process instance yet)
     * 
     * @param userId User performing the action
     * @param action Action being performed (typically "create")
     * @param processDefinitionKey Process definition key
     * @param businessAppName Business application name
     * @return true if authorized, false otherwise
     */
    public boolean isAuthorizedForCreation(String userId, 
                                          String action, 
                                          String processDefinitionKey, 
                                          String businessAppName,
                                          StartProcessRequest startRequest) {
        
        log.debug("Checking creation authorization: user={}, action={}, processKey={}, businessApp={}", 
                userId, action, processDefinitionKey, businessAppName);
        
        try {
            // Build complete Cerbos request for process creation
            CerbosRequest cerbosRequest = buildCerbosRequestForCreation(userId, action, processDefinitionKey, businessAppName, startRequest);
            
            // Log the request for debugging (remove in production)
            logCerbosRequest(cerbosRequest);
            
            // Make authorization decision using Cerbos SDK
            boolean authorized = makeAuthorizationDecision(userId, action, cerbosRequest.getResource());
            
            log.debug("Creation authorization result: user={}, action={}, authorized={}", userId, action, authorized);
            return authorized;
            
        } catch (Exception e) {
            log.error("Creation authorization check failed: user={}, action={}, error={}", userId, action, e.getMessage(), e);
            // Fail closed - deny access on error
            return false;
        }
    }
    
    /**
     * Build Cerbos authorization request for existing process
     */
    private CerbosRequest buildCerbosRequestForProcess(String userId, 
                                                      String action, 
                                                      String processInstanceId, 
                                                      String businessAppName, 
                                                      String taskId) {
        
        // Build principal context (user + roles + attributes)
        CerbosRequest.CerbosPrincipal principal = buildPrincipalContext(userId, businessAppName);
        
        // Build resource context for existing workflow instance
        CerbosResourceContext resource = resourceBuilder.buildResourceContext(processInstanceId, businessAppName, taskId);
        
        // Keep the specific resource kind (businessApp::processKey) for workflow instances
        // No need to override - use the specific kind from resourceBuilder
        
        return CerbosRequest.builder()
                .principal(principal)
                .resource(resource)
                .action(action)
                .build();
    }
    
    /**
     * Build Cerbos authorization request for process creation
     */
    private CerbosRequest buildCerbosRequestForCreation(String userId, 
                                                       String action, 
                                                       String processDefinitionKey, 
                                                       String businessAppName,
                                                       StartProcessRequest startRequest) {
        
        // Build principal context (user + roles + attributes)
        CerbosRequest.CerbosPrincipal principal = buildPrincipalContext(userId, businessAppName);
        
        // Build resource context for workflow instance creation
        CerbosResourceContext resource = resourceBuilder.buildResourceContextForCreation(processDefinitionKey, businessAppName);
        
        // Embed createRequest variables at top level for flexible authorization
        if (startRequest != null && startRequest.getVariables() != null) {
            // Create enhanced resource attributes with createRequest at top level
            CerbosResourceContext.CerbosResourceAttributes enhancedAttributes = 
                CerbosResourceContext.CerbosResourceAttributes.builder()
                    .businessApp(resource.getAttr().getBusinessApp())
                    .processDefinitionKey(resource.getAttr().getProcessDefinitionKey())
                    .processInstanceId(resource.getAttr().getProcessInstanceId())
                    .currentTask(resource.getAttr().getCurrentTask())
                    .processVariables(resource.getAttr().getProcessVariables())
                    .taskStates(resource.getAttr().getTaskStates())
                    .businessAppMetadata(resource.getAttr().getBusinessAppMetadata())
                    .workflowMetadata(resource.getAttr().getWorkflowMetadata())
                    .createRequest(startRequest.getVariables())
                    .build();
            
            // Rebuild resource with enhanced attributes
            resource = CerbosResourceContext.builder()
                    .kind(resource.getKind())
                    .id(resource.getId())
                    .attr(enhancedAttributes)
                    .build();
            
            log.debug("Embedded createRequest variables at top level for flexible authorization");
        }
        
        // Keep the specific resource kind (businessApp::processKey) for workflow instances
        // No need to override - use the specific kind from resourceBuilder
        
        return CerbosRequest.builder()
                .principal(principal)
                .resource(resource)
                .action(action)
                .build();
    }
    
    /**
     * Build principal context for the user
     */
    private CerbosRequest.CerbosPrincipal buildPrincipalContext(String userId, String businessAppName) {
        try {
            // Get user's roles in this business application
            UserWorkflowRoleResponse userRoles = userManagementService.getUserWorkflowRoles(userId, businessAppName);
            
            // Extract roles from roleDetails
            List<String> roles = userRoles.getRoleDetails().stream()
                    .map(WorkflowRoleResponse::getRoleName)
                    .collect(Collectors.toList());
            
            // Extract business apps from roleDetails
            List<String> businessApps = userRoles.getRoleDetails().stream()
                    .map(WorkflowRoleResponse::getBusinessAppName)
                    .distinct()
                    .collect(Collectors.toList());
            
            // Build principal attributes with businessApps
            Map<String, Object> userAttributes = userRoles.getUser().getAttributes();
            Map<String, Object> principalAttributes = new HashMap<>(userAttributes != null ? userAttributes : new HashMap<>());
            principalAttributes.put("businessApps", businessApps);
            
            return CerbosRequest.CerbosPrincipal.builder()
                    .id(userId)
                    .roles(roles)
                    .attr(principalAttributes)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to build principal context for user: {}, businessApp: {}", userId, businessAppName, e);
            // Don't swallow exceptions that could lead to authorization bypasses
            throw new RuntimeException("Failed to build principal context for authorization", e);
        }
    }
    
    /**
     * Make actual Cerbos authorization decision using SDK
     */
    private boolean makeAuthorizationDecision(String userId, String action, CerbosResourceContext resourceContext) {
        try {
            log.debug("Making authorization decision: userId={}, action={}, resourceKind={}", 
                    userId, action, resourceContext.getKind());
            
            // Get user roles and attributes
            UserWorkflowRoleResponse userRoles = userManagementService.getUserWorkflowRoles(userId, resourceContext.getAttr().getBusinessApp());
            
            // Build principal for Cerbos SDK
            Principal principal = Principal.newInstance(userId);
            
            // Extract and add roles from roleDetails
            List<String> roles = userRoles.getRoleDetails().stream()
                    .map(WorkflowRoleResponse::getRoleName)
                    .collect(Collectors.toList());
            
            for (String role : roles) {
                principal = principal.withRoles(role);
            }
            
            // Extract and add businessApps from roleDetails
            List<String> businessApps = userRoles.getRoleDetails().stream()
                    .map(WorkflowRoleResponse::getBusinessAppName)
                    .distinct()
                    .collect(Collectors.toList());
            
            List<AttributeValue> businessAppValues = businessApps.stream()
                    .map(AttributeValue::stringValue)
                    .collect(Collectors.toList());
            principal = principal.withAttribute("businessApps", AttributeValue.listValue(businessAppValues));
            
            // Add attributes
            Map<String, Object> userAttributes = userRoles.getUser().getAttributes();
            if (userAttributes != null) {
                for (Map.Entry<String, Object> entry : userAttributes.entrySet()) {
                    AttributeValue attrValue = convertToAttributeValue(entry.getValue());
                    if (attrValue != null) {
                        principal = principal.withAttribute(entry.getKey(), attrValue);
                    }
                }
            }
            
            // Build resource for Cerbos SDK
            Resource resource = Resource.newInstance(resourceContext.getKind(), resourceContext.getId());
            
            // Add resource attributes
            CerbosResourceContext.CerbosResourceAttributes attrs = resourceContext.getAttr();
            resource = resource.withAttribute("businessApp", AttributeValue.stringValue(attrs.getBusinessApp()))
                    .withAttribute("processDefinitionKey", AttributeValue.stringValue(attrs.getProcessDefinitionKey()));
            
            if (attrs.getProcessInstanceId() != null) {
                resource = resource.withAttribute("processInstanceId", AttributeValue.stringValue(attrs.getProcessInstanceId()));
            }
            
            // Add business app metadata
            if (attrs.getBusinessAppMetadata() != null && !attrs.getBusinessAppMetadata().isEmpty()) {
                for (Map.Entry<String, Object> entry : attrs.getBusinessAppMetadata().entrySet()) {
                    AttributeValue attrValue = convertToAttributeValue(entry.getValue());
                    if (attrValue != null) {
                        resource = resource.withAttribute("businessAppMetadata." + entry.getKey(), attrValue);
                    }
                }
            }
            
            // Add workflow metadata
            if (attrs.getWorkflowMetadata() != null && !attrs.getWorkflowMetadata().isEmpty()) {
                for (Map.Entry<String, Object> entry : attrs.getWorkflowMetadata().entrySet()) {
                    AttributeValue attrValue = convertToAttributeValue(entry.getValue());
                    if (attrValue != null) {
                        resource = resource.withAttribute("workflowMetadata." + entry.getKey(), attrValue);
                    }
                }
            }
            
            // Add current queue for queue access operations
            if (attrs.getCurrentQueue() != null) {
                resource = resource.withAttribute("currentQueue", AttributeValue.stringValue(attrs.getCurrentQueue()));
            }
            
            // Add current task context for task-level operations (claim, complete, unclaim, view)
            if (attrs.getCurrentTask() != null) {
                CerbosResourceContext.CurrentTaskContext currentTask = attrs.getCurrentTask();
                
                // Build nested currentTask object (not flat attributes)
                Map<String, AttributeValue> currentTaskMap = new HashMap<>();
                
                if (currentTask.getTaskDefinitionKey() != null) {
                    currentTaskMap.put("taskDefinitionKey", AttributeValue.stringValue(currentTask.getTaskDefinitionKey()));
                }
                if (currentTask.getTaskId() != null) {
                    currentTaskMap.put("taskId", AttributeValue.stringValue(currentTask.getTaskId()));
                }
                if (currentTask.getQueue() != null) {
                    currentTaskMap.put("queue", AttributeValue.stringValue(currentTask.getQueue()));
                }
                // Only set assignee if not null - policy checks for missing key using has()
                if (currentTask.getAssignee() != null) {
                    currentTaskMap.put("assignee", AttributeValue.stringValue(currentTask.getAssignee()));
                }
                if (currentTask.getStatus() != null) {
                    currentTaskMap.put("status", AttributeValue.stringValue(currentTask.getStatus()));
                }
                
                // Add as nested object
                resource = resource.withAttribute("currentTask", AttributeValue.mapValue(currentTaskMap));
                
                log.debug("Added currentTask nested object: taskDefinitionKey={}, queue={}, assignee={}, status={}", 
                        currentTask.getTaskDefinitionKey(), currentTask.getQueue(), currentTask.getAssignee(), currentTask.getStatus());
            }
            
            // Add task states for Four-Eyes Principle enforcement
            if (attrs.getTaskStates() != null && !attrs.getTaskStates().isEmpty()) {
                Map<String, AttributeValue> taskStatesMap = new HashMap<>();
                
                for (Map.Entry<String, CerbosResourceContext.TaskStateInfo> entry : attrs.getTaskStates().entrySet()) {
                    String taskDefKey = entry.getKey();
                    CerbosResourceContext.TaskStateInfo taskState = entry.getValue();
                    
                    // Build nested task state object
                    Map<String, AttributeValue> taskStateMap = new HashMap<>();
                    
                    // Only set assignee if not null - policy checks for missing key using has()
                    if (taskState.getAssignee() != null) {
                        taskStateMap.put("assignee", AttributeValue.stringValue(taskState.getAssignee()));
                    }
                    if (taskState.getStatus() != null) {
                        taskStateMap.put("status", AttributeValue.stringValue(taskState.getStatus()));
                    }
                    if (taskState.getCompletedAt() != null) {
                        taskStateMap.put("completedAt", AttributeValue.stringValue(taskState.getCompletedAt()));
                    }
                    if (taskState.getCreatedAt() != null) {
                        taskStateMap.put("createdAt", AttributeValue.stringValue(taskState.getCreatedAt()));
                    }
                    
                    // Add this task state as nested object
                    taskStatesMap.put(taskDefKey, AttributeValue.mapValue(taskStateMap));
                }
                
                // Add taskStates as nested object
                resource = resource.withAttribute("taskStates", AttributeValue.mapValue(taskStatesMap));
                
                log.debug("Added taskStates nested object for {} tasks", attrs.getTaskStates().size());
            }
            
            // Log authorization check at DEBUG level to avoid information disclosure
            log.info("=== CERBOS CHECK REQUEST ===");
            log.debug("Action: {}", action);
            log.debug("Resource Kind: {}", resourceContext.getKind());
            log.debug("Resource ID: {}", resourceContext.getId());
            log.debug("Business App: {}", resourceContext.getAttr().getBusinessApp());
            log.debug("Process Definition Key: {}", resourceContext.getAttr().getProcessDefinitionKey());
            log.debug("User ID: {}", userId);
            log.debug("User Roles: {}", roles);
            log.debug("User Business Apps: {}", businessApps);
            // Don't log user attributes at INFO level - may contain PII
            log.debug("User Attributes: {}", userRoles.getUser().getAttributes());
            log.debug("============================");
            
            // Make the authorization call using Cerbos SDK
            CheckResult result = cerbosClient.check(principal, resource, action);
            
            // Get the decision
            boolean isAllowed = result.isAllowed(action);
            
            log.info("Cerbos authorization result: userId={}, action={}, resourceKind={}, allowed={}", 
                    userId, action, resourceContext.getKind(), isAllowed);
            
            log.debug("Cerbos authorization result: userId={}, action={}, resourceKind={}, allowed={}", 
                    userId, action, resourceContext.getKind(), isAllowed);
            
            return isAllowed;
            
        } catch (CerbosException e) {
            log.error("Cerbos authorization check failed: userId={}, action={}, error={}", userId, action, e.getMessage(), e);
            // Fail closed - deny access on error
            return false;
        } catch (Exception e) {
            log.error("Authorization check failed: userId={}, action={}, error={}", userId, action, e.getMessage(), e);
            // Fail closed - deny access on error
            return false;
        }
    }
    
    /**
     * Log Cerbos request for debugging (remove in production)
     */
    private void logCerbosRequest(CerbosRequest request) {
        log.debug("=== CERBOS REQUEST ===");
        log.debug("Principal: id={}, roles={}", request.getPrincipal().getId(), request.getPrincipal().getRoles());
        log.debug("Resource: kind={}, id={}", request.getResource().getKind(), request.getResource().getId());
        log.debug("Action: {}", request.getAction());
        log.debug("Business App: {}", request.getResource().getAttr().getBusinessApp());
        log.debug("Process Definition Key: {}", request.getResource().getAttr().getProcessDefinitionKey());
        log.debug("Process Instance ID: {}", request.getResource().getAttr().getProcessInstanceId());
        log.debug("Business App Metadata: {}", request.getResource().getAttr().getBusinessAppMetadata().keySet());
        log.debug("Process Variables: {}", request.getResource().getAttr().getProcessVariables().keySet());
        log.debug("Task States: {}", request.getResource().getAttr().getTaskStates().keySet());
        log.debug("====================");
    }
    
    // =========================================================================
    // CONVENIENCE METHODS FOR COMMON OPERATIONS
    // =========================================================================
    
    /**
     * Check if user can claim a specific task
     */
    public boolean canClaimTask(String userId, String processInstanceId, String businessAppName, String taskId) {
        return isAuthorized(userId, "claim", processInstanceId, businessAppName, taskId);
    }
    
    /**
     * Check if user can complete a specific task
     */
    public boolean canCompleteTask(String userId, String processInstanceId, String businessAppName, String taskId) {
        return isAuthorized(userId, "complete", processInstanceId, businessAppName, taskId);
    }
    
    /**
     * Check if user can view process instance
     */
    public boolean canViewProcess(String userId, String processInstanceId, String businessAppName) {
        return isAuthorized(userId, "view", processInstanceId, businessAppName, null);
    }
    
    /**
     * Check if user can access queue within the workflow context
     * Queue authorization is part of the same workflow policy
     */
    public boolean canAccessQueue(String userId, String businessAppName, String queueName) {
        log.debug("Checking queue access authorization: user={}, businessApp={}, queue={}", 
                userId, businessAppName, queueName);
        
        try {
            // Build workflow-specific Cerbos request for queue access
            CerbosRequest cerbosRequest = buildCerbosRequestForQueueAccess(userId, businessAppName, queueName);
            
            // Log the request for debugging
            logCerbosRequest(cerbosRequest);
            
            // Make authorization decision using Cerbos SDK
            boolean authorized = makeAuthorizationDecision(userId, "view_queue", cerbosRequest.getResource());
            
            log.debug("Queue access authorization result: user={}, businessApp={}, queue={}, authorized={}", 
                    userId, businessAppName, queueName, authorized);
            return authorized;
            
        } catch (Exception e) {
            log.error("Queue access authorization check failed: user={}, businessApp={}, queue={}, error={}", 
                    userId, businessAppName, queueName, e.getMessage(), e);
            // Fail closed - deny access on error
            return false;
        }
    }
    
    /**
     * Build Cerbos authorization request for queue access within workflow context
     */
    private CerbosRequest buildCerbosRequestForQueueAccess(String userId, String businessAppName, String queueName) {
        // Build principal context (user + roles + attributes)
        CerbosRequest.CerbosPrincipal principal = buildPrincipalContext(userId, businessAppName);
        
        // Build workflow resource context for queue access - use the same resource kind as workflow
        CerbosResourceContext resource = resourceBuilder.buildQueueResourceContextForWorkflow(businessAppName, queueName);
        
        return CerbosRequest.builder()
                .principal(principal)
                .resource(resource)
                .action("view_queue")
                .build();
    }
    
    /**
     * Check if user can create a new process instance
     */
    public boolean canCreateProcess(String userId, String processDefinitionKey, String businessAppName) {
        return isAuthorizedForCreation(userId, "create", processDefinitionKey, businessAppName, null);
    }
    
    /**
     * Generic task authorization method for any business application and workflow pattern
     * This method builds the appropriate resource context for task operations and delegates 
     * all business logic (Four-Eyes Principle, queue access, etc.) to Cerbos policies
     * 
     * @param userId User performing the action
     * @param action Task action (claim_task, complete_task, unclaim_task, view_task)
     * @param businessAppName Business application name
     * @param taskId Task ID for task-specific context
     * @return true if authorized, false otherwise
     */
    public boolean isAuthorizedForTask(String userId, 
                                      String action, 
                                      String businessAppName, 
                                      String taskId) {
        
        log.debug("Checking generic task authorization: user={}, action={}, businessApp={}, task={}", 
                userId, action, businessAppName, taskId);
        
        try {
            // Build task-specific Cerbos request
            CerbosRequest cerbosRequest = buildCerbosRequestForTask(userId, action, businessAppName, taskId);
            
            // Log the request for debugging
            logCerbosRequest(cerbosRequest);
            
            // Make authorization decision using Cerbos SDK
            boolean authorized = makeAuthorizationDecision(userId, action, cerbosRequest.getResource());
            
            log.debug("Task authorization result: user={}, action={}, businessApp={}, task={}, authorized={}", 
                    userId, action, businessAppName, taskId, authorized);
            return authorized;
            
        } catch (Exception e) {
            log.error("Task authorization check failed: user={}, action={}, businessApp={}, task={}, error={}", 
                    userId, action, businessAppName, taskId, e.getMessage(), e);
            // Fail closed - deny access on error
            return false;
        }
    }
    
    /**
     * Build Cerbos authorization request for generic task operations
     */
    private CerbosRequest buildCerbosRequestForTask(String userId, String action, String businessAppName, String taskId) {
        // Build principal context (user + roles + attributes)
        CerbosRequest.CerbosPrincipal principal = buildPrincipalContext(userId, businessAppName);
        
        // Build generic task resource context - let the resource builder handle task-specific context
        CerbosResourceContext resource = resourceBuilder.buildTaskResourceContext(businessAppName, taskId);
        
        return CerbosRequest.builder()
                .principal(principal)
                .resource(resource)
                .action(action)
                .build();
    }
    
    /**
     * Check authorization for workflow management operations (register, deploy, view workflows)
     */
    public boolean isAuthorizedForWorkflowManagement(String userId, String action, String processDefinitionKey, String businessAppName) {
        try {
            log.debug("Checking workflow management authorization: userId={}, action={}, processKey={}, businessApp={}", 
                    userId, action, processDefinitionKey, businessAppName);
            
            // Build Cerbos request for workflow management
            CerbosRequest cerbosRequest = buildCerbosRequestForWorkflowManagement(userId, action, processDefinitionKey, businessAppName);
            
            // Log the request for debugging
            logCerbosRequest(cerbosRequest);
            
            // Make authorization decision using Cerbos SDK
            boolean isAuthorized = makeAuthorizationDecision(userId, action, cerbosRequest.getResource());
            
            log.debug("Workflow management authorization result: userId={}, action={}, processKey={}, businessApp={}, authorized={}", 
                    userId, action, processDefinitionKey, businessAppName, isAuthorized);
            
            return isAuthorized;
            
        } catch (Exception e) {
            log.error("Failed to check workflow management authorization for userId: {}, action: {}, processKey: {}, businessApp: {}", 
                    userId, action, processDefinitionKey, businessAppName, e);
            return false; // Fail secure
        }
    }
    
    /**
     * Build Cerbos authorization request for workflow management operations
     */
    private CerbosRequest buildCerbosRequestForWorkflowManagement(String userId, String action, String processDefinitionKey, String businessAppName) {
        // Build principal context (user + roles + attributes)
        CerbosRequest.CerbosPrincipal principal = buildPrincipalContext(userId, businessAppName);
        
        // Build resource context for workflow management
        CerbosResourceContext resource = buildWorkflowManagementResourceContext(processDefinitionKey, businessAppName);
        
        return CerbosRequest.builder()
                .principal(principal)
                .resource(resource)
                .action(action)
                .build();
    }
    
    /**
     * Build resource context for workflow management operations
     */
    private CerbosResourceContext buildWorkflowManagementResourceContext(String processDefinitionKey, String businessAppName) {
        // For workflow management, we use a common resource kind
        String resourceKind = "workflow-management";
        String resourceId = businessAppName + "::" + processDefinitionKey;
        
        // Build minimal resource attributes for workflow management
        CerbosResourceContext.CerbosResourceAttributes attributes = CerbosResourceContext.CerbosResourceAttributes.builder()
                .businessApp(businessAppName)
                .businessAppMetadata(new HashMap<>()) // Could be populated from business app service
                .processDefinitionKey(processDefinitionKey)
                .processInstanceId(null) // No process instance for workflow management
                .currentTask(null) // No current task for workflow management
                .processVariables(new HashMap<>()) // No process variables for workflow management
                .taskStates(new HashMap<>()) // No task states for workflow management
                .workflowMetadata(new HashMap<>()) // Could be populated from workflow metadata service
                .build();
        
        return CerbosResourceContext.builder()
                .kind(resourceKind)
                .id(resourceId)
                .attr(attributes)
                .build();
    }
    
    /**
     * Convert Java object to Cerbos AttributeValue
     */
    private AttributeValue convertToAttributeValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof String) {
            return AttributeValue.stringValue((String) value);
        } else if (value instanceof Boolean) {
            return AttributeValue.boolValue((Boolean) value);
        } else if (value instanceof Number) {
            return AttributeValue.doubleValue(((Number) value).doubleValue());
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<AttributeValue> attrValues = new java.util.ArrayList<>();
            for (Object item : list) {
                AttributeValue attrValue = convertToAttributeValue(item);
                if (attrValue != null) {
                    attrValues.add(attrValue);
                }
            }
            return AttributeValue.listValue(attrValues);
        } else {
            // For other types, convert to string
            return AttributeValue.stringValue(value.toString());
        }
    }
}