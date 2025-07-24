package com.flowable.wrapper.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Business application role information")
public class WorkflowRoleResponse {
    
    @Schema(description = "Role ID")
    private Long id;
    
    @Schema(description = "Business application name", example = "Sanctions-Management")
    private String businessAppName;
    
    @Schema(description = "Role name", example = "level1-maker")
    private String roleName;
    
    @Schema(description = "Role display name", example = "Level 1 Maker")
    private String roleDisplayName;
    
    @Schema(description = "Role description")
    private String description;
    
    @Schema(description = "Whether role is active")
    private Boolean isActive;
    
    @Schema(description = "Role creation time")
    private Instant createdAt;
    
    @Schema(description = "Business app metadata (flexible key-value pairs)", 
            example = "{\"processDefinitionKey\": \"sanctionsCaseManagement\", \"version\": \"1.0\", \"owner\": \"compliance-team\"}")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}