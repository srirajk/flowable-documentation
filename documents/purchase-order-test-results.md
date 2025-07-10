# Purchase Order Workflow Test Results

## Business Workflow Description

The purchase order approval workflow represents a typical corporate procurement process with the following business rules:

### Process Flow:
1. **Employee Submits Request**: Any employee can submit a purchase request with item details, amount, vendor, and business justification

2. **Manager Review**: The request goes to the managers group for approval with three possible decisions:
   - **Approve**: Move forward with the purchase
   - **Reject**: Deny the request (ends process)
   - **Need More Info**: Send back to employee for additional details (loops back to manager after info provided)

3. **Amount-Based Routing**: After manager approval, the system automatically routes based on purchase amount:
   - **≤ $5,000**: Goes directly to procurement team
   - **> $5,000**: Requires additional finance team approval

4. **Finance Review** (for amounts > $5,000): Finance team can:
   - **Approve**: Assign budget code and forward to procurement
   - **Reject**: Deny the request with reason (ends process)

5. **Procurement Processing**: Procurement team creates the purchase order, assigns PO number and expected delivery date

6. **Notifications**: 
   - **Success**: Requester receives order confirmation with PO details
   - **Rejection**: Requester receives rejection notice with reasons from manager/finance

### Key Business Rules:
- Manager can request additional information multiple times (loop)
- Finance approval only required for high-value purchases
- Rejected requests notify the original requester
- All tasks are assigned to groups except notifications (assigned to requester)

## Overview
This document captures all API calls and responses from testing the purchase order approval workflow, demonstrating complex routing including conditional paths, loops, and multi-level approvals.

## Test Setup

### 1. Start Flowable
```bash
./rest-postgres.sh start
```

### 2. Deploy Purchase Order Workflow
```bash
curl -X POST http://localhost:8080/flowable-rest/service/repository/deployments \
  -u rest-admin:test \
  -F "deployment=@/Users/srirajkadimisetty/projects/flowable-engine/purchase-order-improved.bpmn"
```

**Response:**
```json
{
  "id": "4e159675-5d11-11f0-8602-0242ac160003",
  "name": "purchase-order-improved",
  "deploymentTime": "2025-07-09T22:08:55.435Z",
  "category": null,
  "parentDeploymentId": "4e159675-5d11-11f0-8602-0242ac160003",
  "url": "http://localhost:8080/flowable-rest/service/repository/deployments/4e159675-5d11-11f0-8602-0242ac160003",
  "tenantId": ""
}
```

## Test Scenario 1: Manager Needs More Info Loop

### Start Process Instance
```bash
curl -X POST http://localhost:8080/flowable-rest/service/runtime/process-instances \
  -u rest-admin:test \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "purchaseOrderApproval",
    "variables": [
      {"name": "itemDescription", "value": "New Laptop for Development"},
      {"name": "amount", "value": 3500},
      {"name": "vendor", "value": "TechSupplies Inc"},
      {"name": "justification", "value": "Current laptop is 5 years old and failing"},
      {"name": "requester", "value": "john.doe"}
    ]
  }'
```

**Response:**
```json
{
  "id": "526ede79-5d11-11f0-8602-0242ac160003",
  "url": "http://localhost:8080/flowable-rest/service/runtime/process-instances/526ede79-5d11-11f0-8602-0242ac160003",
  "processDefinitionName": "Purchase Order Approval Process",
  "startTime": "2025-07-09T22:09:02.731Z",
  "variables": [
    {"name": "requester", "type": "string", "value": "john.doe"},
    {"name": "amount", "type": "integer", "value": 3500},
    {"name": "vendor", "type": "string", "value": "TechSupplies Inc"},
    {"name": "itemDescription", "type": "string", "value": "New Laptop for Development"},
    {"name": "justification", "type": "string", "value": "Current laptop is 5 years old and failing"}
  ]
}
```

