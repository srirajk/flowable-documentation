# Task Validation Pattern - Design Guidelines (Script-Based)

## Overview

This document describes the standard, **code-free** pattern for implementing task validation with rejection handling in our queue-based Flowable system. All engineers building workflows MUST follow this pattern to ensure consistent user experience and proper queue management without writing or deploying custom Java code.

Validation is performed using **Script Tasks** directly within the BPMN definition.

## Mental Model

### Traditional Approach (Not Recommended)
```
User Task → Business Logic Decides → Create NEW Rejection Task
```
**Problem**: Users think they're done, but later find a new task in their queue.

### Our Approach (Required Pattern)
```
User Task → Validation Script → Same Task Loops Back if Failed
```
**Benefit**: Immediate feedback, same task context, better UX, **no Java code required**.

## Core Concepts

### 1. Task Validation Flow

The flow uses a `scriptTask` instead of a `serviceTask` for validation.

```
┌─────────────┐     ┌────────────────┐     ┌─────────────┐
│  User Task  │────>│  Script Task   │────>│  Gateway    │
│ (Data Entry)│     │ (Validation)   │     │  (Router)   │
└─────────────┘     └────────────────┘     └──────┬──────┘
       ↑                                         │ │
       │ Rejected                                │ │
       └────────────────────┬────────────────────┘ │
                            │                      │ Approved
┌───────────────────┐       │                      ↓
│ Increment Tries   │<──────┘                Next Stage
│ (Script Task)     │
└───────────────────┘
```

### 2. Key Principles

1.  **Validation is Synchronous** - Happens immediately after task completion.
2.  **Self-Contained Logic** - Validation rules are written in Groovy/Juel directly in the BPMN XML.
3.  **Same Task Loops Back** - Failed validation returns to the SAME task definition.
4.  **Context is Preserved** - User sees their previous input with error messages.
5.  **Queue Remains Consistent** - Task reappears in the same queue.

## Implementation Requirements

### 1. BPMN Design Rules

#### Required Elements
- **User Task**: Must have a unique `id` (e.g., `submitExpenseReport`).
- **Script Task**: Immediately follows the user task for validation. Must be configured for `groovy`.
- **Exclusive Gateway**: Routes based on the validation result.
- **Loop-back Flow**: Connects the gateway back to the user task on failure.
- **Increment Script Task**: A second, smaller script task on the failure path to increment the attempt counter.

#### Required Variables
Every validation script task MUST set:
```
- {taskId}Valid: boolean (e.g., submitDataValid)
- {taskId}ValidationError: string or list (e.g., submitDataValidationError)
```
The increment script task MUST set:
```
- {taskId}AttemptCount: integer (e.g., submitDataAttemptCount)
```

### 2. BPMN Example Template (Code-Free)

This is the standard pattern to be used.

