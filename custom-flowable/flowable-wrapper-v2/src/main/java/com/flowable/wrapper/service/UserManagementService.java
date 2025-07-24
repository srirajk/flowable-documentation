package com.flowable.wrapper.service;

import com.flowable.wrapper.dto.user.AssignRoleRequest;
import com.flowable.wrapper.dto.user.UserResponse;
import com.flowable.wrapper.dto.user.UserWorkflowRoleResponse;
import com.flowable.wrapper.dto.user.WorkflowRoleResponse;
import com.flowable.wrapper.entity.*;
import com.flowable.wrapper.exception.ResourceNotFoundException;
import com.flowable.wrapper.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserManagementService {
    
    private final UserRepository userRepository;
    private final BusinessApplicationRepository businessApplicationRepository;
    private final BusinessAppRoleRepository businessAppRoleRepository;
    private final UserBusinessAppRoleRepository userBusinessAppRoleRepository;
    
    /**
     * Get user by ID
     */
    public UserResponse getUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        return mapToUserResponse(user);
    }
    
    /**
     * Get user's roles for a specific business application
     */
    public UserWorkflowRoleResponse getUserWorkflowRoles(String userId, String businessAppName) {
        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        // Verify business application exists
        BusinessApplication businessApp = businessApplicationRepository.findByBusinessAppName(businessAppName)
                .orElseThrow(() -> new ResourceNotFoundException("Business Application", businessAppName));
        
        // Get user's active roles for this business application
        List<UserBusinessAppRole> userRoles = userBusinessAppRoleRepository.findActiveUserRoles(userId, businessAppName);
        
        List<WorkflowRoleResponse> roleDetails = userRoles.stream()
                .map(uar -> mapToWorkflowRoleResponse(uar.getBusinessAppRole(), businessApp))
                .collect(Collectors.toList());
        
        return UserWorkflowRoleResponse.builder()
                .user(mapToUserResponse(user))
                .roleDetails(roleDetails)
                .metadata(businessApp.getMetadata())
                .build();
    }
    
    /**
     * Get all user's roles across all business applications
     */
    public List<UserWorkflowRoleResponse> getUserAllWorkflowRoles(String userId) {
        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        List<UserBusinessAppRole> allUserRoles = userBusinessAppRoleRepository.findAllActiveUserRoles(userId);
        
        // Group by business application
        return allUserRoles.stream()
                .collect(Collectors.groupingBy(uar -> uar.getBusinessAppRole().getBusinessApplication()))
                .entrySet().stream()
                .map(entry -> {
                    BusinessApplication businessApp = entry.getKey();
                    List<UserBusinessAppRole> appRoles = entry.getValue();
                    
                    List<WorkflowRoleResponse> roleDetails = appRoles.stream()
                            .map(uar -> mapToWorkflowRoleResponse(uar.getBusinessAppRole(), businessApp))
                            .collect(Collectors.toList());
                    
                    return UserWorkflowRoleResponse.builder()
                            .user(mapToUserResponse(user))
                            .roleDetails(roleDetails)
                            .metadata(businessApp.getMetadata())
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Find users by criteria
     */
    public List<UserResponse> findUsers(String businessAppName, String roleName, 
                                       String attributeKey, String attributeValue) {
        
        List<User> users = new ArrayList<>();
        
        if (businessAppName != null || roleName != null) {
            // Find users with specific roles
            users = userRepository.findUsersWithRoles(businessAppName, roleName);
        } else if (attributeKey != null && attributeValue != null) {
            // Find users by attribute
            users = userRepository.findByAttribute(attributeKey, attributeValue);
        } else {
            // Return all active users
            users = userRepository.findByIsActiveTrue();
        }
        
        return users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all roles for a business application
     */
    public List<WorkflowRoleResponse> getWorkflowRoles(String businessAppName) {
        BusinessApplication businessApp = businessApplicationRepository.findByBusinessAppName(businessAppName)
                .orElseThrow(() -> new ResourceNotFoundException("Business Application", businessAppName));
        
        List<BusinessAppRole> roles = businessAppRoleRepository.findByBusinessApplicationBusinessAppNameAndIsActiveTrue(businessAppName);
        
        return roles.stream()
                .map(role -> mapToWorkflowRoleResponse(role, businessApp))
                .collect(Collectors.toList());
    }
    
    // =========================================================================
    // MAPPING METHODS
    // =========================================================================
    
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .attributes(user.getAttributes())
                .build();
    }
    
    private WorkflowRoleResponse mapToWorkflowRoleResponse(BusinessAppRole role, BusinessApplication businessApp) {
        return WorkflowRoleResponse.builder()
                .id(role.getId())
                .businessAppName(businessApp.getBusinessAppName())
                .roleName(role.getRoleName())
                .roleDisplayName(role.getRoleDisplayName())
                .description(role.getDescription())
                .isActive(role.getIsActive())
                .createdAt(role.getCreatedAt())
                .metadata(role.getMetadata())
                .build();
    }
    
    // =========================================================================
    // STUB METHODS (for future implementation if needed)
    // =========================================================================
    
    @Transactional
    public UserWorkflowRoleResponse assignRoles(String userId, AssignRoleRequest request) {
        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        // Verify business application exists
        BusinessApplication businessApp = businessApplicationRepository.findByBusinessAppName(request.getBusinessAppName())
                .orElseThrow(() -> new ResourceNotFoundException("Business Application", request.getBusinessAppName()));
        
        // Get the business app roles for the requested role names
        List<BusinessAppRole> requestedRoles = businessAppRoleRepository.findByBusinessApplicationBusinessAppNameAndIsActiveTrue(request.getBusinessAppName())
                .stream()
                .filter(role -> request.getRoleNames().contains(role.getRoleName()))
                .collect(Collectors.toList());
        
        if (requestedRoles.size() != request.getRoleNames().size()) {
            List<String> foundRoleNames = requestedRoles.stream()
                    .map(BusinessAppRole::getRoleName)
                    .collect(Collectors.toList());
            List<String> missingRoles = request.getRoleNames().stream()
                    .filter(roleName -> !foundRoleNames.contains(roleName))
                    .collect(Collectors.toList());
            throw new ResourceNotFoundException("Business App Roles", missingRoles.toString());
        }
        
        // Get existing assignments to avoid duplicates
        List<UserBusinessAppRole> existingAssignments = userBusinessAppRoleRepository.findActiveUserRoles(userId, request.getBusinessAppName());
        List<Long> existingRoleIds = existingAssignments.stream()
                .map(uar -> uar.getBusinessAppRole().getId())
                .collect(Collectors.toList());
        
        // Create new assignments for roles that don't exist yet
        List<UserBusinessAppRole> newAssignments = requestedRoles.stream()
                .filter(role -> !existingRoleIds.contains(role.getId()))
                .map(role -> UserBusinessAppRole.builder()
                        .userId(userId)
                        .businessAppRole(role)
                        .isActive(true)
                        .build())
                .collect(Collectors.toList());
        
        // Save new assignments
        if (!newAssignments.isEmpty()) {
            userBusinessAppRoleRepository.saveAll(newAssignments);
            log.info("Assigned {} new roles to user {} in business application {}", 
                    newAssignments.size(), userId, request.getBusinessAppName());
        }
        
        // Return updated user roles
        return getUserWorkflowRoles(userId, request.getBusinessAppName());
    }
    
    @Transactional
    public void removeRoles(String userId, String businessAppName, List<String> roleNames) {
        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        // Verify business application exists
        BusinessApplication businessApp = businessApplicationRepository.findByBusinessAppName(businessAppName)
                .orElseThrow(() -> new ResourceNotFoundException("Business Application", businessAppName));
        
        // Get the business app roles for the requested role names
        List<BusinessAppRole> rolesToRemove = businessAppRoleRepository.findByBusinessAppAndRoleNames(
                businessAppName, roleNames);
        
        if (rolesToRemove.isEmpty()) {
            log.warn("No roles found to remove for user {} in business application {}", userId, businessAppName);
            return;
        }
        
        List<Long> roleIdsToRemove = rolesToRemove.stream()
                .map(BusinessAppRole::getId)
                .collect(Collectors.toList());
        
        // Get existing assignments that match the roles to remove
        List<UserBusinessAppRole> existingAssignments = userBusinessAppRoleRepository.findActiveUserRoles(userId, businessAppName);
        List<UserBusinessAppRole> assignmentsToDeactivate = existingAssignments.stream()
                .filter(uar -> roleIdsToRemove.contains(uar.getBusinessAppRole().getId()))
                .collect(Collectors.toList());
        
        // Deactivate the assignments
        if (!assignmentsToDeactivate.isEmpty()) {
            assignmentsToDeactivate.forEach(assignment -> assignment.setIsActive(false));
            userBusinessAppRoleRepository.saveAll(assignmentsToDeactivate);
            log.info("Deactivated {} role assignments for user {} in business application {}", 
                    assignmentsToDeactivate.size(), userId, businessAppName);
        } else {
            log.warn("No active role assignments found to remove for user {} in business application {}", 
                    userId, businessAppName);
        }
    }
}