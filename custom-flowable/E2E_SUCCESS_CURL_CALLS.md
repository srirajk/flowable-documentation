# End-to-End Success Curl Calls - Sanctions Management Workflow

This document contains all the successful curl calls for testing the complete Sanctions Management workflow with Cerbos integration and Four-Eyes Principle validation.

## Prerequisites
- Docker containers running (Cerbos, PostgreSQL, Application)
- Database initialized with data-new.sql
- Environment ready on localhost:8090

---

## 1. Register Workflow Metadata

```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/register" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: automation-user-2" \
    -d '{
      "processDefinitionKey": "sanctionsCaseManagement",
      "processName": "Sanctions L1-L2 Flow",
      "businessAppName": "Sanctions-Management",
      "description": "Level 1 and Level 2 sanctions case management workflow",
      "candidateGroupMappings": {
        "level1-maker": "level1-queue",
        "level1-checker": "level1-queue",
        "level1-supervisor": "level1-supervisor-queue",
        "level2-maker": "level2-queue",
        "level2-checker": "level2-queue",
        "level2-supervisor": "level2-supervisor-queue"
      }
    }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Workflow metadata registered successfully",
  "data": {
    "id": 1,
    "processDefinitionKey": "sanctionsCaseManagement",
    "businessAppName": "Sanctions-Management"
  }
}
```

---

## 2. Deploy BPMN Workflow

```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/deploy" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: automation-user-2" \
    -d '{
      "processDefinitionKey": "sanctionsCaseManagement"
    }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Workflow deployed successfully",
  "data": {
    "deploymentId": "deployment-123",
    "processDefinitionId": "sanctionsCaseManagement:1:proc-def-456"
  }
}
```

---

## 3. Start Process Instance

```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/process-instances/start" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: automation-user-2" \
    -d '{
      "processDefinitionKey": "sanctionsCaseManagement",
      "processInstanceName": "Sanctions Case #SC001",
      "variables": {
        "caseId": "SC001",
        "customerName": "John Doe",
        "sanctionType": "PEP",
        "riskLevel": "HIGH",
        "region": "US",
        "amount": 150000.00,
        "currency": "USD",
        "description": "High-risk PEP customer requiring L1 and L2 review"
      }
    }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Process instance started successfully",
  "data": {
    "processInstanceId": "proc-inst-789",
    "processDefinitionKey": "sanctionsCaseManagement",
    "businessKey": "SC001"
  }
}
```

---

## 4. View L1 Queue Tasks (US L1 Operator)

```bash
curl -X GET "http://localhost:8090/api/Sanctions-Management/queues/level1-queue/tasks" \
    -H "X-User-Id: us-l1-operator-1"
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "queueName": "level1-queue",
    "tasks": [
      {
        "taskId": "task-123",
        "taskName": "Level 1 Maker Review",
        "taskDefinitionKey": "l1_maker_review_task",
        "processInstanceId": "proc-inst-789",
        "assignee": null,
        "status": "OPEN",
        "priority": 0,
        "createdDate": "2025-01-20T10:00:00Z",
        "processVariables": {
          "caseId": "SC001",
          "customerName": "John Doe",
          "riskLevel": "HIGH"
        }
      },
      {
        "taskId": "task-124",
        "taskName": "Level 1 Checker Review",
        "taskDefinitionKey": "l1_checker_review_task",
        "processInstanceId": "proc-inst-789",
        "assignee": null,
        "status": "OPEN",
        "priority": 0,
        "createdDate": "2025-01-20T10:00:00Z"
      }
    ]
  }
}
```

---

## 5. Claim L1 Maker Task

```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/task-123/claim" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l1-operator-1" \
    -d '{}'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Task claimed successfully",
  "data": {
    "taskId": "task-123",
    "assignee": "us-l1-operator-1",
    "status": "CLAIMED",
    "claimedAt": "2025-01-20T10:05:00Z"
  }
}
```

---

## 6. Complete L1 Maker Task (Approve)

```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/task-123/complete" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l1-operator-1" \
    -d '{
      "action": "APPROVE",
      "comment": "Customer details verified, risk assessment completed. Approving for L1 checker review.",
      "formData": {
        "l1MakerDecision": "APPROVE",
        "l1MakerComment": "All documentation verified",
        "riskScore": 8.5
      }
    }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Task completed successfully",
  "data": {
    "taskId": "task-123",
    "completedBy": "us-l1-operator-1",
    "completedAt": "2025-01-20T10:15:00Z",
    "action": "APPROVE",
    "nextTasks": ["task-124"]
  }
}
```

---

## 7. Claim L1 Checker Task (Different User - Four-Eyes)

```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/task-124/claim" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l1-operator-2" \
    -d '{}'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Task claimed successfully",
  "data": {
    "taskId": "task-124",
    "assignee": "us-l1-operator-2",
    "status": "CLAIMED",
    "claimedAt": "2025-01-20T10:20:00Z"
  }
}
```

---

## 8. Complete L1 Checker Task (Approve)

```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/task-124/complete" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l1-operator-2" \
    -d '{
      "action": "APPROVE",
      "comment": "L1 Maker review validated. Customer risk profile confirmed. Escalating to L2.",
      "formData": {
        "l1CheckerDecision": "APPROVE",
        "l1CheckerComment": "Maker assessment validated",
        "escalationReason": "High risk case requires L2 approval"
      }
    }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Task completed successfully",
  "data": {
    "taskId": "task-124",
    "completedBy": "us-l1-operator-2",
    "completedAt": "2025-01-20T10:25:00Z",
    "action": "APPROVE",
    "nextTasks": ["task-225", "task-226"]
  }
}
```

