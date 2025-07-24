package com.flowable.wrapper.cerbos.service;

import com.flowable.wrapper.cerbos.builder.CerbosResourceBuilder;
import com.flowable.wrapper.cerbos.dto.CerbosRequest;
import com.flowable.wrapper.cerbos.dto.CerbosResourceContext;
import com.flowable.wrapper.dto.user.UserWorkflowRoleResponse;
import com.flowable.wrapper.dto.user.WorkflowRoleResponse;
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
                                          String businessAppName) {
        
        log.debug("Checking creation authorization: user={}, action={}, processKey={}, businessApp={}", 
                userId, action, processDefinitionKey, businessAppName);
        
        try {
            // Build complete Cerbos request for process creation
            CerbosRequest cerbosRequest = buildCerbosRequestForCreation(userId, action, processDefinitionKey, businessAppName);
            
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
        
        // Override resource kind for workflow instances
        if (resource.getKind().contains("::")) {
            // Change from businessApp::processKey to workflow-instance
            resource = CerbosResourceContext.builder()
                    .kind("workflow-instance")
                    .id(resource.getId())
                    .attr(resource.getAttr())
                    .build();
        }
        
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
                                                       String businessAppName) {
        
        // Build principal context (user + roles + attributes)
        CerbosRequest.CerbosPrincipal principal = buildPrincipalContext(userId, businessAppName);
        
        // Build resource context for workflow instance creation
        CerbosResourceContext resource = resourceBuilder.buildResourceContextForCreation(processDefinitionKey, businessAppName);
        
        // Override resource kind for workflow instances
        resource = CerbosResourceContext.builder()
                .kind("workflow-instance")
                .id(businessAppName + "::" + processDefinitionKey)
                .attr(resource.getAttr())
                .build();
        
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
            // Return minimal principal on error
            return CerbosRequest.CerbosPrincipal.builder()
                    .id(userId)
                    .roles(List.of())
                    .attr(new HashMap<>())
                    .build();
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
            
            // Log detailed principal and resource attributes before Cerbos check
            log.info("=== CERBOS CHECK REQUEST ===");
            log.info("Action: {}", action);
            log.info("Resource Kind: {}", resourceContext.getKind());
            log.info("Resource ID: {}", resourceContext.getId());
            log.info("Business App: {}", resourceContext.getAttr().getBusinessApp());
            log.info("Process Definition Key: {}", resourceContext.getAttr().getProcessDefinitionKey());
            log.info("User ID: {}", userId);
            log.info("User Roles: {}", roles);
            log.info("User Business Apps: {}", businessApps);
            log.info("User Attributes: {}", userRoles.getUser().getAttributes());
            log.info("============================");
            
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
     * Check if user can access business app queue
     */
    public boolean canAccessQueue(String userId, String businessAppName, String queueName) {
        // For queue access, we use businessApp as both resource kind and ID
        return isAuthorized(userId, "view_queue", businessAppName, businessAppName, null);
    }
    
    /**
     * Check if user can create a new process instance
     */
    public boolean canCreateProcess(String userId, String processDefinitionKey, String businessAppName) {
        return isAuthorizedForCreation(userId, "create", processDefinitionKey, businessAppName);
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