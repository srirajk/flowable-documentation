package com.flowable.wrapper.controller;

import com.flowable.wrapper.dto.request.DeployWorkflowRequest;
import com.flowable.wrapper.dto.request.RegisterWorkflowMetadataRequest;
import com.flowable.wrapper.dto.response.WorkflowMetadataResponse;
import com.flowable.wrapper.exception.WorkflowException;
import com.flowable.wrapper.service.WorkflowMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflow-metadata")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Workflow Metadata", description = "APIs for managing workflow metadata and deployments")
public class WorkflowMetadataController {
    
    private final WorkflowMetadataService workflowMetadataService;
    
    @PostMapping("/register")
    @Operation(summary = "Register workflow metadata", 
              description = "Register a new workflow with its task-to-queue mappings")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Workflow metadata registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or workflow already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<WorkflowMetadataResponse> registerWorkflowMetadata(
            @Valid @RequestBody RegisterWorkflowMetadataRequest request) throws WorkflowException {
        
        log.info("Registering workflow metadata: {}", request.getProcessDefinitionKey());
        WorkflowMetadataResponse response = workflowMetadataService.registerWorkflowMetadata(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/deploy")
    @Operation(summary = "Deploy BPMN workflow", 
              description = "Deploy a BPMN workflow to the Flowable engine")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Workflow deployed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or deployment failed"),
        @ApiResponse(responseCode = "404", description = "Workflow metadata not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<WorkflowMetadataResponse> deployWorkflow(
            @Valid @RequestBody DeployWorkflowRequest request) throws WorkflowException {
        
        log.info("Deploying workflow: {}", request.getProcessDefinitionKey());
        WorkflowMetadataResponse response = workflowMetadataService.deployWorkflow(request);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{processDefinitionKey}")
    @Operation(summary = "Get workflow metadata", 
              description = "Retrieve workflow metadata by process definition key")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Workflow metadata found"),
        @ApiResponse(responseCode = "404", description = "Workflow metadata not found")
    })
    public ResponseEntity<WorkflowMetadataResponse> getWorkflowMetadata(
            @Parameter(description = "Process definition key", required = true)
            @PathVariable String processDefinitionKey) {
        
        log.info("Getting workflow metadata for process: {}", processDefinitionKey);
        WorkflowMetadataResponse response = workflowMetadataService.getWorkflowMetadata(processDefinitionKey);
        
        return ResponseEntity.ok(response);
    }
}