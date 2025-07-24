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
@Schema(description = "User information response")
public class UserResponse {
    
    @Schema(description = "User ID", example = "alice.johnson")
    private String id;
    
    @Schema(description = "Username", example = "alice.johnson")
    private String username;
    
    @Schema(description = "Email address", example = "alice.johnson@company.com")
    private String email;
    
    @Schema(description = "First name", example = "Alice")
    private String firstName;
    
    @Schema(description = "Last name", example = "Johnson")
    private String lastName;
    
    @Schema(description = "Whether user is active")
    private Boolean isActive;
    
    @Schema(description = "User creation time")
    private Instant createdAt;
    
    @Schema(description = "Last update time")
    private Instant updatedAt;
    
    @Schema(description = "User attributes (flexible key-value pairs)", 
            example = "{\"department\": \"compliance\", \"region\": \"US\", \"level\": \"L1\", \"hireDate\": \"2022-01-15\"}")
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
    
    /**
     * Get user attributes for Cerbos principal (returns the attributes map directly)
     */
    public Map<String, Object> getAttributes() {
        return attributes != null ? attributes : new HashMap<>();
    }
}