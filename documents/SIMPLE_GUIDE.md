# Simple Guide to Flowable Wrapper

## What is This?

This is a **smart task management system** built on top of Flowable. Think of it like having different **inboxes** for different teams where tasks automatically appear.

## Key Ideas (Very Simple)

### 1. **Queues = Inboxes**
- Instead of complex Flowable concepts, we use simple **queues**
- Each team has their own queue: "Manager Queue", "HR Queue", etc.
- Tasks automatically go to the right queue

### 2. **Four-Eyes Principle**
- Important rule: **Same person cannot do both steps**
- Example: If John reviews a document, Mary must approve it (not John)
- System automatically prevents this

### 3. **Business Apps**
- Different systems: "HR System", "Finance System", etc.
- Each has its own workflows and users
- Users only see their own systems

## How It Works (Step by Step)

### Step 1: Someone Starts a Workflow
```
Employee submits vacation request → System creates workflow
```

### Step 2: Tasks Go to Right Queues
```
Manager approval needed → Task goes to "Manager Queue"
HR review needed → Task goes to "HR Queue"
```

### Step 3: People Work on Tasks
```
1. Manager opens their queue
2. Sees vacation request
3. Clicks "Approve" or "Reject"
4. Task moves to next step
```

## What Makes This Special?

### ✅ **Simple for Users**
- No need to understand BPMN or Flowable
- Just check your queue and complete tasks

### ✅ **Smart Security**
- Only see tasks you're allowed to see
- System prevents security violations automatically

### ✅ **No Code Needed**
- Business rules in the workflow diagram
- No programming required for basic changes

## Common Examples

### Example 1: Vacation Request
1. **Employee**: Submits request
2. **Manager Queue**: Approve/reject
3. **HR Queue**: Final processing
4. **Employee**: Gets notification

### Example 2: Purchase Order
1. **Requester**: Creates purchase order
2. **Manager Queue**: First approval
3. **Finance Queue**: Budget check
4. **Senior Manager Queue**: Final approval (if high amount)

### Example 3: Document Review
1. **Author**: Submits document
2. **Reviewer Queue**: First review
3. **Approver Queue**: Different person approves
4. **System**: Four-Eyes rule prevents same person doing both

## For Developers (Simple Version)

### Main Parts:
- **Controllers**: Handle web requests
- **Services**: Do the work
- **Cerbos**: Security decisions
- **Database**: Store everything

### Key Files:
- **TaskController**: Handle task operations
- **CerbosService**: Check permissions
- **QueueTaskService**: Manage queues

## Troubleshooting

### Problem: Can't see tasks
**Check**: Do you have access to the right queue?

### Problem: Can't claim task
**Check**: Is someone else already working on it?

### Problem: Authorization error
**Check**: Do you have the right role for this action?

## Getting Started

1. **For Users**: Just log in and check your queue
2. **For Admins**: Set up users and roles
3. **For Developers**: Deploy workflows and test

That's it! The system handles all the complex stuff automatically.