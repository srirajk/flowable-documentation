package com.flowable.wrapper.repository;

import com.flowable.wrapper.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    List<User> findByIsActiveTrue();
    
    @Query("SELECT u FROM User u WHERE u.isActive = true " +
           "AND (:attributeKey IS NULL OR :attributeValue IS NULL OR " +
           "JSON_EXTRACT(u.attributes, CONCAT('$.', :attributeKey)) = :attributeValue)")
    List<User> findByAttribute(@Param("attributeKey") String attributeKey, 
                              @Param("attributeValue") String attributeValue);
    
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN UserBusinessAppRole uar ON u.id = uar.userId " +
           "JOIN BusinessAppRole r ON uar.businessAppRole.id = r.id " +
           "JOIN BusinessApplication a ON r.businessApplication.id = a.id " +
           "WHERE u.isActive = true AND uar.isActive = true " +
           "AND (:businessAppName IS NULL OR a.businessAppName = :businessAppName) " +
           "AND (:roleName IS NULL OR r.roleName = :roleName)")
    List<User> findUsersWithRoles(@Param("businessAppName") String businessAppName,
                                 @Param("roleName") String roleName);
}