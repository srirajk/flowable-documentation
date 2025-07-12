package com.flowable.wrapper.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response after task completion")
public class TaskCompletionResponse {
    
    @Schema(description = "Completed task ID", example = "5d7e9c3a-1234-5678-9abc-def012345678")
    private String taskId;
    
    @Schema(description = "Task name", example = "Manager Approval")
    private String taskName;
    
    @Schema(description = "Process instance ID", example = "5d7e9c3a-1234-5678-9abc-def012345678")
    private String processInstanceId;
    
    @Schema(description = "Completion time")
    private Instant completedAt;
    
    @Schema(description = "User who completed the task", example = "john.doe")
    private String completedBy;
    
    @Schema(description = "Is the process instance still active", example = "true")
    private boolean processActive;
    
    @Schema(description = "Next task ID if process is still active", example = "5d7e9c3a-1234-5678-9abc-def012345678")
    private String nextTaskId;
    
    @Schema(description = "Next task name if process is still active", example = "Finance Review")
    private String nextTaskName;
    
    @Schema(description = "Next task queue if process is still active", example = "finance-queue")
    private String nextTaskQueue;
}