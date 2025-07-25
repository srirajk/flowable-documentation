# Simple Guide: How Security Works

## What is Metadata?

**Metadata = Information about information**

Think of it like this:
- **Task**: "Approve John's vacation request"
- **Metadata**: Who submitted it, how much money, which department, what step we're on

## Why Do We Need Metadata?

The security system (Cerbos) needs to know details to make smart decisions:

### Example Decision:
```
Question: "Can Alice approve this expense?"

Cerbos needs to know:
- Who is Alice? (Manager? HR? Finance?)
- What expense? ($100? $10,000?)
- Who submitted it? (Alice herself? Her team member?)
- What step? (First review? Final approval?)

Answer: "Yes, Alice can approve it" OR "No, Alice cannot"
```

## Two Types of Information

### 1. **About the User** (Principal)
- **Who they are**: Alice, Bob, Carol
- **What role**: Manager, HR, Finance
- **What department**: Sales, Engineering, Finance
- **What they can do**: Approve up to $5,000

### 2. **About the Task** (Resource)  
- **What workflow**: Vacation request, Expense claim
- **Current step**: Review, Approval, Final check
- **Data inside**: Amount, department, requester
- **History**: Who did previous steps

## How It Works (Simple Example)

### Vacation Request Example:

#### User Information:
```
Alice = {
  role: "Manager",
  department: "Sales", 
  region: "US",
  can_approve: "team_requests"
}
```

#### Task Information:
```
Vacation Request = {
  workflow: "vacation_approval",
  current_step: "manager_approval",
  submitted_by: "John (Sales team)",
  days: 5,
  previous_steps: "none"
}
```

#### Security Decision:
```
Can Alice approve John's vacation?

✅ Alice is a Manager (has approval role)
✅ John is in Sales (Alice's department)  
✅ 5 days is reasonable (not excessive)
✅ No previous steps by Alice (no conflict)

Result: ALLOW
```

## Real Security Checks

### Check 1: Right Role
```
Question: Can this person do this action?
Check: User role vs required role
Example: Only "Manager" can approve vacation
```

### Check 2: Right Queue  
```
Question: Can this person access this queue?
Check: User's queues vs task's queue
Example: Alice can access "manager-queue" but not "hr-queue"
```

### Check 3: Four-Eyes Rule
```
Question: Did this person do a previous step?
Check: Task history vs current user
Example: If Alice reviewed, she cannot also approve
```

### Check 4: Business Rules
```
Question: Does this meet business requirements?
Check: Data values vs company policies  
Example: Expenses over $1000 need senior approval
```

## Where Information Comes From

### User Information Sources:
1. **Database tables**: User roles, departments, permissions
2. **HR system**: Employee details, manager relationships
3. **Active Directory**: Login information, groups

### Task Information Sources:
1. **Workflow engine**: Current step, process variables
2. **Queue system**: Task status, assignments
3. **Business data**: Form fields, amounts, dates
4. **History**: Previous steps, who did what

## Examples of Smart Decisions

### Example 1: Expense Approval
```
User: Bob (Senior Manager)
Task: $2,000 expense from his team

Checks:
✅ Bob is Senior Manager (right role)
✅ $2,000 is under his limit ($5,000)
✅ Team member submitted (not Bob himself)
✅ First approval step (no conflicts)

Decision: ALLOW
```

### Example 2: Purchase Order
```
User: Carol (Finance Manager)  
Task: $50,000 purchase order

Checks:
✅ Carol is Finance Manager (right role)
❌ $50,000 exceeds her limit ($25,000)
✅ Different person submitted it
✅ Previous reviews completed

Decision: DENY (amount too high, needs CEO approval)
```

### Example 3: Document Review
```
User: Dave (Team Lead)
Task: Approve document he already reviewed

Checks:
✅ Dave is Team Lead (right role)
✅ Document is from his team (right department)  
❌ Dave already did the review step (Four-Eyes violation)

Decision: DENY (someone else must approve)
```

## What Makes This System Smart

### 1. **Context-Aware**
- Knows who you are, what you're trying to do
- Considers all relevant information
- Makes decisions based on full picture

### 2. **Real-Time**
- Uses live data from workflows
- Updates as things change
- No stale information

### 3. **Flexible**
- Different rules for different workflows
- Easy to change policies without code
- Handles complex business scenarios

### 4. **Auditable**
- Records all decisions
- Explains why allowed or denied
- Full trail for compliance

## For Regular Users

**You don't need to understand all this!**

The system automatically:
- ✅ Shows you tasks you can work on
- ✅ Hides tasks you shouldn't see  
- ✅ Prevents security violations
- ✅ Guides you through proper process

Just focus on your work - the security handles itself!

## For Administrators

When setting up security, you control:
- **User roles and permissions**
- **Queue access rules** 
- **Business approval limits**
- **Four-Eyes enforcement**
- **Department restrictions**

The system uses all this information to make smart, secure decisions automatically.