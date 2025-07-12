# Purchase Order Approval - Test Scenarios

## Prerequisites

Ensure Docker containers are running:
```bash
cd /Users/srirajkadimisetty/projects/flowable-engine/custom-flowable/docker
docker-compose up -d
```

## Scenario 1: Low Value Order - Direct Approval

**Story**: John from Engineering needs 2 laptops for new developers ($3,500 total)

### 1.1 Register Workflow (One-time setup)

```bash
curl -X POST http://localhost:8090/api/workflow-metadata/register \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "purchaseOrderApproval",
    "processName": "Purchase Order Approval Process",
    "description": "Multi-level purchase order approval with amount-based routing",
    "candidateGroupMappings": {
      "managers": "manager-queue",
      "directors": "director-queue",
      "finance": "finance-queue",
      "procurement": "procurement-queue"
    }
  }'
```

### 1.2 Deploy Workflow (One-time setup)

```bash
# First, save the BPMN file content
BPMN_CONTENT=$(cat purchase-order-approval.bpmn20.xml | jq -Rs .)

curl -X POST http://localhost:8090/api/workflow-metadata/deploy \
  -H "Content-Type: application/json" \
  -d "{
    \"processDefinitionKey\": \"purchaseOrderApproval\",
    \"bpmnXml\": $BPMN_CONTENT,
    \"deploymentName\": \"Purchase Order Approval v1.0\"
  }"
```

### 1.3 Submit Purchase Order

```bash
curl -X POST http://localhost:8090/api/process-instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "purchaseOrderApproval",
    "businessKey": "PO-2024-001",
    "variables": {
      "orderId": "PO-2024-001",
      "requester": "john.doe@company.com",
      "department": "Engineering",
      "amount": 3500,
      "description": "2x Dell Latitude laptops for new developers",
      "urgency": "normal",
      "justification": "New hires starting next week"
    }
  }' | jq
```

Save the `processInstanceId` from the response.

### 1.4 Manager Reviews and Approves

Check manager queue:
```bash
curl http://localhost:8090/api/tasks/queue/manager-queue | jq
```

Get task details (replace {taskId} with actual ID):
```bash
curl http://localhost:8090/api/tasks/{taskId} | jq
```

Manager claims the task:
```bash
curl -X POST "http://localhost:8090/api/tasks/{taskId}/claim?userId=sarah.manager" | jq
```

Manager approves:
```bash
curl -X POST http://localhost:8090/api/tasks/{taskId}/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "sarah.manager",
    "variables": {
      "decision": "approve",
      "comments": "Approved - Essential equipment for new hires"
    }
  }' | jq
```

### 1.5 Verify Routing to Procurement

Since amount < $5000, task should go directly to procurement:
```bash
curl http://localhost:8090/api/tasks/queue/procurement-queue | jq
```

### 1.6 Procurement Processes Order

```bash
# Get the procurement task ID first
PROC_TASK_ID=$(curl -s http://localhost:8090/api/tasks/queue/procurement-queue | jq -r '.[0].taskId')

# Claim and complete
curl -X POST "http://localhost:8090/api/tasks/$PROC_TASK_ID/claim?userId=mike.procurement" | jq

curl -X POST http://localhost:8090/api/tasks/$PROC_TASK_ID/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "mike.procurement",
    "variables": {
      "vendorSelected": "Dell Direct",
      "expectedDeliveryDate": "2024-02-20",
      "purchaseOrderNumber": "PO-2024-001-DELL"
    }
  }' | jq
```

## Scenario 2: High Value Order - Finance Approval Required

**Story**: Marketing needs trade show materials and booth ($15,000)

### 2.1 Submit High Value Order

```bash
curl -X POST http://localhost:8090/api/process-instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "purchaseOrderApproval",
    "businessKey": "PO-2024-002",
    "variables": {
      "orderId": "PO-2024-002",
      "requester": "jane.smith@company.com",
      "department": "Marketing",
      "amount": 15000,
      "description": "Trade show booth, banners, and promotional materials",
      "urgency": "high",
      "justification": "Annual tech conference next month"
    }
  }' | jq
```

### 2.2 Manager Approval

```bash
# Get manager task
TASK_ID=$(curl -s http://localhost:8090/api/tasks/queue/manager-queue | jq -r '.[] | select(.businessKey=="PO-2024-002") | .taskId')

# Manager approves
curl -X POST http://localhost:8090/api/tasks/$TASK_ID/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "sarah.manager",
    "variables": {
      "decision": "approve",
      "comments": "Important marketing event, approved"
    }
  }' | jq
```

### 2.3 Finance Review (Amount ≥ $5000)

Check finance queue:
```bash
curl http://localhost:8090/api/tasks/queue/finance-queue | jq
```

Finance approves with budget code:
```bash
FINANCE_TASK=$(curl -s http://localhost:8090/api/tasks/queue/finance-queue | jq -r '.[0].taskId')

curl -X POST http://localhost:8090/api/tasks/$FINANCE_TASK/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "tom.finance",
    "variables": {
      "financeApproval": "approve",
      "budgetCode": "MKT-2024-EVENTS",
      "financeComments": "Within marketing events budget"
    }
  }' | jq
```

### 2.4 Procurement Processing

```bash
PROC_TASK=$(curl -s http://localhost:8090/api/tasks/queue/procurement-queue | jq -r '.[] | select(.businessKey=="PO-2024-002") | .taskId')

curl -X POST http://localhost:8090/api/tasks/$PROC_TASK/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "mike.procurement",
    "variables": {
      "vendorSelected": "ExpoDesign Co",
      "expectedDeliveryDate": "2024-03-01",
      "purchaseOrderNumber": "PO-2024-002-EXPO"
    }
  }' | jq
```

