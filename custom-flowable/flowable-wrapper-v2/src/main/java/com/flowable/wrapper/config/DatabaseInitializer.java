package com.flowable.wrapper.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer {
    
    private final JdbcTemplate jdbcTemplate;
    
    @PostConstruct
    public void initializeDatabase() {
        try {
            log.info("Initializing database schema...");
            
            // Read schema.sql file
            ClassPathResource resource = new ClassPathResource("db/schema.sql");
            Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
            String sql = FileCopyUtils.copyToString(reader);
            
            // Execute the SQL
            jdbcTemplate.execute(sql);
            
            log.info("Database schema initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize database schema: {}", e.getMessage(), e);
            // Don't fail startup - tables might already exist
        }
    }
}