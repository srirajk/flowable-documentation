package com.flowable.wrapper.cerbos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Complete Cerbos authorization request containing principal, resource, and action
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CerbosRequest {
    
    /**
     * The user/principal requesting authorization
     */
    private CerbosPrincipal principal;
    
    /**
     * The resource being accessed
     */
    private CerbosResourceContext resource;
    
    /**
     * The action being performed
     */
    private String action;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CerbosPrincipal {
        
        /**
         * User ID
         */
        private String id;
        
        /**
         * User roles for the specific business application
         */  
        private java.util.List<String> roles;
        
        /**
         * User attributes (department, region, clearance, etc.)
         */
        private Map<String, Object> attr;
    }
}