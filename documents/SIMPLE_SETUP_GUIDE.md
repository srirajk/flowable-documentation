# Simple Setup Guide

## Quick Start (5 Minutes)

### Step 1: Start the System
```bash
# Go to the project folder
cd custom-flowable

# Start everything
docker-compose up -d

# Wait 2 minutes for everything to start
```

### Step 2: Check It's Working
```bash
# Check if application is running
curl http://localhost:8090/actuator/health

# Should return: {"status":"UP"}
```

### Step 3: Test Basic Function
```bash
# Get list of workflows
curl http://localhost:8090/api/Sanctions-Management/workflow-metadata \
  -H "X-User-Id: operation-user-1"
```

## What Gets Started

### 🐳 **Docker Containers**:
- **Application**: Main workflow system (port 8090)
- **Database**: PostgreSQL with all data (port 5430)
- **Security**: Cerbos authorization (port 3592)

### 📊 **Database**:
- **Users**: Test users for different roles
- **Business Apps**: Sample applications (Sanctions, Expenses, etc.)
- **Workflows**: Pre-configured workflow templates

### 🔐 **Security**:
- **Roles**: Manager, HR, Finance, etc.
- **Policies**: Four-Eyes rules and access controls
- **Test Users**: Ready to use immediately

## Test Users (Already Created)

### US Region Users:
```
Manager: us-l1-supervisor-1
Operator: us-l1-operator-1, us-l1-operator-2
```

### EU Region Users:
```
Manager: eu-l1-supervisor-1  
Operator: eu-l1-operator-1, eu-l1-operator-2
```

### Admin Users:
```
Admin: operation-user-1 (can do everything)
Automation: automation-user-2 (for system operations)
```

## Simple Test Workflow

### 1. Register a New Workflow
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/register" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: automation-user-2" \
  -d '{
    "processDefinitionKey": "simpleApproval",
    "processName": "Simple Approval Process",
    "businessAppName": "Sanctions-Management",
    "description": "Simple two-step approval",
    "candidateGroupMappings": {
      "level1-approver": "level1-queue"
    }
  }'
```

### 2. Start a Process
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/process-instances/start" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: automation-user-2" \
  -d '{
    "processDefinitionKey": "simpleApproval",
    "businessKey": "TEST001",
    "variables": {
      "requestor": "John Doe",
      "amount": 1000,
      "description": "Test approval request"
    }
  }'
```

### 3. Check Queue for Tasks
```bash
curl -X GET "http://localhost:8090/api/Sanctions-Management/tasks/queue/level1-queue" \
  -H "X-User-Id: us-l1-operator-1"
```

### 4. Claim and Complete Task
```bash
# First, claim the task (use task ID from step 3)
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{TASK_ID}/claim" \
  -H "X-User-Id: us-l1-operator-1" \
  -d '{}'

# Then complete it
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{TASK_ID}/complete" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: us-l1-operator-1" \
  -d '{
    "action": "APPROVE",
    "comment": "Looks good to me"
  }'
```

## Troubleshooting

### Problem: "Connection refused"
```bash
# Check if containers are running
docker-compose ps

# If not running, start them
docker-compose up -d
```

### Problem: "Database not ready"
```bash
# Check database logs
docker-compose logs postgres

# Wait longer (database takes time to start)
sleep 30
```

### Problem: "Authorization failed"
```bash
# Check if user exists in database
docker exec -it custom-flowable-postgres-1 psql -U flowable_user -d flowable_db -c "SELECT id FROM users WHERE id = 'us-l1-operator-1';"

# Should return the user ID
```

### Problem: "Workflow not found"
```bash
# Check if workflow is registered
curl "http://localhost:8090/api/Sanctions-Management/workflow-metadata" \
  -H "X-User-Id: operation-user-1"
```

## Common Commands

### Restart Everything:
```bash
docker-compose down
docker-compose up -d
```

### View Logs:
```bash
# Application logs
docker-compose logs app

# Database logs
docker-compose logs postgres

# All logs
docker-compose logs
```

### Clean Start:
```bash
# Stop and remove everything
docker-compose down -v

# Rebuild and start fresh
docker-compose up -d --build
```

## API Endpoints (Quick Reference)

### Workflows:
- `GET /api/{businessApp}/workflow-metadata` - List workflows
- `POST /api/{businessApp}/workflow-metadata/register` - Register new workflow
- `POST /api/{businessApp}/workflow-metadata/deploy-from-file` - Deploy workflow

### Process Instances:
- `POST /api/{businessApp}/process-instances/start` - Start new process
- `GET /api/{businessApp}/process-instances/{id}` - Get process details

### Tasks:
- `GET /api/{businessApp}/tasks/queue/{queueName}` - Get tasks in queue
- `POST /api/{businessApp}/tasks/{taskId}/claim` - Claim task
- `POST /api/{businessApp}/tasks/{taskId}/complete` - Complete task
- `GET /api/{businessApp}/tasks/my-tasks` - Get my assigned tasks

### Remember:
- Always include `X-User-Id` header
- Use correct business app name in URL
- Replace `{taskId}`, `{businessApp}`, etc. with actual values

## File Locations

### Important Files:
```
custom-flowable/
├── docker-compose.yml          # Main startup file
├── definitions/               # BPMN workflow files
├── docker/cerbos/policies/   # Security policies
└── flowable-wrapper-v2/      # Application code
```

### BPMN Files:
- `definitions/SanctionsL1L2Flow.bpmn20.xml` - Sanctions workflow
- `definitions/ExpenseReimbursement.bpmn20.xml` - Expense workflow

### Database Scripts:
- `src/main/resources/db/schema.sql` - Database structure
- `src/main/resources/db/data-new.sql` - Test data

That's it! You should now have a working system ready for testing.