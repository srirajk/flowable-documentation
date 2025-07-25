# Simple Security Guide (Cerbos + Four-Eyes)

## What is Security in This System?

Security means **making sure people only do what they're allowed to do**.

## Two Main Security Rules

### 1. **Role-Based Access**
- **Managers** can approve requests
- **HR people** can process employee stuff  
- **Finance people** can handle money stuff
- **Regular users** can only submit requests

### 2. **Four-Eyes Principle**
- **Important tasks need TWO different people**
- Example: One person reviews, different person approves
- System automatically prevents same person doing both

## How It Works (Very Simple)

### Step 1: User Tries to Do Something
```
User clicks "Claim Task" → System checks: "Are you allowed?"
```

### Step 2: System Checks Rules
```
✅ Right role? (Manager trying to approve)
✅ Right queue? (Task in Manager queue)
✅ Not assigned to someone else?
✅ Didn't do previous step? (Four-Eyes check)
```

### Step 3: Allow or Deny
```
If ALL checks pass → ✅ Allow
If ANY check fails → ❌ Deny
```

## Real Examples

### Example 1: Vacation Approval
```
Employee: "I want vacation"
↓
Manager Queue: "Please approve this vacation"
↓
Manager Alice: ✅ Can approve (right role, right queue)
↓
HR Queue: "Please process this vacation"  
↓
HR Bob: ✅ Can process (different person than Alice)
↓
Manager Alice: ❌ Cannot process (Four-Eyes violation!)
```

### Example 2: High-Value Purchase
```
Employee: "I need to buy $50,000 equipment"
↓
Manager Queue: "Please review"
↓
Manager John: ✅ Can review
↓
Senior Manager Queue: "Please approve"
↓
Senior Manager Sarah: ✅ Can approve (different person)
↓
Manager John: ❌ Cannot approve (Four-Eyes violation!)
```

## What Cerbos Does

**Cerbos is like a security guard** that checks every action:

### Before Every Action:
```
User: "I want to claim this task"
Cerbos: "Let me check..."
Cerbos: "What's your role? What queue? Did you do previous step?"
Cerbos: "OK, you're allowed" or "Sorry, not allowed"
```

### What Cerbos Checks:
- ✅ **User's role**: Manager, HR, Finance, etc.
- ✅ **Queue access**: Can you work on this queue?
- ✅ **Task status**: Is it available?
- ✅ **Four-Eyes**: Did you work on related tasks?
- ✅ **Business rules**: Custom rules for your company

## Security Levels

### Level 1: Basic Access
- **Who**: Regular users
- **Can do**: Submit requests, view their own stuff
- **Cannot do**: Approve anything

### Level 2: Team Lead
- **Who**: Managers, supervisors
- **Can do**: Approve team requests, view team tasks
- **Cannot do**: Approve their own requests

### Level 3: Department Head
- **Who**: Senior managers
- **Can do**: Approve high-value items, override decisions
- **Cannot do**: Approve if they were involved in earlier steps

### Level 4: System Admin
- **Who**: IT administrators
- **Can do**: Everything (emergency access)
- **Should do**: Only use in emergencies

## Common Security Scenarios

### ✅ **Good Examples**:
- Alice submits vacation request
- Bob (Alice's manager) approves it
- Carol (HR) processes it

### ❌ **Blocked Examples**:
- Alice submits request, tries to approve it herself
- Bob reviews purchase order, tries to approve it too
- Carol tries to access Finance queue without permission

## For Regular Users

### What You Can Do:
1. **Submit requests**: Vacation, purchases, etc.
2. **Check your queue**: See tasks assigned to you
3. **Complete your tasks**: Follow the process

### What You Cannot Do:
1. **See other people's tasks**: Privacy protection
2. **Approve your own requests**: Four-Eyes rule
3. **Access wrong queues**: Role-based restriction

## For Managers

### Extra Permissions:
1. **Approve team requests**: Vacation, expenses, etc.
2. **See team queues**: Monitor team work
3. **Escalate issues**: Move tasks to higher level

### Still Cannot Do:
1. **Approve own requests**: Still need different approver
2. **Override Four-Eyes**: Security rule always applies
3. **Access other departments**: Stay in your area

## Troubleshooting Security Issues

### "Access Denied" Error:
1. **Check your role**: Do you have permission?
2. **Check the queue**: Is this your queue?
3. **Check Four-Eyes**: Did you work on this before?

### "Cannot Claim Task" Error:
1. **Someone else claimed it**: Try different task
2. **You're not allowed**: Check your role
3. **Four-Eyes violation**: You did previous step

### "Cannot Complete Task" Error:
1. **Not assigned to you**: Claim it first
2. **Missing information**: Fill all required fields
3. **Business rule violation**: Check requirements

## Why This Security Matters

### Prevents Fraud:
- No one can approve their own requests
- Always need two people for important decisions

### Ensures Compliance:
- Meets audit requirements
- Follows company policies
- Tracks who did what

### Protects Company:
- Prevents unauthorized actions
- Reduces risk of mistakes
- Creates clear accountability

Remember: **Security is automatic**. You don't need to think about it - the system handles it for you!