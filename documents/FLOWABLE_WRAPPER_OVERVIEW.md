# Flowable Wrapper - What We Built

## Overview in Simple Terms

Think of this project as a **smart task management system** built on top of Flowable (a powerful workflow engine). We've made Flowable easier to use by adding a layer that organizes work into queues - like having different inboxes for different teams.

## What Problem Does It Solve?

**Without our wrapper:**
- Flowable is powerful but complex
- Tasks are assigned to "candidate groups" (technical term)
- Validation requires Java code
- Users need to understand BPMN concepts

**With our wrapper:**
- Tasks appear in simple queues (like "HR Queue" or "Manager Queue")
- Validation happens automatically without writing code
- Failed tasks come back to the same person with error messages
- Everything works through simple REST APIs

## Core Concepts Explained

### 1. **Queues Instead of Technical Terms**
- **What it is**: We organize tasks into named queues
- **Example**: When someone submits a vacation request, it goes to the "manager-queue"
- **Why it matters**: Users don't need to know about "candidate groups" or BPMN

### 2. **Smart Task Validation**
- **What it is**: Tasks can be automatically checked and sent back if something's wrong
- **Example**: If vacation days exceed allowed limit, task returns with error message
- **Why it matters**: No need to write Java code - validation rules live in the workflow

### 3. **Event-Driven Updates**
- **What it is**: Everything updates in real-time
- **Example**: When a task is completed, it immediately disappears from the queue
- **Why it matters**: No delays or inconsistencies

## How It Works - Real Example

Let's say you're building a vacation request system:

### Step 1: Define Your Workflow
Create a simple workflow (BPMN file) that says:
1. Employee submits request
2. Manager approves (goes to "manager-queue")
3. HR processes (goes to "hr-queue")
4. Employee gets notification

### Step 2: Register the Workflow
Tell the system which queues to use:
```json
{
  "processDefinitionKey": "vacationRequest",
  "candidateGroupMappings": {
    "management": "manager-queue",
    "hr": "hr-queue"
  }
}
```

### Step 3: Use Simple APIs
- **Start a request**: `POST /api/process-instances/start`
- **Get tasks from queue**: `GET /api/queues/manager-queue/tasks`
- **Complete a task**: `POST /api/tasks/{taskId}/complete`

### Step 4: Validation Happens Automatically
If a manager rejects the request:
- Task stays in the same queue
- Employee sees the rejection reason
- Can fix and resubmit

## Technical Architecture (Simplified)

```
Your App → REST APIs → Flowable Wrapper → Flowable Engine → Database
                              ↓
                        Queue Tables
```

### Key Components:

1. **Queue Task Service**
   - Manages the queue abstraction
   - Keeps track of which tasks are in which queues
   - Handles task priorities

2. **Task Service**
   - Handles task operations (claim, complete, reject)
   - Manages validation and error handling
   - Updates task status

3. **Workflow Metadata Service**
   - Registers workflows with queue mappings
   - Deploys BPMN files to Flowable
   - Maintains workflow configurations

## Benefits for Different Users

### For Business Users
- See tasks in familiar "queue" format
- No technical knowledge needed
- Clear validation messages

### For Developers
- Simple REST APIs instead of complex Flowable APIs
- Validation without Java code
- Built-in error handling

### For Operations
- Docker-based deployment
- PostgreSQL for reliability
- Proper logging and monitoring

## What Makes This Special?

1. **No-Code Validation**: Business rules in the workflow, not in Java
2. **Queue-Based Mental Model**: Everyone understands queues/inboxes
3. **Real-Time Updates**: No polling or delays
4. **Enterprise Ready**: Handles errors, scales well, easy to monitor

## Example Use Cases

- **Approval Workflows**: Expense reports, vacation requests, purchase orders
- **Document Processing**: Review queues, approval chains, quality checks
- **Customer Service**: Ticket routing, escalation queues, SLA management
- **HR Processes**: Onboarding tasks, review cycles, training assignments

## Summary

We've taken Flowable (a powerful but complex workflow engine) and wrapped it with a simple, queue-based interface. This makes it easy for anyone to build and use workflow applications without needing deep technical knowledge. The validation pattern ensures data quality without writing code, and the event-driven architecture keeps everything in sync in real-time.