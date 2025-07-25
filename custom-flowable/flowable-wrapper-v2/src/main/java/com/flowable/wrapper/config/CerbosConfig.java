package com.flowable.wrapper.config;

import dev.cerbos.sdk.CerbosBlockingClient;
import dev.cerbos.sdk.CerbosClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cerbos client configuration
 */
@Configuration
@Slf4j
public class CerbosConfig {

    @Value("${cerbos.endpoint:cerbos:3592}")
    private String cerbosEndpoint;

    @Value("${cerbos.tls.enabled:false}")
    private boolean tlsEnabled;

    @Bean
    public CerbosBlockingClient cerbosClient() {
        try {
            log.info("Initializing Cerbos client with endpoint: {}", cerbosEndpoint);
            
            CerbosClientBuilder builder = new CerbosClientBuilder(cerbosEndpoint).withInsecure()
                    .withPlaintext();

            if (!tlsEnabled) {
                builder = builder.withPlaintext();
            }
            
            CerbosBlockingClient client = builder.buildBlockingClient();
            
            log.info("Cerbos client initialized successfully");
            return client;
            
        } catch (Exception e) {
            log.error("Failed to initialize Cerbos client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Cerbos client", e);
        }
    }
}