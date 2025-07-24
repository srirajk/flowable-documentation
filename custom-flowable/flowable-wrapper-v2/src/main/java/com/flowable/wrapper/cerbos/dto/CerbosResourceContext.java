package com.flowable.wrapper.cerbos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO representing the Cerbos resource context for authorization decisions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CerbosResourceContext {
    
    /**
     * Resource kind - should be the processDefinitionKey from BPMN
     */
    private String kind;
    
    /**
     * Resource ID - format: businessAppName::processInstanceId
     */
    private String id;
    
    /**
     * Resource attributes containing all context needed for authorization
     */
    private CerbosResourceAttributes attr;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CerbosResourceAttributes {
        
        /**
         * Business application name
         */
        private String businessApp;
        
        /**
         * Business application metadata (from workflow_metadata table)
         */
        private Map<String, Object> businessAppMetadata;
        
        /**
         * Process definition key
         */
        private String processDefinitionKey;
        
        /**
         * Process instance ID (null for process creation scenarios)
         */
        private String processInstanceId;
        
        /**
         * Current task context (for claim/complete operations)
         */
        private CurrentTaskContext currentTask;
        
        /**
         * Process variables from Flowable (workflow-specific data)
         * Empty for process creation scenarios
         */
        private Map<String, Object> processVariables;
        
        /**
         * Task states for this process instance
         * Empty for process creation scenarios
         */
        private Map<String, TaskStateInfo> taskStates;
        
        /**
         * Workflow metadata (task-to-queue mappings and configuration)
         */
        private Map<String, Object> workflowMetadata;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentTaskContext {
        private String taskDefinitionKey;
        private String taskId;
        private String queue;
        private String assignee;
        private String status;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskStateInfo {
        private String assignee;
        private String status;
        private String completedAt;
        private String createdAt;
    }
}