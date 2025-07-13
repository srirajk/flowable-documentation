# Queue System - Validation Pattern Integration Guide

## Overview

This guide explains how our queue-based task management system handles the validation pattern, providing immediate feedback when tasks fail business validation.

## System Behavior

### Task Completion Flow

```
1. User completes task via API
2. Flowable executes validation (synchronous)
3. System detects validation outcome
4. Returns appropriate response
```

### Detection Logic

When `TaskService.completeTask()` is called:

```java
public TaskCompletionResponse completeTask(String taskId, CompleteTaskRequest request) {
    // 1. Complete the task
    taskService.complete(taskId, variables);
    
    // 2. Check what happened
    List<Task> activeTasks = taskService.createTaskQuery()
        .processInstanceId(processInstanceId)
        .active()
        .list();
    
    // 3. Detect if same task reappeared (validation failed)
    Optional<Task> sameTaskReappeared = activeTasks.stream()
        .filter(t -> t.getTaskDefinitionKey().equals(completedTaskDefinitionKey))
        .findFirst();
    
    if (sameTaskReappeared.isPresent()) {
        // Validation failed - task looped back
        return buildValidationFailedResponse(sameTaskReappeared.get());
    }
    
    // Normal flow continues...
}
```

## API Response Patterns

### Success Response
```json
{
    "taskId": "task-123",
    "taskName": "Submit Expense Report",
    "status": "COMPLETED",
    "processActive": true,
    "validationResult": "PASSED",
    "nextStep": {
        "description": "Sent to manager for approval",
        "queue": "manager-queue"
    }
}
```

### Validation Failed Response
```json
{
    "taskId": "task-123",
    "taskName": "Submit Expense Report",
    "status": "VALIDATION_FAILED",
    "processActive": true,
    "validationResult": "FAILED",
    "retryRequired": true,
    "retryTask": {
        "taskId": "task-456",
        "taskName": "Submit Expense Report",
        "queue": "employee-queue"
    },
    "validationErrors": [
        "Receipt required for expenses over $100",
        "Description must be at least 10 characters"
    ],
    "attemptNumber": 2,
    "preservedData": {
        "amount": 250,
        "description": "Lunch",
        "hasReceipt": false
    }
}
```

## Queue Task Table Behavior

### On Initial Task Creation
```sql
INSERT INTO queue_tasks (
    task_id, 
    task_name, 
    queue_name, 
    status,
    metadata
) VALUES (
    'task-123',
    'Submit Expense Report',
    'employee-queue',
    'OPEN',
    '{"attemptNumber": 1}'
);
```

### On Validation Failure
```sql
-- Original task marked complete
UPDATE queue_tasks 
SET status = 'COMPLETED', 
    completed_at = NOW(),
    metadata = jsonb_set(metadata, '{validationFailed}', 'true')
WHERE task_id = 'task-123';

-- New task created for retry
INSERT INTO queue_tasks (
    task_id,
    task_name,
    queue_name,
    status,
    metadata
) VALUES (
    'task-456',
    'Submit Expense Report',
    'employee-queue',
    'OPEN',
    '{"attemptNumber": 2, "previousTaskId": "task-123", "validationErrors": [...]}'
);
```

## Implementation Requirements

### 1. Enhanced Task Completion Response

```java
@Data
@Builder
public class TaskCompletionResponse {
    private String taskId;
    private String taskName;
    private String status; // COMPLETED, VALIDATION_FAILED, ERROR
    private boolean processActive;
    
    // Validation specific fields
    private ValidationResult validationResult;
    private boolean retryRequired;
    private RetryTaskInfo retryTask;
    private List<String> validationErrors;
    private Integer attemptNumber;
    private Map<String, Object> preservedData;
}
```

### 2. Validation Detection Service