---

## 9. View L2 Queue Tasks

```bash
curl -X GET "http://localhost:8090/api/Sanctions-Management/queues/level2-queue/tasks" \
    -H "X-User-Id: us-l2-operator-1"
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "queueName": "level2-queue",
    "tasks": [
      {
        "taskId": "task-225",
        "taskName": "Level 2 Maker Review",
        "taskDefinitionKey": "l2_maker_review_task",
        "processInstanceId": "proc-inst-789",
        "assignee": null,
        "status": "OPEN",
        "priority": 0,
        "createdDate": "2025-01-20T10:25:00Z"
      },
      {
        "taskId": "task-226",
        "taskName": "Level 2 Checker Review",
        "taskDefinitionKey": "l2_checker_review_task",
        "processInstanceId": "proc-inst-789",
        "assignee": null,
        "status": "OPEN",
        "priority": 0,
        "createdDate": "2025-01-20T10:25:00Z"
      }
    ]
  }
}
```

---

## 10. Claim and Complete L2 Maker Task

```bash
# Claim L2 Maker Task
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/task-225/claim" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l2-operator-1" \
    -d '{}'

# Complete L2 Maker Task
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/task-225/complete" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l2-operator-1" \
    -d '{
      "action": "APPROVE",
      "comment": "L2 review completed. High-risk case approved with enhanced monitoring.",
      "formData": {
        "l2MakerDecision": "APPROVE",
        "l2MakerComment": "Enhanced due diligence completed",
        "finalRiskAssessment": "APPROVED_WITH_MONITORING",
        "monitoringLevel": "ENHANCED"
      }
    }'
```

---

## 11. Claim and Complete L2 Checker Task (Final Approval)

```bash
# Claim L2 Checker Task
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/task-226/claim" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l2-operator-2" \
    -d '{}'

# Complete L2 Checker Task
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/task-226/complete" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l2-operator-2" \
    -d '{
      "action": "APPROVE",
      "comment": "Final L2 approval completed. Case approved for customer onboarding.",
      "formData": {
        "l2CheckerDecision": "APPROVE",
        "l2CheckerComment": "L2 Maker assessment validated and approved",
        "finalDecision": "APPROVED",
        "approvalDate": "2025-01-20T10:45:00Z"
      }
    }'
```

---

## 12. Verify Process Completion

```bash
curl -X GET "http://localhost:8090/api/Sanctions-Management/process-instances/proc-inst-789" \
    -H "X-User-Id: operation-user-1"
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "processInstanceId": "proc-inst-789",
    "processDefinitionKey": "sanctionsCaseManagement",
    "businessKey": "SC001",
    "status": "COMPLETED",
    "startTime": "2025-01-20T10:00:00Z",
    "endTime": "2025-01-20T10:45:00Z",
    "finalDecision": "APPROVED"
  }
}
```

---

## 13. Test Four-Eyes Principle Validation (Should Fail)

```bash
# Try to claim L1 Checker task with same user who completed L1 Maker
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/task-124/claim" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l1-operator-1" \
    -d '{}'
```

**Expected Response (Failure):**
```json
{
  "success": false,
  "error": "AUTHORIZATION_DENIED",
  "message": "Four-Eyes Principle violation: User cannot claim task that was completed by the same user in the parallel maker/checker flow",
  "details": {
    "userId": "us-l1-operator-1",
    "taskId": "task-124",
    "violationType": "FOUR_EYES_PRINCIPLE"
  }
}
```

---

## 14. Test Regional Access Control

```bash
# Try to access US region case with EU user (Should fail)
curl -X GET "http://localhost:8090/api/Sanctions-Management/queues/level1-queue/tasks" \
    -H "X-User-Id: eu-l1-operator-1"
```

**Expected Response (Filtered/Empty):**
```json
{
  "success": true,
  "data": {
    "queueName": "level1-queue",
    "tasks": []
  }
}
```

---

## 15. Health Check and System Status

```bash
# Application Health
curl -X GET "http://localhost:8090/actuator/health"

# Cerbos Health
curl -X GET "http://localhost:3592/_cerbos/health"

# Check Active Process Instances
curl -X GET "http://localhost:8090/api/Sanctions-Management/process-instances/active" \
    -H "X-User-Id: operation-user-1"
```

---

## Usage Notes

1. **Sequential Execution**: Run these commands in order for a complete E2E test
2. **User Context**: Each call uses appropriate user based on role and region
3. **Four-Eyes Validation**: Commands 13-14 demonstrate security features
4. **Regional Control**: Different regional users see different data
5. **Process Completion**: Full workflow from start to completion with proper validation

## Backup Commands for Testing

### Reset Process Instance (if needed)
```bash
curl -X DELETE "http://localhost:8090/api/Sanctions-Management/process-instances/proc-inst-789" \
    -H "X-User-Id: operation-user-1"
```

### View All Queues Status
```bash
curl -X GET "http://localhost:8090/api/Sanctions-Management/queues/status" \
    -H "X-User-Id: operation-user-1"
```

### Check User Permissions
```bash
curl -X GET "http://localhost:8090/api/Sanctions-Management/users/us-l1-operator-1/permissions" \
    -H "X-User-Id: operation-user-1"
```