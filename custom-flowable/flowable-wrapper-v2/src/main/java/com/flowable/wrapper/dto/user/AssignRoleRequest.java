package com.flowable.wrapper.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to assign business application roles to a user")
public class AssignRoleRequest {
    
    @NotBlank(message = "Business application name is required")
    @Schema(description = "Business application name", example = "Sanctions-Management")
    private String businessAppName;
    
    @NotEmpty(message = "At least one role must be specified")
    @Schema(description = "Role names to assign", example = "[\"level1-maker\", \"level1-checker\"]")
    private List<String> roleNames;
    
}