```java
@Service
public class ValidationDetectionService {
    
    public boolean isValidationFailure(
            String completedTaskDefKey, 
            List<Task> activeTasks,
            Map<String, Object> processVariables) {
        
        // Check 1: Same task definition reappeared
        boolean sameTaskExists = activeTasks.stream()
            .anyMatch(t -> t.getTaskDefinitionKey().equals(completedTaskDefKey));
        
        if (!sameTaskExists) {
            return false;
        }
        
        // Check 2: Validation variables indicate failure
        String validKey = completedTaskDefKey + "Valid";
        Boolean isValid = (Boolean) processVariables.get(validKey);
        
        return Boolean.FALSE.equals(isValid);
    }
    
    public ValidationInfo extractValidationInfo(
            String taskDefKey,
            Map<String, Object> processVariables) {
        
        return ValidationInfo.builder()
            .errors((List<String>) processVariables.get(taskDefKey + "ValidationError"))
            .attemptCount((Integer) processVariables.get(taskDefKey + "AttemptCount"))
            .rejectedAt((Instant) processVariables.get(taskDefKey + "RejectedAt"))
            .build();
    }
}
```

### 3. Queue Task Metadata Enhancement

Store validation context in the metadata:

```json
{
    "attemptNumber": 2,
    "previousTaskId": "task-123",
    "validationErrors": [
        "Receipt required for expenses over $100"
    ],
    "validationFailedAt": "2024-01-15T10:30:00Z",
    "preservedFormData": {
        "amount": 250,
        "description": "Lunch meeting"
    }
}
```

## User Interface Considerations

### Display Retry Context

When a user opens a retry task:

1. Show previous values (preserved data)
2. Highlight validation errors clearly
3. Display attempt number
4. Show validation rules upfront

### Example UI Response

```json
{
    "task": {
        "id": "task-456",
        "name": "Submit Expense Report",
        "description": "Please correct the errors and resubmit"
    },
    "validationContext": {
        "attemptNumber": 2,
        "errors": [
            {
                "field": "receipt",
                "message": "Receipt required for expenses over $100"
            },
            {
                "field": "description",
                "message": "Description must be at least 10 characters"
            }
        ],
        "previousValues": {
            "amount": 250,
            "description": "Lunch",
            "hasReceipt": false
        },
        "helpText": "Tip: You can upload receipts by clicking the attachment button"
    }
}
```

## Monitoring and Analytics

### Metrics to Track

1. **Validation Failure Rate** - Per task type
2. **Average Attempts** - Before successful completion
3. **Common Validation Errors** - To improve UX
4. **Time to Resolution** - From first attempt to success

### Sample Queries

```sql
-- Validation failure rate by task
SELECT 
    task_definition_key,
    COUNT(CASE WHEN metadata->>'validationFailed' = 'true' THEN 1 END)::float / 
    COUNT(*)::float as failure_rate
FROM queue_tasks
GROUP BY task_definition_key;

-- Average attempts per successful completion
SELECT 
    task_definition_key,
    AVG((metadata->>'attemptNumber')::int) as avg_attempts
FROM queue_tasks
WHERE status = 'COMPLETED'
  AND metadata->>'validationFailed' IS NULL
GROUP BY task_definition_key;
```

## Best Practices

1. **Always Return Clear Feedback** - Users should know immediately if validation failed
2. **Preserve User Input** - Don't make users re-enter everything
3. **Specific Error Messages** - Tell exactly what's wrong and how to fix it
4. **Track Attempts** - Monitor if users are struggling with certain validations
5. **Consider Client-Side Validation** - Catch simple errors before submission

## Migration Path

For existing workflows without validation pattern:

1. **Phase 1**: Add validation tracking to new workflows
2. **Phase 2**: Update high-volume workflows
3. **Phase 3**: Retrofit remaining workflows
4. **Phase 4**: Add analytics dashboard

## Summary

This validation pattern integration ensures:
- Immediate feedback on validation failures
- Consistent user experience across all workflows
- Clear tracking of validation attempts
- Better error handling and user guidance

By detecting when tasks loop back due to validation failure, we can provide a superior user experience compared to traditional BPM systems.