```xml
<process id="standardValidationPattern">

  <startEvent id="start"/>

  <!-- 1. User Task -->
  <userTask id="submitData" name="Submit Data" flowable:candidateGroups="level1-makers">
  </userTask>

  <!-- 2. Validation Script Task -->
  <scriptTask id="validateData" name="Validate Submission" scriptFormat="groovy">
    <script>
      <![CDATA[
        // REQUIRED: Follow naming convention for variables
        def taskId = "submitData"; // Must match the User Task ID

        // --- Start of User-Defined Validation Logic ---

        def errors = new java.util.ArrayList();
        
        // Example 1: Check for presence of a variable
        if (!execution.hasVariable("amount") || execution.getVariable("amount") == null) {
          errors.add("Amount is a required field.");
        } else {
          // Example 2: Check for business rule
          def amount = execution.getVariable("amount") as Integer;
          if (amount > 5000) {
            errors.add("Amount cannot exceed \$5,000.");
          }
        }

        // Example 3: Check for required attachment (variable ending in '_resourceId')
        if (!execution.hasVariable("receipt_resourceId")) {
            errors.add("A receipt attachment is mandatory.");
        }

        // --- End of User-Defined Validation Logic ---

        // REQUIRED: Set standard output variables
        if (errors.isEmpty()) {
          execution.setVariable(taskId + "Valid", true);
          execution.setVariable(taskId + "ValidationError", ""); // Clear previous errors
        } else {
          execution.setVariable(taskId + "Valid", false);
          execution.setVariable(taskId + "ValidationError", errors);
        }
      ]]>
    </script>
  </scriptTask>

  <!-- 3. Routing Gateway -->
  <exclusiveGateway id="validationGateway" name="Is Valid?" />

  <!-- 4. Sequence Flows -->
  <sequenceFlow id="flowToValidation" sourceRef="submitData" targetRef="validateData" />
  <sequenceFlow id="flowToGateway" sourceRef="validateData" targetRef="validationGateway" />

  <sequenceFlow id="flowToSubmitData" sourceRef="start" targetRef="submitData"/>

  <!-- Success Path -->
  <sequenceFlow id="successPath" sourceRef="validationGateway" targetRef="nextStage">
    <conditionExpression>${execution.getVariable('submitDataValid') == true}</conditionExpression>
  </sequenceFlow>

  <!-- Failure Path -->
  <sequenceFlow id="failurePath" sourceRef="validationGateway" targetRef="incrementTries">
    <conditionExpression>${execution.getVariable('submitDataValid') == false}</conditionExpression>
  </sequenceFlow>
  
  <!-- 5. Increment Attempt Count Script Task -->
  <scriptTask id="incrementTries" name="Increment Attempt Count" scriptFormat="groovy">
    <script>
      <![CDATA[
        def taskId = "submitData";
        def currentAttempts = execution.getVariable(taskId + "AttemptCount") ?: 0;
        execution.setVariable(taskId + "AttemptCount", currentAttempts + 1);
      ]]>
    </script>
  </scriptTask>
  
  <!-- 6. Loop-back Flow -->
  <sequenceFlow id="loopback" sourceRef="incrementTries" targetRef="submitData" />

  <!-- Placeholder for next stage -->
  <userTask id="nextStage" name="Next Stage" />

</process>
```

### 3. Maker-Checker Pattern Example

To validate after both a maker and a checker, you simply duplicate the pattern.

```
Maker Task → Validate Maker Script → Gateway → Checker Task → Validate Checker Script → Gateway → ...
   ↑                 |                     |      ↑                  |                      |
   └----(loop)-------┘                     |      └-----(loop)--------┘                      |
                                           |                                                |
                                           └----------------(next)--------------------------┘
```

## Queue System Integration

### How Our System Detects Rejections

When a task is completed, our system:
1.  Checks if the process is still active and if the **next active task has the same ID** as the one just completed.
2.  If so, it reads the `{taskId}Valid`, `{taskId}ValidationError`, and `{taskId}AttemptCount` variables.
3.  It then returns a special API response indicating validation failure.

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
        "Amount cannot exceed $5,000.",
        "A receipt attachment is mandatory."
    ],
    "attemptNumber": 2,
    "retryTaskId": "task-456" // The ID of the new task instance
}
```

## Checklist for Engineers

Before deploying a workflow, ensure:

- [ ] Every user task requiring validation is followed by a `scriptTask`.
- [ ] The validation `scriptTask` sets the `{taskId}Valid` and `{taskId}ValidationError` variables.
- [ ] The gateway correctly checks the `{taskId}Valid` variable.
- [ ] The failure path includes a `scriptTask` to increment `{taskId}AttemptCount`.
- [ ] The loop-back flow connects from the incrementer back to the original user task.
- [ ] Task IDs in the script match the actual `userTask` ID.
- [ ] Candidate groups are set on the `userTask` for queue mapping.
- [ ] Validation error messages are user-friendly.

---

**Remember**: This pattern ensures users get immediate feedback and understand exactly what needs to be corrected, all without requiring any changes to the wrapper API's Java code.
