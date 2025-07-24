package com.flowable.wrapper.controller;

import com.flowable.wrapper.dto.user.AssignRoleRequest;
import com.flowable.wrapper.dto.user.UserResponse;
import com.flowable.wrapper.dto.user.UserWorkflowRoleResponse;
import com.flowable.wrapper.dto.user.WorkflowRoleResponse;
import com.flowable.wrapper.exception.WorkflowException;
import com.flowable.wrapper.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for user and workflow role management")
public class UserController {
    
    private final UserManagementService userManagementService;
    
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", 
              description = "Retrieve user profile information including attributes for Cerbos")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable String userId) throws WorkflowException {
        
        log.info("Getting user profile for: {}", userId);
        UserResponse user = userManagementService.getUser(userId);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/{userId}/roles")
    @Operation(summary = "Get user's business application roles", 
              description = "Retrieve user's roles for a specific business application")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Roles retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserWorkflowRoleResponse> getUserRoles(
            @Parameter(description = "User ID", required = true)
            @PathVariable String userId,
            @Parameter(description = "Business application name", required = true)
            @RequestParam String businessAppName) throws WorkflowException {
        
        log.info("Getting roles for user {} in business application {}", userId, businessAppName);
        UserWorkflowRoleResponse roles = userManagementService.getUserWorkflowRoles(userId, businessAppName);
        return ResponseEntity.ok(roles);
    }
    
    @GetMapping("/{userId}/all-roles")
    @Operation(summary = "Get all user's business application roles", 
              description = "Retrieve user's roles across all business applications")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "All roles retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<UserWorkflowRoleResponse>> getUserAllRoles(
            @Parameter(description = "User ID", required = true)
            @PathVariable String userId) throws WorkflowException {
        
        log.info("Getting all roles for user: {}", userId);
        List<UserWorkflowRoleResponse> allRoles = userManagementService.getUserAllWorkflowRoles(userId);
        return ResponseEntity.ok(allRoles);
    }
    
    @PostMapping("/{userId}/roles")
    @Operation(summary = "Assign business application roles to user", 
              description = "Assign one or more business application roles to a user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Roles assigned successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "User or business application not found")
    })
    public ResponseEntity<UserWorkflowRoleResponse> assignRoles(
            @Parameter(description = "User ID", required = true)
            @PathVariable String userId,
            @Valid @RequestBody AssignRoleRequest request) throws WorkflowException {
        
        log.info("Assigning roles {} to user {} in business application {}", 
                request.getRoleNames(), userId, request.getBusinessAppName());
        
        UserWorkflowRoleResponse result = userManagementService.assignRoles(userId, request);
        return ResponseEntity.ok(result);
    }
    
    @DeleteMapping("/{userId}/roles")
    @Operation(summary = "Remove business application roles from user", 
              description = "Remove specific business application roles from a user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Roles removed successfully"),
        @ApiResponse(responseCode = "404", description = "User or roles not found")
    })
    public ResponseEntity<Void> removeRoles(
            @Parameter(description = "User ID", required = true)
            @PathVariable String userId,
            @Parameter(description = "Business application name", required = true)
            @RequestParam String businessAppName,
            @Parameter(description = "Role names to remove")
            @RequestParam List<String> roleNames) throws WorkflowException {
        
        log.info("Removing roles {} from user {} in business application {}", roleNames, userId, businessAppName);
        userManagementService.removeRoles(userId, businessAppName, roleNames);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping
    @Operation(summary = "Find users by criteria", 
              description = "Find users by business application, role, or user attributes")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Users found successfully")
    })
    public ResponseEntity<List<UserResponse>> findUsers(
            @Parameter(description = "Business application name")
            @RequestParam(required = false) String businessAppName,
            @Parameter(description = "Role name")
            @RequestParam(required = false) String roleName,
            @Parameter(description = "Attribute key")
            @RequestParam(required = false) String attributeKey,
            @Parameter(description = "Attribute value")
            @RequestParam(required = false) String attributeValue) throws WorkflowException {
        
        log.info("Finding users with filters - businessApp: {}, role: {}, attr: {}={}", 
                businessAppName, roleName, attributeKey, attributeValue);
        
        List<UserResponse> users = userManagementService.findUsers(businessAppName, roleName, attributeKey, attributeValue);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/business-apps/{businessAppName}/roles")
    @Operation(summary = "Get all roles for a business application", 
              description = "Retrieve all available roles for a specific business application")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Business application roles retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Business application not found")
    })
    public ResponseEntity<List<WorkflowRoleResponse>> getBusinessAppRoles(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName) throws WorkflowException {
        
        log.info("Getting all roles for business application: {}", businessAppName);
        List<WorkflowRoleResponse> roles = userManagementService.getWorkflowRoles(businessAppName);
        return ResponseEntity.ok(roles);
    }
}