## Scenario 3: Escalation to Director

**Story**: Manager escalates a $50,000 server purchase to director

### 3.1 Submit Large Order

```bash
curl -X POST http://localhost:8090/api/process-instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "purchaseOrderApproval",
    "businessKey": "PO-2024-003",
    "variables": {
      "orderId": "PO-2024-003",
      "requester": "alex.sysadmin@company.com",
      "department": "IT",
      "amount": 50000,
      "description": "High-performance database server cluster",
      "urgency": "high",
      "justification": "Critical infrastructure upgrade"
    }
  }' | jq
```

### 3.2 Manager Escalates

```bash
TASK_ID=$(curl -s http://localhost:8090/api/tasks/queue/manager-queue | jq -r '.[] | select(.businessKey=="PO-2024-003") | .taskId')

curl -X POST http://localhost:8090/api/tasks/$TASK_ID/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "sarah.manager",
    "variables": {
      "decision": "escalate",
      "comments": "Large infrastructure purchase, needs director approval"
    }
  }' | jq
```

### 3.3 Director Approves

Check director queue:
```bash
curl http://localhost:8090/api/tasks/queue/director-queue | jq
```

Director approval:
```bash
DIR_TASK=$(curl -s http://localhost:8090/api/tasks/queue/director-queue | jq -r '.[0].taskId')

curl -X POST http://localhost:8090/api/tasks/$DIR_TASK/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "robert.director",
    "variables": {
      "directorDecision": "approve",
      "directorComments": "Critical infrastructure, approved"
    }
  }' | jq
```

### 3.4 Continue with Finance and Procurement

The process continues to finance (amount ≥ $5000) and then procurement.

## Scenario 4: Rejection Flow

**Story**: Finance rejects an over-budget request

### 4.1 Submit Order

```bash
curl -X POST http://localhost:8090/api/process-instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "purchaseOrderApproval",
    "businessKey": "PO-2024-004",
    "variables": {
      "orderId": "PO-2024-004",
      "requester": "bob.sales@company.com",
      "department": "Sales",
      "amount": 8000,
      "description": "Luxury office furniture",
      "urgency": "low"
    }
  }' | jq
```

### 4.2 Manager Approves

```bash
TASK_ID=$(curl -s http://localhost:8090/api/tasks/queue/manager-queue | jq -r '.[] | select(.businessKey=="PO-2024-004") | .taskId')

curl -X POST http://localhost:8090/api/tasks/$TASK_ID/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "sarah.manager",
    "variables": {
      "decision": "approve",
      "comments": "Seems reasonable"
    }
  }' | jq
```

### 4.3 Finance Rejects

```bash
FIN_TASK=$(curl -s http://localhost:8090/api/tasks/queue/finance-queue | jq -r '.[] | select(.businessKey=="PO-2024-004") | .taskId')

curl -X POST http://localhost:8090/api/tasks/$FIN_TASK/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "tom.finance",
    "variables": {
      "financeApproval": "reject",
      "budgetCode": "N/A",
      "financeComments": "Over budget for Q1, please resubmit next quarter"
    }
  }' | jq
```

## Scenario 5: Using GetNext for Operator Workflow

**Story**: An operator wants to work through tasks efficiently without browsing

### 5.1 Get Next Task from Queue

```bash
# Get the next available task from manager queue
curl http://localhost:8090/api/tasks/queue/manager-queue/next | jq
```

If a task is available, you'll get the task details. If no tasks are available, you'll get a 204 No Content response.

### 5.2 Claim and Complete Workflow

```bash
# 1. Get next task
NEXT_TASK=$(curl -s http://localhost:8090/api/tasks/queue/manager-queue/next)

if [ -z "$NEXT_TASK" ]; then
  echo "No tasks available in queue"
else
  TASK_ID=$(echo $NEXT_TASK | jq -r .taskId)
  echo "Working on task: $TASK_ID"
  
  # 2. Claim the task
  curl -X POST "http://localhost:8090/api/tasks/$TASK_ID/claim?userId=sarah.manager"
  
  # 3. Complete the task
  curl -X POST http://localhost:8090/api/tasks/$TASK_ID/complete \
    -H "Content-Type: application/json" \
    -d '{
      "userId": "sarah.manager",
      "variables": {
        "decision": "approve",
        "comments": "Approved via operator workflow"
      }
    }'
  
  # 4. Get next task (loop back to step 1)
  curl http://localhost:8090/api/tasks/queue/manager-queue/next
fi
```

## Useful Queries

### Check All Queues
```bash
for queue in manager-queue director-queue finance-queue procurement-queue; do
  echo "=== $queue ==="
  curl -s http://localhost:8090/api/tasks/queue/$queue | jq length
done
```

### Get User's Tasks
```bash
curl "http://localhost:8090/api/tasks/my-tasks?userId=sarah.manager" | jq
```

### Check Process Status
```bash
curl http://localhost:8090/api/process-instances/{processInstanceId} | jq
```

### Database Verification
```bash
# Check queue tasks
docker exec flowable-wrapper-postgres psql -U flowable -d flowable_wrapper \
  -c "SELECT task_id, task_name, queue_name, status, assignee FROM queue_tasks ORDER BY created_at DESC LIMIT 10;"

# Check process count by status
docker exec flowable-wrapper-postgres psql -U flowable -d flowable_wrapper \
  -c "SELECT queue_name, status, COUNT(*) FROM queue_tasks GROUP BY queue_name, status;"
```

## Expected Outcomes

1. **Low value orders** (<$5000) skip finance review
2. **High value orders** (≥$5000) require finance approval
3. **Escalated orders** go to director queue before continuing
4. **Rejected orders** end immediately with notification
5. All tasks are automatically routed to correct queues based on candidate groups