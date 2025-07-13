# Task Validation Pattern - Design Guidelines

## Overview

This document describes the standard pattern for implementing task validation with rejection handling in our queue-based Flowable system. All engineers building workflows MUST follow this pattern to ensure consistent user experience and proper queue management.

## Mental Model

### Traditional Approach (Not Recommended)
```
User Task → Business Logic Decides → Create NEW Rejection Task
```
**Problem**: Users think they're done, but later find a new task in their queue.

### Our Approach (Required Pattern)
```
User Task → Validation → Same Task Loops Back if Failed
```
**Benefit**: Immediate feedback, same task context, better UX.

## Core Concepts

### 1. Task Validation Flow

Think of it like a quality control checkpoint:

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│  User Task  │────>│ Service Task │────>│  Gateway    │
│ (Data Entry)│     │ (Validation) │     │  (Router)   │
└─────────────┘     └──────────────┘     └──────┬──────┘
       ↑                                         │ │
       │                                         │ │ Rejected
       └─────────────────────────────────────────┘ │
                                                    │ Approved
                                                    ↓
                                              Next Stage
```

### 2. Key Principles

1. **Validation is Synchronous** - Happens immediately after task completion
2. **Same Task Loops Back** - Failed validation returns to the SAME task definition
3. **Context is Preserved** - User sees their previous input with error messages
4. **Queue Remains Consistent** - Task reappears in the same queue

## Implementation Requirements

### 1. BPMN Design Rules

#### Required Elements
- **User Task**: Must have a unique `id` (e.g., `submitExpenseReport`)
- **Service Task**: Immediately follows user task for validation
- **Exclusive Gateway**: Routes based on validation result
- **Loop-back Flow**: Connects gateway back to user task on failure

#### Required Variables
Every validation service task MUST set:
```
- {taskId}Valid: boolean (e.g., task1Valid)
- {taskId}ValidationError: string/list (e.g., task1ValidationError)
- {taskId}AttemptCount: integer (e.g., task1AttemptCount)
```

### 2. BPMN Example Template

```xml
<process id="standardValidationPattern">
  
  <!-- User Task -->
  <userTask id="submitData" name="Submit Data">
    <extensionElements>
      <!-- REQUIRED: Set candidate groups for queue mapping -->
      <flowable:candidateGroups>level1-makers</flowable:candidateGroups>
      
      <!-- REQUIRED: Track attempt count -->
      <flowable:taskListener event="create" 
        class="com.flowable.wrapper.listeners.IncrementAttemptListener" />
    </extensionElements>
  </userTask>
  
  <!-- Validation Service Task -->
  <serviceTask id="validateData" name="Validate Submission"
    flowable:delegateExpression="${dataValidator}" />
  
  <!-- Routing Gateway -->
  <exclusiveGateway id="validationGateway" name="Is Valid?" />
  
  <!-- Success Path -->
  <sequenceFlow sourceRef="validationGateway" targetRef="nextStage">
    <conditionExpression>${submitDataValid == true}</conditionExpression>
  </sequenceFlow>
  
  <!-- REQUIRED: Failure Loop Back -->
  <sequenceFlow sourceRef="validationGateway" targetRef="submitData">
    <conditionExpression>${submitDataValid == false}</conditionExpression>
  </sequenceFlow>
  
</process>
```

### 3. Service Task Implementation

```java
@Component
public class DataValidator implements JavaDelegate {
    
    @Override
    public void execute(DelegateExecution execution) {
        // REQUIRED: Follow naming convention
        String taskId = "submitData"; // Must match BPMN task ID
        
        // Get attempt count
        Integer attemptCount = (Integer) execution.getVariable(taskId + "AttemptCount");
        if (attemptCount == null) attemptCount = 0;
        
        // Perform validation
        ValidationResult result = validate(execution.getVariables());
        
        // REQUIRED: Set standard variables
        execution.setVariable(taskId + "Valid", result.isValid());
        execution.setVariable(taskId + "ValidationError", result.getErrors());
        execution.setVariable(taskId + "AttemptCount", attemptCount + 1);
        
        // Optional: Set additional context
        if (!result.isValid()) {
            execution.setVariable(taskId + "RejectedAt", Instant.now());
            execution.setVariable(taskId + "RejectedBy", "SYSTEM");
        }
    }
}
```

## Queue System Integration

### How Our System Detects Rejections

When a task is completed, our system:

1. Checks if the same `taskDefinitionKey` appears in active tasks
2. Examines validation variables (`{taskId}Valid = false`)
3. Returns appropriate response to the user

### API Response Pattern

```json
// Successful validation
{
    "status": "COMPLETED",
    "message": "Task completed successfully",
    "nextStep": "Awaiting manager approval"
}

// Failed validation - task loops back
{
    "status": "VALIDATION_FAILED",
    "message": "Please correct the errors and resubmit",
    "validationErrors": [
        "Amount exceeds limit of $5000",
        "Receipt attachment required for amounts over $100"
    ],
    "attemptNumber": 2,
    "retryTaskId": "task-456"
}
```

## Parallel Task Validation

For workflows with parallel tasks that must all succeed:

```
Level 1:
┌──────────┐     ┌──────────┐
│  Task A  │     │  Task B  │
└────┬─────┘     └────┬─────┘
     │                │
     ▼                ▼
┌──────────┐     ┌──────────┐
│Validate A│     │Validate B│
└────┬─────┘     └────┬─────┘
     │                │
     ├──<loopback>────┤
     │                │
     └──────┬─────────┘
            ▼
      Join Gateway
```

### Rules for Parallel Validation:
1. Each parallel branch has its own validation loop
2. Join gateway waits for ALL branches to have `valid=true`
3. Each task can fail/retry independently
4. Use unique variable names per task (task1Valid, task2Valid)

## Common Patterns

### 1. Simple Validation
- One task → One validation → Continue or retry

### 2. Maker-Checker Pattern
- Maker submits → Validation → Checker reviews → Validation → Next level

### 3. Parallel Validation
- Multiple tasks in parallel → Each validates independently → Join when all valid

### 4. Multi-Level Validation
- Level 1 validation (basic) → Level 2 validation (advanced) → Progressive checks

## Checklist for Engineers

Before deploying a workflow, ensure:

- [ ] Every user task that requires validation has a following service task
- [ ] Service tasks set the required variables: `{taskId}Valid`, `{taskId}ValidationError`
- [ ] Gateways have proper conditions checking the `Valid` variable
- [ ] Loop-back sequence flows connect to the original task
- [ ] Task IDs follow naming conventions
- [ ] Candidate groups are set for queue mapping
- [ ] Validation error messages are user-friendly
- [ ] Attempt counters are implemented
- [ ] Documentation includes validation rules

## Error Messages Best Practices

### Good Error Messages:
- "Invoice amount $5,234 exceeds approval limit of $5,000"
- "Missing required attachment: Receipt for expense"
- "Invalid account number format. Expected: XXX-XXXXX-XX"

### Bad Error Messages:
- "Validation failed"
- "Error in task"
- "Invalid data"

## Testing Guidelines

1. Test the happy path (all validations pass)
2. Test each validation failure independently
3. Test multiple attempts (ensure counter increments)
4. Test parallel task failures
5. Verify error messages are helpful
6. Confirm tasks reappear in correct queues

## Migration Guide

For existing workflows:
1. Identify tasks that need validation
2. Add service tasks after user tasks
3. Add gateways with proper routing
4. Update task services to set required variables
5. Test thoroughly before deployment

---

**Remember**: This pattern ensures users get immediate feedback and understand exactly what needs to be corrected. Following these guidelines creates a consistent, user-friendly experience across all our workflows.