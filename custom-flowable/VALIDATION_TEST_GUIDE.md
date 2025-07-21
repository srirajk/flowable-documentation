# Test Guide: Script-Based Task Validation

This guide provides a step-by-step walkthrough for testing the script-based task validation pattern using the `standardValidationPattern` workflow.

## Prerequisites

- The Flowable Wrapper API is running.
- You have a tool like `curl` or Postman to make API requests.

## Workflow Overview

The `standardValidationPattern` workflow (`simple-validation-script.bpmn20.xml`) has one user task, `submitData`. This task has the following validation rules, implemented in a script task:

1.  The `amount` variable must exist and be a number.
2.  The `amount` must not be greater than 5000.
3.  A variable named `receipt_resourceId` must exist (simulating a required file attachment).

## Step 1: Register and Deploy the Workflow

First, we need to register the workflow and its queue mappings, then deploy it.

### 1.1 Register Metadata

```bash
curl -X POST http://localhost:8090/api/workflow-metadata/register \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "standardValidationPattern",
    "processName": "Standard Validation Pattern Process",
    "description": "A process to demonstrate the standard script-based validation.",
    "candidateGroupMappings": {
      "level1-makers": "maker-queue"
    }
  }'
```

### 1.2 Deploy the BPMN

This command reads the BPMN file from the `definitions` directory and deploys it.

```bash
# Read the BPMN file content
BPMN_CONTENT=$(cat ./custom-flowable/definitions/simple-validation-script.bpmn20.xml | jq -Rs .)

# Deploy the workflow
curl -X POST http://localhost:8090/api/workflow-metadata/deploy \
  -H "Content-Type: application/json" \
  -d "{
    \"processDefinitionKey\": \"standardValidationPattern\",
    \"bpmnXml\": $BPMN_CONTENT,
    \"deploymentName\": \"Standard Validation Pattern v1.0\"
  }"
```

## Step 2: Start a Process Instance

Now, start an instance of the workflow.

```bash
curl -X POST http://localhost:8090/api/process-instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "standardValidationPattern",
    "businessKey": "VALIDATION-TEST-1"
  }'
```

This will create a new task in the `maker-queue`.

## Step 3: Test Validation Failure

Let's try to complete the task with invalid data.

### 3.1 Check the Queue and Get Task ID

```bash
curl http://localhost:8090/api/tasks/queue/maker-queue
```

From the response, copy the `taskId` of the task.

### 3.2 Claim the Task

Replace `{taskId}` with the ID from the previous step.

```bash
curl -X POST "http://localhost:8090/api/tasks/{taskId}/claim?userId=test-user"
```

### 3.3 Attempt to Complete with Invalid Data

We will submit an `amount` that is too high and omit the required `receipt_resourceId`.

```bash
curl -X POST http://localhost:8090/api/tasks/{taskId}/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "variables": {
      "amount": 6000
    }
  }'
```

### 3.4 Analyze the Failure Response

You should receive a `VALIDATION_FAILED` response. This confirms the API correctly detected the loopback.

```json
{
    "status": "VALIDATION_FAILED",
    "message": "Please correct the errors and resubmit",
    "validationErrors": [
        "Amount cannot exceed $5,000.",
        "A receipt attachment is mandatory."
    ],
    "attemptNumber": 1,
    "retryTaskId": "{new-task-id}", // Note the new task ID
    "processInstanceId": "...",
    "completedAt": "...",
    "completedBy": "test-user",
    "processActive": true
}
```

**Key things to note:**
- The `status` is `VALIDATION_FAILED`.
- The `validationErrors` array contains the user-friendly error messages from our script.
- The `attemptNumber` is 1.
- A new `retryTaskId` has been generated.

## Step 4: Test Successful Completion

Now, let's complete the task correctly using the `retryTaskId` from the previous step.

### 4.1 Check the Queue Again

You will see the new task in the `maker-queue`.

```bash
curl http://localhost:8090/api/tasks/queue/maker-queue
```

### 4.2 Claim the New Task

Use the `retryTaskId` to claim the new task.

```bash
curl -X POST "http://localhost:8090/api/tasks/{retryTaskId}/claim?userId=test-user"
```

### 4.3 Complete with Valid Data

This time, we provide a valid `amount` and the required `receipt_resourceId`.

```bash
curl -X POST http://localhost:8090/api/tasks/{retryTaskId}/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "variables": {
      "amount": 4500,
      "receipt_resourceId": "some-file-id-12345"
    }
  }'
```

### 4.4 Analyze the Success Response

This time, you should get a standard `COMPLETED` response, and the workflow will move to the `nextStage`.

```json
{
    "status": "COMPLETED",
    "message": "Task completed successfully",
    "taskId": "{retryTaskId}",
    "taskName": "Submit Data",
    "processInstanceId": "...",
    "completedAt": "...",
    "completedBy": "test-user",
    "processActive": true,
    "nextTaskId": "{id-of-next-stage-task}",
    "nextTaskName": "Next Stage",
    "nextTaskQueue": "default" // Or whatever queue is configured for the next task
}
```

This completes the testing of the script-based validation pattern. You have successfully tested both a validation failure and a successful completion.
