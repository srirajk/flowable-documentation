# Wrapper API Endpoint Design

## Overview
This document describes each endpoint in plain English - what it does, what logic it needs, and how it should behave.

## 1. Start Process Endpoint

**Purpose**: Start a new workflow instance

**Endpoint**: `POST /api/processes/start`

**What it does**:
1. Receives process key and initial data from user
2. Validates user has permission to start this process type
   - Phase 1: Only ADMIN role can start processes
   - Phase 2: Automated/scheduled starts
   - Phase 3: API keys/webhooks for external systems
3. Calls Flowable to start the process (let Flowable validate variables)
4. If successful, returns process instance ID and initial task info
5. If failed, returns Flowable's validation error

**Request Example**:
```json
{
  "processKey": "purchase-order-approval",
  "businessKey": "PO-2024-001",  // Optional, for business reference
  "variables": {
    "amount": 5000,
    "vendor": "ABC Corp",
    "requester": "john.doe"
  }
}
```

**Logic Flow**:
- Call Flowable RuntimeService.startProcessInstanceByKey()
- If successful:
  - Query what tasks were created
  - Return success with process ID
- If failed:
  - Return Flowable's error (e.g., missing required variables)

**Response Example**:
```json
{
  "processInstanceId": "proc-123",
  "businessKey": "PO-2024-001",
  "status": "active",
  "currentTasks": [
    {
      "taskId": "task-456",
      "name": "Manager Review",
      "assignee": null,
      "queue": "managers"
    }
  ]
}
```

## 2. Get My Queue Endpoint

**Purpose**: Get tasks available to the current user

**Endpoint**: `GET /api/tasks/my-queue`

**What it does**:
1. Identifies the current user
2. Finds all groups the user belongs to
3. Queries wrapper's queue table for tasks in those groups
4. Returns tasks user can see/claim

**Query Parameters**:
- `status` - filter by task status (open, claimed)
- `processType` - filter by process definition key
- `priority` - filter by priority
- `page` - pagination
- `size` - page size

**Logic Flow**:
- Get user's groups from identity service
- Query queue table WHERE group IN (user's groups)
- Filter by any query parameters
- Sort by priority and create time
- Return paginated results

**Response Example**:
```json
{
  "tasks": [
    {
      "taskId": "task-456",
      "name": "Manager Review",
      "processInstanceId": "proc-123",
      "processName": "Purchase Order Approval",
      "createTime": "2024-01-15T10:00:00Z",
      "priority": "high",
      "queue": "managers",
      "assignee": null,
      "variables": {
        "amount": 5000,
        "vendor": "ABC Corp"
      }
    }
  ],
  "page": 0,
  "size": 20,
  "total": 45
}
```

## 3. Claim Task Endpoint

**Purpose**: Assign a task to the current user

**Endpoint**: `POST /api/tasks/{taskId}/claim`

**What it does**:
1. Verifies task exists and is not already claimed
2. Checks user is in a valid group for this task
3. (Future: Calls Cerbos for complex rules)
4. Claims task in Flowable
5. Updates queue table with assignment

**Logic Flow**:
- Fetch task from Flowable
- Check task not already assigned
- Verify user can claim (is in candidate group)
- Call Flowable TaskService.claim()
- Update wrapper's queue table
- Return success

**Request**: No body needed (user from auth context)

**Response Example**:
```json
{
  "success": true,
  "taskId": "task-456",
  "assignee": "john.doe",
  "claimedAt": "2024-01-15T10:30:00Z"
}
```

**Error Cases**:
- Task already claimed → 409 Conflict
- User not authorized → 403 Forbidden
- Task not found → 404 Not Found

## 4. Complete Task Endpoint

**Purpose**: Complete a task and move the process forward

**Endpoint**: `POST /api/tasks/{taskId}/complete`

**What it does**:
1. Verifies user owns the task
2. Completes task in Flowable (let Flowable validate required variables)
3. If successful, lets event listener handle queue updates
4. Returns what happened next in the process
5. If failed, returns Flowable's validation error

**Request Example**:
```json
{
  "variables": {
    "approved": true,
    "comments": "Looks good, approved for purchase"
  }
}
```

**Logic Flow**:
- Fetch task from Flowable
- Verify current user is assignee
- Call TaskService.complete() with provided variables
- If successful:
  - Query process instance for next active tasks
  - Return next state info
- If failed:
  - Return Flowable's validation error

**Response Example**:
```json
{
  "success": true,
  "completedTaskId": "task-456",
  "processInstanceId": "proc-123",
  "nextTasks": [
    {
      "taskId": "task-789",
      "name": "Finance Approval",
      "queue": "finance",
      "created": true
    }
  ],
  "processStatus": "active"  // or "completed" if process ended
}
```

## Common Patterns

### Authentication
- All endpoints require authenticated user
- User identity from Spring Security context
- Groups/roles from identity service

### Error Handling
```json
{
  "error": "TASK_ALREADY_CLAIMED",
  "message": "Task is already assigned to another user",
  "details": {
    "taskId": "task-456",
    "currentAssignee": "jane.doe"
  }
}
```

### Event Side Effects
- Task created → Event listener updates queue table
- Task completed → Event listener updates queue table
- Process completed → Event listener cleans up queues

## Database Tables Needed

### queue_tasks
- task_id (PK)
- process_instance_id
- task_name
- task_definition_key
- queue_name (group)
- assignee
- status (open, claimed, completed)
- priority
- created_at
- claimed_at
- metadata (JSONB)

### process_metadata
- process_definition_id (PK)
- process_definition_key
- version
- task_definitions (JSONB - all tasks and their groups)
- deployed_at
- deployed_by

## Next Steps
Once we agree on these endpoints, we can:
1. Create the Spring Boot project structure
2. Implement controllers
3. Add service layer with business logic
4. Set up event listeners
5. Create database migrations