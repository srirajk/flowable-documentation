package com.flowable.wrapper.controller;

import com.flowable.wrapper.dto.request.StartProcessRequest;
import com.flowable.wrapper.dto.response.ProcessInstanceResponse;
import com.flowable.wrapper.exception.WorkflowException;
import com.flowable.wrapper.service.ProcessInstanceService;
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
@RequestMapping("/api/process-instances")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Process Instances", description = "APIs for managing process instances")
public class ProcessInstanceController {
    
    private final ProcessInstanceService processInstanceService;
    
    @PostMapping("/start")
    @Operation(summary = "Start a new process instance", 
              description = "Start a new instance of a deployed workflow")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Process instance started successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Workflow not found or not deployed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ProcessInstanceResponse> startProcess(
            @Valid @RequestBody StartProcessRequest request) throws WorkflowException {
        
        log.info("Starting process instance for workflow: {}", request.getProcessDefinitionKey());
        ProcessInstanceResponse response = processInstanceService.startProcess(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{processInstanceId}")
    @Operation(summary = "Get process instance details", 
              description = "Retrieve details of a specific process instance")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Process instance found"),
        @ApiResponse(responseCode = "404", description = "Process instance not found")
    })
    public ResponseEntity<ProcessInstanceResponse> getProcessInstance(
            @Parameter(description = "Process instance ID", required = true)
            @PathVariable String processInstanceId) {
        
        log.info("Getting process instance: {}", processInstanceId);
        ProcessInstanceResponse response = processInstanceService.getProcessInstance(processInstanceId);
        
        return ResponseEntity.ok(response);
    }
}