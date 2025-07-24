package com.flowable.wrapper.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User's business application roles response")
public class UserWorkflowRoleResponse {
    
    @Schema(description = "User information")
    private UserResponse user;
    
    
    @Schema(description = "Detailed role information")
    private List<WorkflowRoleResponse> roleDetails;
    
    
    @Schema(description = "Business app metadata (flexible key-value pairs)", 
            example = "{\"processDefinitionKey\": \"sanctionsCaseManagement\", \"description\": \"Level 1 and Level 2 sanctions case management\"}")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}