### Check Active Tasks - Manager Review
```bash
curl -X GET "http://localhost:8080/flowable-rest/service/runtime/tasks?processInstanceId=526ede79-5d11-11f0-8602-0242ac160003" \
  -u rest-admin:test
```

**Response:**
```json
{
  "data": [{
    "name": "Manager Review",
    "assignee": null,
    "candidateGroups": "managers",
    "description": "Review purchase request from john.doe:\n- Item: New Laptop for Development\n- Amount: $3500\n- Vendor: TechSupplies Inc\n- Justification: Current laptop is 5 years old and failing"
  }]
}
```

### Manager Requests More Info
```bash
curl -X POST "http://localhost:8080/flowable-rest/service/runtime/tasks/52708c39-5d11-11f0-8602-0242ac160003" \
  -u rest-admin:test \
  -H "Content-Type: application/json" \
  -d '{
    "action": "complete",
    "variables": [
      {"name": "managerApproved", "value": "needsInfo"},
      {"name": "managerComments", "value": "Please provide cost comparison with other vendors"}
    ]
  }'
```

### Check Active Tasks - Provide Info Task
```bash
curl -X GET "http://localhost:8080/flowable-rest/service/runtime/tasks?processInstanceId=526ede79-5d11-11f0-8602-0242ac160003" \
  -u rest-admin:test
```

**Response:**
```json
{
  "data": [{
    "name": "Provide Additional Information",
    "assignee": "john.doe",
    "description": "Manager requested more information:\nPlease provide cost comparison with other vendors"
  }]
}
```

### Requester Provides Info
```bash
curl -X POST "http://localhost:8080/flowable-rest/service/runtime/tasks/{taskId}" \
  -u rest-admin:test \
  -H "Content-Type: application/json" \
  -d '{
    "action": "complete",
    "variables": [
      {"name": "additionalInfo", "value": "Vendor comparison: TechSupplies $3500, CompuStore $3800, BestTech $4200"},
      {"name": "updatedAmount", "value": null}
    ]
  }'
```

### Check Tasks - Back to Manager Review
```bash
curl -X GET "http://localhost:8080/flowable-rest/service/runtime/tasks?processInstanceId=526ede79-5d11-11f0-8602-0242ac160003" \
  -u rest-admin:test
```

**Response:**
```json
{
  "data": [{
    "name": "Manager Review",
    "assignee": null
  }]
}
```

### Manager Approves
```bash
curl -X POST "http://localhost:8080/flowable-rest/service/runtime/tasks/{taskId}" \
  -u rest-admin:test \
  -H "Content-Type: application/json" \
  -d '{
    "action": "complete",
    "variables": [
      {"name": "managerApproved", "value": "approved"},
      {"name": "managerComments", "value": "Good comparison provided. Approved for purchase."}
    ]
  }'
```

### Check Tasks - Procurement (Amount ≤ $5000)
```bash
curl -X GET "http://localhost:8080/flowable-rest/service/runtime/tasks?processInstanceId=526ede79-5d11-11f0-8602-0242ac160003" \
  -u rest-admin:test
```

**Response:**
```json
{
  "data": [{
    "name": "Process Purchase Order",
    "candidateGroups": "procurement",
    "description": "Create purchase order:\n- Item: ${itemDescription}\n- Amount: $${amount}\n- Vendor: ${vendor}\n- Budget Code: ${budgetCode}"
  }]
}
```

## Test Scenario 2: High Amount Finance Approval and Rejection

### Start High-Value Process
```bash
curl -X POST http://localhost:8080/flowable-rest/service/runtime/process-instances \
  -u rest-admin:test \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "purchaseOrderApproval",
    "variables": [
      {"name": "itemDescription", "value": "Server Hardware Upgrade"},
      {"name": "amount", "value": 15000},
      {"name": "vendor", "value": "ServerPro Systems"},
      {"name": "justification", "value": "Critical infrastructure upgrade for increased capacity"},
      {"name": "requester", "value": "jane.smith"}
    ]
  }'
```

