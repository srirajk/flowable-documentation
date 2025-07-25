# TODO Items - Flowable Wrapper API

## High Priority Issues

### 1. Task Validation Failure - Task ID Continuity Bug

**Issue**: When BPMN validation fails, the system creates a NEW task with a NEW task ID instead of maintaining the original task ID for retry.

**Current Broken Behavior**:
```
User claims Task-123 → Submits with validation error → 
Flowable creates Task-456 (new ID) → 
Wrapper creates new queue_tasks record with assignee=null → 
User loses ownership and has to reclaim
```

**Expected Correct Behavior**:
```
User claims Task-123 → Submits with validation error → 
Keep Task-123 (same ID) → 
Update existing queue_tasks record → 
Maintain assignee ownership for retry
```

**ASCII Diagram - Current BROKEN Behavior**:
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   User Action   │    │  Flowable BPMN  │    │  Wrapper Queue  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
    1. Claim Task-123            │                       │
         │              ┌─────────────────┐             │
         │              │ queue_tasks:    │             │
         │              │ id: Task-123    │             │
         │              │ assignee: user1 │             │
         │              │ status: CLAIMED │             │
         │              └─────────────────┘             │
         │                       │                       │
    2. Submit with                │                       │
       validation error           │                       │
         │                       │                       │
         │              ┌─────────────────┐             │
         │              │ BPMN creates    │             │
         │              │ NEW Task-456!   │             │
         │              └─────────────────┘             │
         │                       │                       │
         │                       │              ┌─────────────────┐
         │                       │              │ NEW queue_tasks:│
         │                       │              │ id: Task-456    │
         │                       │              │ assignee: NULL  │ ❌ BROKEN!
         │                       │              │ status: OPEN    │
         │                       │              └─────────────────┘
         │                       │                       │
    3. User must reclaim          │                       │
       different task! ❌         │                       │
```

**ASCII Diagram - Correct FIXED Behavior**:
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   User Action   │    │  Flowable BPMN  │    │  Wrapper Queue  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
    1. Claim Task-123            │                       │
         │              ┌─────────────────┐             │
         │              │ queue_tasks:    │             │
         │              │ id: Task-123    │             │
         │              │ assignee: user1 │             │
         │              │ status: CLAIMED │             │
         │              └─────────────────┘             │
         │                       │                       │
    2. Submit with                │                       │
       validation error           │                       │
         │                       │                       │
         │              ┌─────────────────┐             │
         │              │ BPMN creates    │             │
         │              │ internal        │             │
         │              │ Task-456        │             │
         │              └─────────────────┘             │
         │                       │                       │
         │                       │              ┌─────────────────┐
         │                       │              │ SAME queue_task:│
         │                       │              │ id: Task-123    │ ✓ SAME ID!
         │                       │              │ assignee: user1 │ ✓ SAME USER!
         │                       │              │ status: CLAIMED │ ✓ KEEP CLAIM!
         │                       │              │ retry_count: 1  │ ✓ TRACK RETRY!
         │                       │              │ flowable_id:456 │ ✓ MAP INTERNAL!
         │                       │              └─────────────────┘
         │                       │                       │
    3. User continues with        │                       │
       SAME task! ✓              │                       │
```

**Why This Matters**:
- **Task ID is the primary user identifier** - users recognize tasks by ID
- **Audit trail continuity** - same logical task should have same ID
- **User experience** - no need to reclaim after validation failure
- **Four-Eyes Principle integrity** - prevents accidental task switching between users
- **Workflow continuity** - validation failures are retries, not new tasks

**Technical Implementation Strategy**:
1. **Task ID Mapping**: Create mapping between Flowable task IDs and logical wrapper task IDs
2. **Queue Task Update**: Instead of creating new queue_tasks record, update existing one
3. **Status Management**: Reset status from COMPLETED → CLAIMED (maintain ownership)
4. **Retry Counter**: Track validation attempt numbers
5. **Error Context**: Store validation errors for user feedback

**Files to Modify**:
- `TaskService.java:completeTask()` - validation failure handling
- `QueueTaskService.java:populateQueueTasksForProcessInstance()` - prevent duplicate creation
- Database schema - add retry_count, logical_task_id fields
- `TaskCompletionResponse` - include retry context

**Priority**: **CRITICAL** - This breaks fundamental user experience and workflow integrity

---

## Medium Priority Items

### 2. [Future items to be added as discovered]

---

## Design Principles for Flowable Wrapper API

### Task Validation Standards
When implementing task validation in BPMN processes using our wrapper API ecosystem:

1. **Task ID Continuity**: Validation failures must preserve the original task ID
2. **Assignee Preservation**: User ownership must be maintained through validation cycles
3. **Retry Semantics**: Validation failures are retries, not new task instances
4. **Error Context**: Provide clear validation error details to users
5. **Audit Integrity**: Complete task history under single logical identifier

This establishes the standard pattern for anyone building validation-enabled workflows with our Flowable wrapper API.