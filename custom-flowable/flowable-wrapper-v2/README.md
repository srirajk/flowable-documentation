# Flowable Wrapper API V2

A Spring Boot-based wrapper API for Flowable BPM engine that provides queue-based task distribution and RESTful endpoints for workflow management.

## Overview

This project wraps the Flowable BPM engine to provide:
- Queue-based task distribution system
- RESTful API for process and task management
- Automatic task routing based on candidate groups
- PostgreSQL persistence with JSONB support
- Docker containerization for easy deployment

## Architecture

### Key Components

1. **Embedded Flowable Engine**: Runs within the Spring Boot application
2. **Queue System**: Tasks are automatically distributed to queues based on candidate group mappings
3. **PostgreSQL Database**: Stores workflow metadata and queue tasks with JSONB support
4. **RESTful API**: Provides endpoints for workflow deployment, process management, and task operations

### Database Schema

- `workflow_metadata`: Stores workflow definitions and queue mappings
- `queue_tasks`: Stores task queue assignments and status

## Prerequisites

- Docker and Docker Compose
- Java 21 (for local development)
- Maven 3.9+ (for local development)

## Quick Start

1. Clone the repository
2. Navigate to the docker directory:
   ```bash
   cd /Users/srirajkadimisetty/projects/flowable-engine/custom-flowable/docker
   ```

3. Start the services:
   ```bash
   docker-compose up -d
   ```

4. Verify the services are running:
   ```bash
   curl http://localhost:8090/actuator/health
   ```

## Test Scenario: Purchase Order Approval Workflow

We'll demonstrate the API functionality using a realistic Purchase Order Approval workflow that includes:

### Workflow Description

The Purchase Order Approval process (`purchase-order-approval.bpmn20.xml`) implements a multi-level approval system:

1. **Manager Review**: All purchase orders start with manager review
   - Can approve, reject, or escalate to director
   
2. **Director Review**: For escalated orders
   - Can approve or reject
   
3. **Amount-based Routing**: Approved orders are routed based on amount
   - Orders < $5,000: Direct to procurement
   - Orders ≥ $5,000: Require finance approval
   
4. **Finance Review**: For high-value orders
   - Validates budget compliance
   - Can approve or reject
   
5. **Procurement Processing**: Final step for all approved orders
   - Assigns vendor and PO number

### Queue Mappings

- `managers` → `manager-queue`
- `directors` → `director-queue`
- `finance` → `finance-queue`
- `procurement` → `procurement-queue`

## API Testing Guide

### Step 1: Register Workflow Metadata

First, register the workflow with queue mappings:

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
    },
    "metadata": {
      "category": "procurement",
      "sla": "48 hours"
    }
  }'
```

Expected Response:
```json
{
  "id": 1,
  "processDefinitionKey": "purchaseOrderApproval",
  "processName": "Purchase Order Approval Process",
  "candidateGroupMappings": {
    "managers": "manager-queue",
    "directors": "director-queue",
    "finance": "finance-queue",
    "procurement": "procurement-queue"
  },
  "deployed": false
}
```

### Step 2: Deploy the BPMN Workflow

Deploy the BPMN file to Flowable engine:

```bash
# Read the BPMN file content
BPMN_CONTENT=$(cat purchase-order-approval.bpmn20.xml | jq -Rs .)

# Deploy the workflow
curl -X POST http://localhost:8090/api/workflow-metadata/deploy \
  -H "Content-Type: application/json" \
  -d "{
    \"processDefinitionKey\": \"purchaseOrderApproval\",
    \"bpmnXml\": $BPMN_CONTENT,
    \"deploymentName\": \"Purchase Order Approval v1.0\"
  }"
```

The response will include `taskQueueMappings` showing how each task is mapped to its queue.

### Step 3: Start a Process Instance

Let's create two purchase orders - one low value and one high value:

#### Low Value Order (< $5,000)
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
      "description": "Development laptops",
      "urgency": "normal"
    }
  }'
```

#### High Value Order (≥ $5,000)
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
      "description": "Trade show booth and materials",
      "urgency": "high"
    }
  }'
