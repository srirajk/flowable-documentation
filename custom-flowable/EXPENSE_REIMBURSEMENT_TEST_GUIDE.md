# Expense Reimbursement Workflow Test Guide

This guide demonstrates a multi-level expense approval workflow with automatic routing based on expense amount.

## Workflow Overview

The ExpenseReimbursement workflow (`ExpenseReimbursement.bpmn20.xml`) implements a smart expense approval system that automatically routes expenses to the right approval level based on the amount.

### Visual Flow Diagram

```
                        [Employee Submits Expense]
                                   |
                                   v
                        ┌─────────────────────┐
                        │   MANAGER APPROVAL   │
                        │   (manager-queue)    │
                        │  All expenses start  │
                        │       here           │
                        └──────────┬──────────┘
                                   |
                    ┌──────────────┼──────────────┐
                    |              |              |
               [Rejected]    [Needs Info]    [Approved]
                    |              |              |
                    v              v              v
            [END: Rejected] [EMPLOYEE QUEUE]  Check Amount
                           (employee-queue)        |
                                 |                 |
                           [Resubmit to Manager]   |
                                                  |
                ┌─────────────────┬───────────────┴─────────────────┐
                |                 |                                 |
            ≤ $500          $501 - $2000                        > $2000
                |                 |                                 |
                |                 v                                 v
                |      ┌──────────────────┐              ┌──────────────────┐
                |      │ DIRECTOR APPROVAL│              │   VP APPROVAL    │
                |      │ (director-queue) │              │   (vp-queue)     │
                |      └────────┬─────────┘              └────────┬─────────┘
                |               |                                  |
                |        ┌──────┴──────┐                   ┌──────┴──────┐
                |   [Approved]    [Rejected]          [Approved]    [Rejected]
                |        |            |                    |            |
                |        |            v                    |            v
                |        |      [END: Rejected]            |      [END: Rejected]
                |        |                                 |
                v        v                                 v
        ┌───────┴────────┴─────────────────────────────────┴───────┐
        │                      FINANCE PROCESSING                   │
        │                      (finance-queue)                      │
        │                   Process reimbursement                   │
        └───────────────────────────────────────────────────────────┘
                                        |
                                        v
                                  [END: Paid]
```

### Rejection Handling

The workflow handles rejections at every level:

1. **Manager Rejection**: Immediate termination - expense is denied
2. **Director Rejection**: Immediate termination - expense is denied  
3. **VP Rejection**: Immediate termination - expense is denied
4. **Manager Clarification Request**: Special path where expense goes to employee queue for additional information, then returns to manager

Note: The Director also has an "Escalate to VP" option for medium expenses that might need executive review.

### How the Workflow Works

#### 1. **Four Queue System**
The workflow uses dedicated queues for each organizational level:
- **manager-queue**: First stop for ALL expense requests
- **director-queue**: Reviews medium-sized expenses ($501-$2000)
- **vp-queue**: Reviews large expenses (>$2000)
- **finance-queue**: Final processing for ALL approved expenses

#### 2. **Smart Amount-Based Routing**
The system automatically determines the approval path based on expense amount:

| Amount Range | Approval Path | Example |
|--------------|---------------|---------|
| ≤ $500 | Manager → Finance | Office supplies ($300) |
| $501 - $2000 | Manager → Director → Finance | Conference travel ($1500) |
| > $2000 | Manager → VP → Finance | Training program ($5000) |

#### 3. **Real User Assignments**
Instead of generic IDs, the system uses professional email addresses:
- Managers: `sarah.manager@company.com`, `michael.manager@company.com`
- Directors: `john.director@company.com`
- VPs: `jennifer.vp@company.com`
- Finance: `finance.processor@company.com`

This makes the workflow feel more realistic and professional in demos.

#### 4. **Three Distinct Paths Demonstrated**

**Path 1 - Quick Approval (Small Amount)**
```
Sarah Chen ($300) → Manager Approval → Finance Processing
```
- Fastest path
- Single approval needed
- Common for routine expenses

**Path 2 - Standard Approval (Medium Amount)**
```
Michael Johnson ($1500) → Manager → Director → Finance
```
- Two-level approval
- Ensures oversight for significant expenses
- Balances control with efficiency

