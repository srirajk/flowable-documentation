package com.flowable.wrapper.repository;

import com.flowable.wrapper.entity.UserBusinessAppRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBusinessAppRoleRepository extends JpaRepository<UserBusinessAppRole, Long> {
    
    @Query("SELECT uar FROM UserBusinessAppRole uar " +
           "JOIN FETCH uar.businessAppRole r " +
           "JOIN FETCH r.businessApplication a " +
           "WHERE uar.userId = :userId " +
           "AND uar.isActive = true " +
           "AND r.isActive = true " +
           "AND a.isActive = true " +
           "AND (:businessAppName IS NULL OR a.businessAppName = :businessAppName)")
    List<UserBusinessAppRole> findActiveUserRoles(@Param("userId") String userId,
                                                 @Param("businessAppName") String businessAppName);
    
    @Query("SELECT uar FROM UserBusinessAppRole uar " +
           "JOIN FETCH uar.businessAppRole r " +
           "JOIN FETCH r.businessApplication a " +
           "WHERE uar.userId = :userId " +
           "AND uar.isActive = true " +
           "AND r.isActive = true " +
           "AND a.isActive = true")
    List<UserBusinessAppRole> findAllActiveUserRoles(@Param("userId") String userId);
    
    @Query("SELECT DISTINCT uar.userId FROM UserBusinessAppRole uar " +
           "JOIN uar.businessAppRole r " +
           "JOIN r.businessApplication a " +
           "WHERE uar.isActive = true " +
           "AND r.isActive = true " +
           "AND a.isActive = true " +
           "AND a.businessAppName = :businessAppName " +
           "AND r.roleName = :roleName")
    List<String> findUserIdsByBusinessAppAndRole(@Param("businessAppName") String businessAppName,
                                                @Param("roleName") String roleName);
}