```

### Step 4: Manager Queue Operations

Check tasks in the manager queue:

```bash
curl http://localhost:8090/api/tasks/queue/manager-queue
```

Get detailed task information:

```bash
curl http://localhost:8090/api/tasks/{taskId}
```

Claim a task as a manager:

```bash
curl -X POST "http://localhost:8090/api/tasks/{taskId}/claim?userId=manager1"
```

Complete the task with approval:

```bash
curl -X POST http://localhost:8090/api/tasks/{taskId}/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "manager1",
    "variables": {
      "decision": "approve",
      "comments": "Approved - within budget and justified"
    }
  }'
```

### Step 5: Check Queue Routing

After manager approval:
- Low value order → Goes directly to `procurement-queue`
- High value order → Goes to `finance-queue`

Check finance queue:
```bash
curl http://localhost:8090/api/tasks/queue/finance-queue
```

Check procurement queue:
```bash
curl http://localhost:8090/api/tasks/queue/procurement-queue
```

### Step 6: Complete Finance Review (for high value orders)

```bash
curl -X POST http://localhost:8090/api/tasks/{financeTaskId}/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "finance.user",
    "variables": {
      "financeApproval": "approve",
      "budgetCode": "MKT-2024-Q1",
      "financeComments": "Budget available, approved"
    }
  }'
```

### Step 7: Process Order in Procurement

Both orders eventually reach procurement:

```bash
curl -X POST http://localhost:8090/api/tasks/{procurementTaskId}/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "procurement.user",
    "variables": {
      "vendorSelected": "TechSupplies Inc",
      "expectedDeliveryDate": "2024-02-15",
      "purchaseOrderNumber": "PO-2024-001-FINAL"
    }
  }'
```

### Step 8: Test Escalation Path

Start a new process and escalate to director:

```bash
# Complete manager task with escalation
curl -X POST http://localhost:8090/api/tasks/{taskId}/complete \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "manager1",
    "variables": {
      "decision": "escalate",
      "comments": "Outside my approval limit, escalating to director"
    }
  }'
```

Check director queue:
```bash
curl http://localhost:8090/api/tasks/queue/director-queue
```

## API Endpoints

### Workflow Metadata
- `POST /api/workflow-metadata/register` - Register workflow with queue mappings
- `POST /api/workflow-metadata/deploy` - Deploy BPMN to engine
- `GET /api/workflow-metadata/{processDefinitionKey}` - Get workflow metadata

### Process Management
- `POST /api/process-instances/start` - Start new process instance
- `GET /api/process-instances/{processInstanceId}` - Get process instance details

### Task Management
- `GET /api/tasks/queue/{queueName}` - Get tasks by queue
- `GET /api/tasks/queue/{queueName}/next` - Get next available task from queue
- `GET /api/tasks/{taskId}` - Get task details
- `POST /api/tasks/{taskId}/claim?userId={userId}` - Claim task
- `POST /api/tasks/{taskId}/unclaim` - Unclaim task
- `POST /api/tasks/{taskId}/complete` - Complete task
- `GET /api/tasks/my-tasks?userId={userId}` - Get user's tasks

### Health & Monitoring
- `GET /actuator/health` - Application health check
- `GET /actuator/info` - Application info

## Configuration

Environment variables can be set in `docker-compose.yml`:

```yaml
environment:
  - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/flowable_wrapper
  - SPRING_DATASOURCE_USERNAME=flowable
  - SPRING_DATASOURCE_PASSWORD=flowable
  - SERVER_PORT=8090
```

## Troubleshooting

### Check Application Logs
```bash
docker logs flowable-wrapper-v2
```

### Check Database
```bash
docker exec flowable-wrapper-postgres psql -U flowable -d flowable_wrapper
```

### Common Issues

1. **Tasks not appearing in queues**
   - Verify workflow metadata is registered
   - Check that BPMN deployment was successful
   - Ensure candidate groups in BPMN match registered mappings

2. **Port conflicts**
   - Default port is 8090
   - Keycloak runs on 8080
   - PostgreSQL on 5432

3. **Database connection issues**
   - Ensure PostgreSQL container is running
   - Check network connectivity between containers

## Development

### Local Development Setup

1. Install dependencies:
   ```bash
   cd flowable-wrapper-v2
   mvn clean install
   ```

2. Run locally:
   ```bash
   mvn spring-boot:run
   ```

### Building Docker Image

```bash
docker-compose build flowable-wrapper-v2
```

### Running Tests

```bash
mvn test
```

## License

This project is licensed under the MIT License.