**Path 3 - Executive Approval (Large Amount)**
```
Lisa Wang ($5000) → Manager → VP → Finance
```
- Highest level of scrutiny
- VP involvement for strategic expenses
- Complete audit trail

### Key Benefits

1. **Automated Routing**: No manual decisions about who should approve
2. **Clear Accountability**: Each level has specific responsibilities
3. **Scalable Design**: Easy to adjust thresholds or add new levels
4. **Queue Visibility**: Anyone can see what's pending in each queue
5. **Audit Trail**: Complete history of approvals and decisions

## Complete Test Scenarios

### Setup: Register and Deploy Workflow

#### 1. Register Workflow Metadata
```bash
curl -X POST http://localhost:8090/api/workflow-metadata/register \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "expenseReimbursement",
    "processName": "Expense Reimbursement Process",
    "description": "Multi-level expense approval based on amount",
    "candidateGroupMappings": {
      "managers": "manager-queue",
      "directors": "director-queue",
      "vps": "vp-queue",
      "finance": "finance-queue",
      "employees": "employee-queue"
    }
  }'
```

**Response:**
```json
{
  "id": 4,
  "processDefinitionKey": "expenseReimbursement",
  "candidateGroupMappings": {
    "managers": "manager-queue",
    "directors": "director-queue",
    "vps": "vp-queue",
    "finance": "finance-queue",
    "employees": "employee-queue"
  }
}
```

#### 2. Deploy BPMN Workflow
```bash
BPMN_CONTENT=$(cat definitions/ExpenseReimbursement.bpmn20.xml | jq -Rs .)
curl -X POST http://localhost:8090/api/workflow-metadata/deploy \
  -H "Content-Type: application/json" \
  -d "{
    \"processDefinitionKey\": \"expenseReimbursement\",
    \"bpmnXml\": $BPMN_CONTENT,
    \"deploymentName\": \"Expense Reimbursement v1.0\"
  }"
```

---

### Test 1: Small Expense ($300) - Direct to Finance

#### 1. Submit Expense
```bash
curl -X POST http://localhost:8090/api/process-instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "expenseReimbursement",
    "businessKey": "EXP-2025-001",
    "variables": {
      "employeeName": "Sarah Chen",
      "employeeEmail": "sarah.chen@company.com",
      "department": "Engineering",
      "expenseAmount": 300,
      "expenseCategory": "supplies",
      "expenseDescription": "Office supplies and equipment",
      "receiptAttached": true
    }
  }'
```

**Response:**
```json
{
  "processInstanceId": "10e43570-60af-11f0-9630-0242ac130003",
  "businessKey": "EXP-2025-001"
}
```

#### 2. Manager Claims and Approves
```bash
# Find task ID
TASK_ID=$(curl -s http://localhost:8090/api/tasks/queue/manager-queue | \
  jq -r '.[] | select(.businessKey == "EXP-2025-001") | .taskId')

# Claim task
curl -X POST "http://localhost:8090/api/tasks/$TASK_ID/claim?userId=sarah.manager@company.com"

# Approve expense
curl -X POST http://localhost:8090/api/tasks/$TASK_ID/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "sarah.manager@company.com",
    "variables": {
      "managerDecision": "approve",
      "managerComments": "Approved for office supplies"
    }
  }'
```

**Response:**
```json
{
  "completedBy": "sarah.manager@company.com",
  "nextTaskName": "Process reimbursement",
  "nextTaskQueue": "finance-queue"
}
```

**Result:** Small expense routed directly to finance queue after manager approval.

---

### Test 2: Medium Expense ($1500) - Needs Director

#### 1. Submit Expense
```bash
curl -X POST http://localhost:8090/api/process-instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "expenseReimbursement",
    "businessKey": "EXP-2025-002",
    "variables": {
      "employeeName": "Michael Johnson",
      "employeeEmail": "michael.johnson@company.com",
      "department": "Marketing",
      "expenseAmount": 1500,
      "expenseCategory": "travel",
      "expenseDescription": "Conference travel to Las Vegas",
      "receiptAttached": true
    }
  }'
```

