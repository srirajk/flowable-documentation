package com.flowable.wrapper.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "user_business_app_roles",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "business_app_role_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBusinessAppRole {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_app_role_id", nullable = false)
    private BusinessAppRole businessAppRole;
    
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    // Helper method to get user entity (if needed)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}