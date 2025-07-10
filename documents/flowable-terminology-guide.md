# Flowable Terminology Guide

## Core Concepts

### 1. Process Definition
- **What it is**: The blueprint/template of your workflow (the BPMN file)
- **Example**: "Purchase Order Approval" workflow template
- **Key Point**: This is NOT a running instance, just the design

### 2. Process Instance
- **What it is**: A running execution of a process definition
- **Example**: John's laptop purchase request #12345
- **Key Point**: Multiple instances can run from one definition

### 3. Task
- **What it is**: A unit of work that needs to be done
- **Types**:
  - **User Task**: Requires human action (e.g., "Approve Request")
  - **Service Task**: Automated system action
  - **Script Task**: Runs code/script
- **Key Point**: Tasks appear and disappear as the workflow progresses

### 4. Activity
- **What it is**: Any step in the workflow (tasks, gateways, events)
- **Example**: Manager Review, Check Amount, Send Email
- **Key Point**: Not all activities require human interaction

## Assignment Concepts

### 1. Assignee
- **What it is**: The specific person assigned to complete a task
- **Example**: assignee="john.doe"
- **Key Point**: Only one person can be the assignee

### 2. Candidate Groups
- **What it is**: Groups that can claim/work on a task
- **Example**: candidateGroups="managers,supervisors"
- **Key Point**: Anyone in these groups can claim the task

### 3. Candidate Users
- **What it is**: Specific users who can claim a task
- **Example**: candidateUsers="john.doe,jane.smith"
- **Key Point**: Alternative to groups for specific people

## Flow Control

### 1. Sequence Flow
- **What it is**: The arrows connecting activities
- **Example**: Start → Manager Review → Decision
- **Key Point**: Defines the order of execution

### 2. Gateway
- **What it is**: Decision points that control flow direction
- **Types**:
  - **Exclusive**: Only one path taken (XOR)
  - **Parallel**: All paths taken (AND)
  - **Inclusive**: One or more paths taken (OR)

### 3. Conditional Flow
- **What it is**: Flow with a condition that must be true
- **Example**: `amount > 5000` goes to Finance
- **Key Point**: Uses expressions to evaluate

## Variables

### 1. Process Variables
- **What it is**: Data that lives throughout the process
- **Example**: amount, requester, itemDescription
- **Scope**: Entire process instance
- **Usage**: `${variableName}` in BPMN

### 2. Task Variables
- **What it is**: Data specific to a single task
- **Example**: managerComments, approvalDecision
- **Scope**: Just that task
- **Key Point**: Can become process variables when task completes

### 3. Execution Variables (Advanced - Usually Not Needed)
- **What it is**: Variables scoped to a specific execution path/branch
- **What's a branch?**: When workflow splits (like parallel gateway), each path is a "branch"
- **Real Example - Parallel Approval**:
  ```
  Start → Split into 3 parallel branches:
           ├→ Legal Review (Branch 1/Execution 1)
           ├→ Finance Review (Branch 2/Execution 2)  
           └→ Tech Review (Branch 3/Execution 3)
         All merge back → Continue
  ```
- **Why it exists**: Each branch gets its own "execution" so they don't interfere
- **Important**: You rarely need to worry about this! Flowable handles it automatically

### Task Variables vs Process Variables (What You Actually Use)

**Example: Maker-Checker Pattern**
```
Start → Maker Task → Checker Task → End
```

**Option 1: Using Process Variables (Most Common)**
```java
// Maker completes task
complete(makerTaskId, {
  "documentPrepared": true,
  "makerComments": "Document ready for review",
  "preparedBy": "john.doe"
});

// These become PROCESS variables - checker can see them
// Checker task can access ${makerComments}, ${preparedBy}
```

**Option 2: Task-Specific Variables**
```java
// Variables only exist during that task
// They disappear unless you explicitly save them as process variables
```

**For Maker-Checker**: You typically use process variables so the checker can see what the maker did. No need for execution variables!

## Events

### 1. Start Event
- **What it is**: How a process begins
- **Symbol**: Thin circle
- **Types**: None (manual), Timer, Message, Signal

### 2. End Event
- **What it is**: How a process ends
- **Symbol**: Thick circle
- **Types**: None, Error, Terminate

### 3. Intermediate Events
- **What it is**: Events that happen during the process
- **Example**: Timer (wait 3 days), Message (wait for payment)
- **Key Point**: Can be throwing or catching

## BPMN Elements

### 1. Pool
- **What it is**: Container representing a participant
- **Example**: "Company", "Customer", "Vendor"
- **Key Point**: Not always needed for simple workflows

### 2. Lane
- **What it is**: Sub-division within a pool
- **Example**: "HR Department", "Finance Department"
- **Key Point**: Helps organize by role/department

### 3. Sub-Process
- **What it is**: A process within a process
- **Example**: "Document Review" containing multiple steps
- **Key Point**: Can be collapsed to simplify view

## State Management

### 1. Process State
- **Active**: Currently running
- **Suspended**: Paused, can be resumed
- **Completed**: Finished successfully
- **Terminated**: Ended early

### 2. Task State
- **Created**: Task exists but not claimed
- **Claimed**: Someone owns it
- **Completed**: Finished
- **Delegated**: Passed to someone else

## Key APIs to Remember

### 1. Deployment API
- **Purpose**: Upload and activate BPMN files
- **Endpoint**: `/repository/deployments`

### 2. Process API
- **Purpose**: Start and manage process instances
- **Endpoint**: `/runtime/process-instances`

### 3. Task API
- **Purpose**: Query and complete tasks
- **Endpoint**: `/runtime/tasks`

### 4. History API
- **Purpose**: View completed processes/tasks
- **Endpoint**: `/history/process-instances`

## Common Patterns

### 1. Approval Pattern
```
Start → Review → Decision Gateway → (Approved/Rejected) → End
```

### 2. Multi-Level Approval
```
Start → Level 1 → Level 2 → Level 3 → End
         ↓         ↓         ↓
       Reject    Reject    Reject
```

### 3. Parallel Review
```
         → Reviewer A →
Start →                  → Merge → End
         → Reviewer B →
```

### 4. Loop Pattern
```
Start → Task → Decision → (Need More Info) ↻
                ↓
              (Done) → End
```

## Quick Reference

| Term | Simple Explanation |
|------|-------------------|
| Deploy | Upload workflow to engine |
| Start Process | Create new instance from template |
| Claim Task | Take ownership of work |
| Complete Task | Finish work and move forward |
| Process Variable | Data that travels with workflow |
| Gateway | Decision point in flow |
| User Task | Human work required |
| Service Task | System does it automatically |