#### 2. Manager Approves
```bash
TASK_ID=$(curl -s http://localhost:8090/api/tasks/queue/manager-queue | \
  jq -r '.[] | select(.businessKey == "EXP-2025-002") | .taskId')

curl -X POST "http://localhost:8090/api/tasks/$TASK_ID/claim?userId=michael.manager@company.com"

curl -X POST http://localhost:8090/api/tasks/$TASK_ID/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "michael.manager@company.com",
    "variables": {
      "managerDecision": "approve",
      "managerComments": "Conference travel approved"
    }
  }'
```

**Response:**
```json
{
  "nextTaskName": "Director approval required",
  "nextTaskQueue": "director-queue"
}
```

#### 3. Director Approves
```bash
curl -X POST "http://localhost:8090/api/tasks/420fa49b-60af-11f0-9630-0242ac130003/claim?userId=john.director@company.com"

curl -X POST http://localhost:8090/api/tasks/420fa49b-60af-11f0-9630-0242ac130003/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "john.director@company.com",
    "variables": {
      "directorDecision": "approve",
      "directorComments": "Travel expenses within budget"
    }
  }'
```

**Response:**
```json
{
  "nextTaskName": "Process reimbursement",
  "nextTaskQueue": "finance-queue"
}
```

**Result:** Medium expense required director approval before reaching finance.

---

### Test 3: Large Expense ($5000) - Needs VP

#### 1. Submit Expense
```bash
curl -X POST http://localhost:8090/api/process-instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "expenseReimbursement",
    "businessKey": "EXP-2025-003",
    "variables": {
      "employeeName": "Lisa Wang",
      "employeeEmail": "lisa.wang@company.com",
      "department": "Sales",
      "expenseAmount": 5000,
      "expenseCategory": "training",
      "expenseDescription": "Executive leadership training program",
      "receiptAttached": true
    }
  }'
```

#### 2. Manager Approves
```bash
TASK_ID=$(curl -s http://localhost:8090/api/tasks/queue/manager-queue | \
  jq -r '.[] | select(.businessKey == "EXP-2025-003") | .taskId')

curl -X POST "http://localhost:8090/api/tasks/$TASK_ID/claim?userId=lisa.manager@company.com"

curl -X POST http://localhost:8090/api/tasks/$TASK_ID/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "lisa.manager@company.com",
    "variables": {
      "managerDecision": "approve",
      "managerComments": "Leadership training approved"
    }
  }'
```

**Response:**
```json
{
  "nextTaskName": "VP approval required",
  "nextTaskQueue": "vp-queue"
}
```

#### 3. Check VP Queue
```bash
curl http://localhost:8090/api/tasks/queue/vp-queue
```

**Response:**
```json
[{
  "taskName": "VP approval required",
  "queueName": "vp-queue",
  "taskData": {
    "description": "High-value expense requires VP approval:\n        Employee: Lisa Wang\n        Amount: $5000\n        Category: training\n        Previous approvals: Manager, Director"
  }
}]
```

**Result:** Large expense routed to VP queue for high-level approval.

---

## Queue Status Summary

After running all tests:

### Finance Queue (2 approved expenses ready for processing)
```bash
curl http://localhost:8090/api/tasks/queue/finance-queue
```
- EXP-2025-001: Sarah Chen - $300 (Manager only)
- EXP-2025-002: Michael Johnson - $1500 (Manager + Director)

### VP Queue (1 pending approval)
```bash
curl http://localhost:8090/api/tasks/queue/vp-queue
```
- EXP-2025-003: Lisa Wang - $5000 (Awaiting VP approval)

## Key Features Demonstrated

1. **Multi-level Approval**: 4 different approval levels based on amount
2. **Automatic Routing**: Tasks route to correct queues based on business rules
3. **Queue Segregation**: Each role has its own dedicated queue
4. **User Assignment**: Proper email-based user IDs (e.g., john.director@company.com)
5. **Process Visibility**: Clear tracking of expense through approval chain

## Additional Workflow Features

- **Manager Rejection**: Ends process immediately
- **Clarification Request**: Routes to employee-queue for additional information
- **Director Escalation**: Can escalate to VP even for medium amounts
- **Finance Processing**: Captures payment method and reference numbers