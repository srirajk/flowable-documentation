# Vacation Request Workflow Test Guide

This guide demonstrates the complete end-to-end testing of the VacationRequest workflow using the custom Flowable wrapper API.

## Workflow Overview

The VacationRequest workflow (`VacationRequest.bpmn20.xml`) implements a simple approval process:

1. **Employee** initiates vacation request
2. **Manager** reviews and approves/rejects (routed to `manager-queue`)
3. If **approved** → Process ends with email notification
4. If **rejected** → Employee can adjust and resubmit (routed to `default` queue)

## Prerequisites

- Docker services running (`docker-compose up -d`)
- Flowable wrapper service available at `http://localhost:8090`

## Complete Test Scenarios

### Test 1: Approved Vacation Request

#### 1. Register Workflow Metadata
```bash
curl -X POST http://localhost:8090/api/workflow-metadata/register \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "vacationRequest",
    "processName": "Vacation request",
    "description": "Original vacation request workflow",
    "candidateGroupMappings": {
      "management": "manager-queue"
    }
  }'
```

**Response:**
```json
{
  "id": 3,
  "processDefinitionKey": "vacationRequest",
  "processName": "Vacation request",
  "candidateGroupMappings": {
    "management": "manager-queue"
  },
  "deployed": false
}
```

#### 2. Deploy BPMN Workflow
```bash
BPMN_CONTENT=$(cat definitions/VacationRequest.bpmn20.xml | jq -Rs .)
curl -X POST http://localhost:8090/api/workflow-metadata/deploy \
  -H "Content-Type: application/json" \
  -d "{
    \"processDefinitionKey\": \"vacationRequest\",
    \"bpmnXml\": $BPMN_CONTENT,
    \"deploymentName\": \"Vacation Request v1.0\"
  }"
```

**Response:**
```json
{
  "id": 3,
  "deployed": true,
  "deploymentId": "4842a0ef-60ad-11f0-9630-0242ac130003",
  "taskQueueMappings": [
    {
      "taskId": "handleRequest",
      "taskName": "Handle vacation request",
      "candidateGroups": ["management"],
      "queue": "manager-queue"
    },
    {
      "taskId": "adjustVacationRequestTask",
      "taskName": "Adjust vacation request",
      "candidateGroups": [],
      "queue": "default"
    }
  ]
}
```

#### 3. Start Vacation Request Process
```bash
curl -X POST http://localhost:8090/api/process-instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "vacationRequest",
    "businessKey": "VAC-2025-004",
    "variables": {
      "employeeName": "Alice Johnson",
      "numberOfDays": 3,
      "startDate": "2025-02-10",
      "vacationMotivation": "Family time"
    }
  }'
```

**Response:**
```json
{
  "processInstanceId": "4e0e8bc2-60ad-11f0-9630-0242ac130003",
  "processDefinitionKey": "vacationRequest",
  "businessKey": "VAC-2025-004",
  "variables": {
    "employeeName": "Alice Johnson",
    "numberOfDays": 3,
    "startDate": "2025-02-10",
    "vacationMotivation": "Family time"
  }
}
```

#### 4. Check Manager Queue
```bash
curl http://localhost:8090/api/tasks/queue/manager-queue
```

**Response:**
```json
[{
  "taskId": "4e0f0100-60ad-11f0-9630-0242ac130003",
  "taskName": "Handle vacation request",
  "queueName": "manager-queue",
  "assignee": null,
  "status": "OPEN",
  "taskData": {
    "description": "Alice Johnson would like to take 3 day(s) of vacation (Motivation: Family time)."
  }
}]
```

#### 5. Manager Claims Task
```bash
curl -X POST "http://localhost:8090/api/tasks/4e0f0100-60ad-11f0-9630-0242ac130003/claim?userId=manager1"
```

**Response:**
```json
{
  "taskId": "4e0f0100-60ad-11f0-9630-0242ac130003",
  "assignee": "manager1",
  "status": "CLAIMED"
}
```

#### 6. Manager Approves Request
```bash
curl -X POST http://localhost:8090/api/tasks/4e0f0100-60ad-11f0-9630-0242ac130003/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "manager1",
    "variables": {
      "vacationApproved": "true",
      "managerMotivation": "Approved"
    }
  }'
```

**Response:**
```json
{
  "taskId": "4e0f0100-60ad-11f0-9630-0242ac130003",
  "completedBy": "manager1",
  "processActive": false,
  "nextTaskId": null
}
```

**Result:** Process completed successfully. Vacation approved.

---

### Test 2: Rejected Vacation Request with Adjustment

#### 1. Start Another Vacation Request
```bash
curl -X POST http://localhost:8090/api/process-instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "vacationRequest",
    "businessKey": "VAC-2025-005",
    "variables": {
      "employeeName": "Bob Smith",
      "numberOfDays": 10,
      "startDate": "2025-03-01",
      "vacationMotivation": "Personal vacation"
    }
  }'
```

**Response:**
```json
{
  "processInstanceId": "6936e88f-60ad-11f0-9630-0242ac130003",
  "businessKey": "VAC-2025-005"
}
```

#### 2. Find Task in Manager Queue
```bash
TASK_ID=$(curl -s http://localhost:8090/api/tasks/queue/manager-queue | \
  jq -r '.[] | select(.businessKey == "VAC-2025-005") | .taskId')
echo "Task ID: $TASK_ID"
```

#### 3. Manager Claims and Rejects
```bash
# Claim task
curl -X POST "http://localhost:8090/api/tasks/$TASK_ID/claim?userId=manager2"

# Reject request
curl -X POST http://localhost:8090/api/tasks/$TASK_ID/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "manager2",
    "variables": {
      "vacationApproved": "false",
      "managerMotivation": "Too many people out during that period"
    }
  }'
```

**Response:**
```json
{
  "completedBy": "manager2",
  "processActive": true,
  "nextTaskId": "770154ba-60ad-11f0-9630-0242ac130003",
  "nextTaskName": "Adjust vacation request",
  "nextTaskQueue": "default"
}
```

#### 4. Check Default Queue for Employee Task
```bash
curl http://localhost:8090/api/tasks/queue/default
```

**Response:**
```json
[{
  "taskId": "770154ba-60ad-11f0-9630-0242ac130003",
  "taskName": "Adjust vacation request",
  "queueName": "default",
  "assignee": "Bob Smith",
  "taskData": {
    "description": "Your manager has disapproved your vacation request for 10 days.\n        Reason: Too many people out during that period"
  }
}]
```

**Result:** Rejection flow working correctly. Task routed to employee for adjustment.

---

## Queue Routing Summary

| Task | Assigned To | Queue |
|------|------------|--------|
| Handle vacation request | management group | manager-queue |
| Adjust vacation request | Specific employee | default |

## Key Observations

1. **Queue Routing**: Tasks are correctly routed based on candidate groups
2. **Process Variables**: All variables (vacationApproved, managerMotivation) must match BPMN definition exactly
3. **Task Assignment**: Manager tasks use candidate groups, employee adjustment tasks use specific assignment
4. **Process Flow**: Both approval and rejection paths execute correctly

## Tips for Testing

1. Use exact variable names from BPMN (e.g., `vacationApproved` not `approved`)
2. Variable values must be strings (e.g., `"true"` not `true`)
3. Check task descriptions to verify correct data flow
4. Monitor queue assignments to ensure proper routing