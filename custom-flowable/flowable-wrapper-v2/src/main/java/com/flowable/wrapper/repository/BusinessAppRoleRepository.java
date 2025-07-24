package com.flowable.wrapper.repository;

import com.flowable.wrapper.entity.BusinessAppRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessAppRoleRepository extends JpaRepository<BusinessAppRole, Long> {
    
    @Query("SELECT r FROM BusinessAppRole r JOIN r.businessApplication ba WHERE ba.businessAppName = ?1 AND r.isActive = true")
    List<BusinessAppRole> findByBusinessApplicationBusinessAppNameAndIsActiveTrue(String businessAppName);
    
    Optional<BusinessAppRole> findByBusinessApplicationBusinessAppNameAndRoleName(String businessAppName, String roleName);
    
    @Query("SELECT r FROM BusinessAppRole r " +
           "JOIN FETCH r.businessApplication ba " +
           "WHERE ba.businessAppName = :businessAppName " +
           "AND r.isActive = true " +
           "ORDER BY r.roleName")
    List<BusinessAppRole> findActiveRolesByBusinessApp(@Param("businessAppName") String businessAppName);
    
    @Query("SELECT r FROM BusinessAppRole r " +
           "JOIN FETCH r.businessApplication ba " +
           "WHERE ba.businessAppName = :businessAppName " +
           "AND r.roleName IN :roleNames " +
           "AND r.isActive = true")
    List<BusinessAppRole> findByBusinessAppAndRoleNames(@Param("businessAppName") String businessAppName,
                                                      @Param("roleNames") List<String> roleNames);
}