**Response:**
```json
{
  "id": "1af9ad0b-5d12-11f0-8602-0242ac160003",
  "processDefinitionName": "Purchase Order Approval Process",
  "variables": [
    {"name": "requester", "type": "string", "value": "jane.smith"},
    {"name": "amount", "type": "integer", "value": 15000},
    {"name": "vendor", "type": "string", "value": "ServerPro Systems"},
    {"name": "itemDescription", "type": "string", "value": "Server Hardware Upgrade"},
    {"name": "justification", "type": "string", "value": "Critical infrastructure upgrade for increased capacity"}
  ]
}
```

### Manager Approves High-Value Request
```bash
curl -X POST "http://localhost:8080/flowable-rest/service/runtime/tasks/{taskId}" \
  -u rest-admin:test \
  -H "Content-Type: application/json" \
  -d '{
    "action": "complete",
    "variables": [
      {"name": "managerApproved", "value": "approved"},
      {"name": "managerComments", "value": "Critical upgrade needed. Approved."}
    ]
  }'
```

### Check Tasks - Finance Approval Required (Amount > $5000)
```bash
curl -X GET "http://localhost:8080/flowable-rest/service/runtime/tasks?processInstanceId=1af9ad0b-5d12-11f0-8602-0242ac160003" \
  -u rest-admin:test
```

**Response:**
```json
{
  "data": [{
    "name": "Finance Approval",
    "candidateGroups": "finance",
    "description": "High-value purchase requires finance approval:\n- Item: Server Hardware Upgrade\n- Amount: $15000\n- Vendor: ServerPro Systems\n- Manager: Approved"
  }]
}
```

### Finance Rejects
```bash
curl -X POST "http://localhost:8080/flowable-rest/service/runtime/tasks/{taskId}" \
  -u rest-admin:test \
  -H "Content-Type: application/json" \
  -d '{
    "action": "complete",
    "variables": [
      {"name": "financeApproved", "value": false},
      {"name": "budgetCode", "value": "N/A"},
      {"name": "financeNotes", "value": "Budget constraints - please defer to next quarter"}
    ]
  }'
```

### Check Tasks - Rejection Notification
```bash
curl -X GET "http://localhost:8080/flowable-rest/service/runtime/tasks?processInstanceId=1af9ad0b-5d12-11f0-8602-0242ac160003" \
  -u rest-admin:test
```

**Response:**
```json
{
  "data": [{
    "name": "Request Rejected",
    "assignee": "jane.smith",
    "description": "REJECTED: Your purchase request was not approved.\n- Manager comments: Critical upgrade needed. Approved.\n- Finance notes: Budget constraints - please defer to next quarter"
  }]
}
```

## Key Observations

### 1. Automatic State Management
- Flowable automatically tracks workflow state
- No manual state management needed in wrapper API
- Process instances maintain their position in the workflow

### 2. Task Routing
- Tasks automatically appear based on decisions
- Conditional routing works based on variables (amount, approval status)
- Loop-backs work correctly (manager → requester → manager)

### 3. Task Assignment
- Group assignment: `candidateGroups` (managers, finance, procurement)
- Individual assignment: `assignee` (requester gets their own tasks)
- Both patterns work seamlessly

### 4. Variable Handling
- Variables persist throughout the process
- Can be read in task descriptions using ${variableName}
- Can be updated when completing tasks

### 5. Multi-Level Approvals
- Workflow correctly handles multiple approval levels
- Different paths based on business rules (amount thresholds)
- Rejection can happen at any approval level

## Implications for Wrapper API Design

1. **No Event Loops Needed**: Flowable handles all state management internally
2. **Stateless Wrapper**: The wrapper can be completely stateless
3. **Simple Query Pattern**: Just query active tasks by processInstanceId
4. **Direct Task Completion**: Complete tasks and Flowable handles the rest
5. **Automatic Next Task**: After completing a task, query again to get the next active task(s)