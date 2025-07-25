# Simple API Guide

## What are APIs?

**APIs are like buttons on a website, but for computers.**

Instead of clicking buttons, you send messages to tell the system what to do.

## Basic Format

All API calls look like this:
```bash
curl -X [METHOD] "[URL]" \
  -H "X-User-Id: [YOUR_USER_ID]" \
  -H "Content-Type: application/json" \
  -d '[DATA]'
```

### Parts Explained:
- **METHOD**: What you want to do (GET=read, POST=create/do)
- **URL**: Where to send the request
- **X-User-Id**: Who you are (always required)
- **DATA**: Information to send (only for POST)

## Common Operations

### 1. **See Your Tasks**
```bash
curl -X GET "http://localhost:8090/api/Sanctions-Management/tasks/queue/level1-queue" \
  -H "X-User-Id: us-l1-operator-1"
```
**What it does**: Shows all tasks in the level1-queue that you can work on

### 2. **Claim a Task**
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/TASK_ID_HERE/claim" \
  -H "X-User-Id: us-l1-operator-1" \
  -d '{}'
```
**What it does**: Assigns the task to you so you can work on it

### 3. **Complete a Task**
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/TASK_ID_HERE/complete" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: us-l1-operator-1" \
  -d '{
    "action": "APPROVE",
    "comment": "Everything looks good"
  }'
```
**What it does**: Finishes the task and moves it to the next step

### 4. **Start New Process**
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/process-instances/start" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: automation-user-2" \
  -d '{
    "processDefinitionKey": "sanctionsCaseManagement",
    "businessKey": "CASE001",
    "variables": {
      "customerName": "John Doe",
      "amount": 50000,
      "riskLevel": "HIGH"
    }
  }'
```
**What it does**: Creates a new workflow instance (like submitting a new request)

## Step-by-Step Example

Let's walk through approving a vacation request:

### Step 1: Check Your Queue
```bash
curl -X GET "http://localhost:8090/api/Sanctions-Management/tasks/queue/manager-queue" \
  -H "X-User-Id: manager-alice"
```

**Response:**
```json
[
  {
    "taskId": "task-123",
    "taskName": "Approve Vacation Request", 
    "assignee": null,
    "status": "OPEN",
    "processVariables": {
      "employeeName": "Bob Smith",
      "vacationDays": 5,
      "startDate": "2024-03-15"
    }
  }
]
```

### Step 2: Claim the Task
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/task-123/claim" \
  -H "X-User-Id: manager-alice" \
  -d '{}'
```

**Response:**
```json
{
  "taskId": "task-123",
  "assignee": "manager-alice",
  "status": "CLAIMED"
}
```

### Step 3: Approve the Task
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/task-123/complete" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: manager-alice" \
  -d '{
    "action": "APPROVE",
    "comment": "Vacation approved. Enjoy your time off!"
  }'
```

**Response:**
```json
{
  "taskId": "task-123",
  "completedBy": "manager-alice",
  "completedAt": "2024-03-01T10:30:00Z",
  "nextTasks": ["hr-processing-task"]
}
```

## All API Endpoints (Simple List)

### **Tasks** (Main operations):
| What You Want | Method | URL |
|---------------|--------|-----|
| See my tasks | GET | `/api/{app}/tasks/my-tasks` |
| See queue tasks | GET | `/api/{app}/tasks/queue/{queue}` |
| Claim task | POST | `/api/{app}/tasks/{taskId}/claim` |
| Complete task | POST | `/api/{app}/tasks/{taskId}/complete` |
| Unclaim task | POST | `/api/{app}/tasks/{taskId}/unclaim` |
| Get task details | GET | `/api/{app}/tasks/{taskId}` |

### **Processes** (Start new workflows):
| What You Want | Method | URL |
|---------------|--------|-----|
| Start process | POST | `/api/{app}/process-instances/start` |
| Get process info | GET | `/api/{app}/process-instances/{id}` |

### **Workflows** (Admin operations):
| What You Want | Method | URL |
|---------------|--------|-----|
| List workflows | GET | `/api/{app}/workflow-metadata` |
| Register workflow | POST | `/api/{app}/workflow-metadata/register` |
| Deploy workflow | POST | `/api/{app}/workflow-metadata/deploy-from-file` |

## Common Errors and Solutions

### Error: "403 Forbidden"
**Problem**: You don't have permission
**Solution**: Check your role and queue access

### Error: "404 Not Found"  
**Problem**: Task or process doesn't exist
**Solution**: Check the ID is correct

### Error: "400 Bad Request"
**Problem**: Invalid data sent
**Solution**: Check your JSON format

### Error: "500 Internal Server Error"
**Problem**: Something wrong with the system
**Solution**: Check logs or contact admin

## Tips for Success

### 1. **Always Include User ID**
Every request needs `X-User-Id` header with your username.

### 2. **Replace Placeholders**
- `{app}` → Your business app (like "Sanctions-Management")
- `{taskId}` → Actual task ID from previous calls
- `{queue}` → Queue name (like "level1-queue")

### 3. **Check Responses**
Look at what comes back to get IDs for next calls.

### 4. **Use Correct User**
Make sure the user ID matches someone who has access.

### 5. **Test Step by Step**
Start with simple GET requests before trying complex operations.

## Sample Users for Testing

### Managers:
- `us-l1-supervisor-1` (US Level 1 Supervisor)
- `eu-l1-supervisor-1` (EU Level 1 Supervisor)

### Operators:
- `us-l1-operator-1` (US Level 1 Worker)
- `us-l2-operator-1` (US Level 2 Worker)

### Admins:
- `operation-user-1` (Can do everything)
- `automation-user-2` (For system operations)

## Quick Test

Try this to make sure everything works:
```bash
curl -X GET "http://localhost:8090/actuator/health" 
```

Should return: `{"status":"UP"}`

If that works, you're ready to use all the other APIs!