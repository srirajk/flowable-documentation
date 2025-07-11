# Wrapper API Requirements - Real World Patterns

## What Actually Happens in Real Organizations

### 1. How People Work
- Users log in and see **"My Queues"** or **"My Tasks"**
- They work from queues, NOT by searching task IDs
- Tasks appear in queues based on:
  - Role/Group membership
  - Skills/Certifications
  - Geographic location
  - Current workload
  - Business rules

### 2. Queue Patterns
- **Personal Queue**: Tasks assigned directly to me
- **Team Queue**: Tasks for my team(s)
- **Skill Queue**: Tasks requiring specific skills
- **Priority Queue**: Urgent items across all queues
- **Escalation Queue**: Overdue items

### 3. Real-World Task Flow
```
New Task Created → Routing Rules → Queue Assignment → User Claims → User Completes
                                        ↓
                                  Notification
```

## Core Requirements

### 1. Entitlements System
- **Who can see which queues?**
  - User X can see: Personal, TeamA, SkillQueue1
  - User Y can see: Personal, TeamB, TeamC, SkillQueue2
  
- **Who can claim from which queues?**
  - View vs Claim permissions
  - Temporary access (covering for someone)
  
- **Who can reassign/escalate?**
  - Supervisors can reassign within team
  - Managers can escalate

### 2. Queue Management
- **Queue Definition**
  - Static queues (predefined teams/roles)
  - Dynamic queues (created by workflow)
  - Virtual queues (filtered views)

- **Queue Operations**
  - List my queues
  - Get queue contents (with filters/sort)
  - Get queue statistics
  - Claim next available task
  - Bulk operations

### 3. Event-Driven Architecture
- **Push, Don't Poll**
  - New task → Event → Queue Service → Notification
  - Task completed → Event → Route next task → Queue
  
- **Events to Handle**
  - Task Created
  - Task Assigned
  - Task Completed  
  - Task Escalated
  - SLA Warning
  - Queue Threshold

### 4. Work Distribution Patterns
- **Auto-Assignment**
  - Round-robin within queue
  - Load-balanced (least busy person)
  - Skill-matched
  - "Sticky" assignment (same person who handled before)

- **Manual Pull**
  - User claims from queue
  - User can filter/sort queue
  - "Get Next" button

### 5. Business Rules for Routing
- **Initial Routing**
  ```
  If amount > 10000 → Senior Approvers Queue
  If region = "EMEA" → EMEA Team Queue  
  If customerType = "VIP" → Priority Queue
  ```

- **Re-Routing**
  ```
  If no action in 2 hours → Escalation Queue
  If user out of office → Team Queue
  If skill needed → Specialist Queue
  ```

## What This Means for Our API

### 1. Not Task-Centric, Queue-Centric
```
Bad:  GET /tasks/{taskId}
Good: GET /queues/{queueId}/tasks
      GET /my-queues
      POST /queues/{queueId}/claim-next
```

### 2. Entitlement-Aware
```
Every API call filtered by:
- User's queue access
- User's permissions
- Current delegation rules
```

### 3. Event Producers
```
Task operations produce events:
- TaskCreatedEvent
- TaskClaimedEvent
- TaskCompletedEvent
- TaskRoutedEvent
```

### 4. Queue Configuration
```
Queues defined by:
- Flowable candidate groups
- Business metadata
- Dynamic rules
- User preferences
```

## Key Questions to Answer

1. **Queue Definition**: Where/how are queues defined?
   - In BPMN (candidateGroups)?
   - In wrapper config?
   - Dynamically via API?
   - Combination?

2. **Entitlements Source**: Where do permissions come from?
   - External IAM system?
   - Database tables?
   - Process variables?
   - LDAP/AD groups?

3. **Event Infrastructure**: What handles events?
   - Kafka/RabbitMQ?
   - Webhooks?
   - Internal event bus?
   - Database polling?

4. **Queue Persistence**: Where is queue state stored?
   - Just query Flowable?
   - Cache layer?
   - Separate queue service?

5. **Multi-Tenant**: How handle multiple organizations?
   - Separate deployments?
   - Tenant ID in variables?
   - Different queues per tenant?

## User Experience Goals

1. **Login → See My Work**
   - No searching needed
   - Prioritized view
   - Clear next action

2. **One Click Claims**
   - "Get Next Task" button
   - Auto-open task details
   - Lock to prevent conflicts

3. **Smart Routing**
   - Right person, right time
   - No cherry-picking
   - Fair distribution

4. **Real-Time Updates**
   - New task notifications
   - Queue count badges
   - SLA warnings

5. **Manager Visibility**
   - Queue health metrics
   - Bottleneck detection
   - Workload balance

## Success Metrics

- Time to claim task < 10 seconds
- No tasks "lost" or unclaimed > SLA
- Fair distribution (±10% across team)
- Zero permission errors
- Real-time queue counts

## Key Architecture Decision: Wrapper-Managed Queues

### The Approach
When a task is completed:
1. **Event Triggered** (via Postgres WAL or Spring event listener)
2. **Wrapper API queries** "What tasks are now active?"
3. **Updates internal queue state** with task metadata
4. **Manages queue assignment** based on business rules

### Benefits of Managing Queue State in Wrapper
- **Performance**: No constant polling of Flowable
- **Enrichment**: Add business metadata not in Flowable
- **Custom Routing**: Apply complex rules outside BPMN
- **Analytics**: Track queue metrics, wait times
- **Real-time**: Instant notifications via wrapper's state

### Event Options
1. **Postgres WAL** 
   - Listen to Flowable's database changes
   - Get notified when task table changes
   - Most real-time option

2. **Spring Boot Event Hooks**
   - Use Flowable's event listeners
   - Cleaner integration
   - Example: `@EventListener(TaskCreatedEvent.class)`

3. **Flowable Task Listeners**
   - Configure in BPMN
   - Call wrapper API on task events
   - More control per process

### Queue State in Wrapper
```
Wrapper maintains:
- Task ID → Queue mapping
- Task metadata (title, priority, SLA)
- Assignment history
- Queue statistics
- User workload

Flowable maintains:
- Process state
- Task ownership
- Process variables
- Execution flow
```

### Event-Driven Queue Population Flow

When a task is completed:
1. **Event Triggered** - Task completion event fires (via listener/WAL)
2. **Query Next Tasks** - Get all active tasks for the process instance
3. **Get Identity Links** - For each new task, fetch candidate groups
   ```
   GET /runtime/tasks/{taskId}/identitylinks
   → Returns all candidate groups for the task
   ```
4. **Store Queue Mapping** - Save in wrapper DB:
   - Task ID
   - Candidate Groups (queues)
   - Task metadata
   - Process context
5. **Queue Ready** - Users see tasks in their queues immediately

### Why This Matters
- **Flowable doesn't return groups in task list** - Must query separately
- **Performance** - Cache group info vs multiple API calls
- **Real-time** - Users see queue updates instantly
- **Rich Data** - Wrapper can enrich with business metadata

## Complex Scenarios to Handle

### 1. Dynamic Entitlement Changes
- **User leaves team** - Remove from queue access mid-process
- **User joins team** - Add to queue access for existing tasks
- **Role changes** - Manager becomes individual contributor
- **Temporary coverage** - User A covers for User B during vacation

### 2. Cross-Team Visibility
- **Supervisors** - Can view but not claim team's tasks
- **Escalation managers** - Can claim after SLA breach
- **Auditors** - Read-only access to all queues
- **Floaters** - Can help any team when needed

### 3. Task Reassignment Patterns
- **Auto-reassign** - If user doesn't act in X hours
- **Return to queue** - User can "unclaim" task
- **Delegate to specific person** - Keep audit trail
- **Escalate to manager** - Changes candidate groups

### 4. Skills-Based Routing
```
Task requires: ["spanish", "senior", "compliance"]
User has: ["spanish", "senior", "legal"]
Result: User CANNOT see task (missing "compliance")
```

### 5. Time-Based Availability
- **Business hours** - Different queues for different timezones
- **On-call rotation** - Queue membership changes by schedule
- **Holiday coverage** - Automatic rerouting

### 6. Load Balancing Rules
- **Max active tasks** - User at limit? Hide queue
- **Fair distribution** - Round-robin within team
- **Skill + availability** - Complex scoring

### 7. Special Assignment Rules
- **Sticky assignment** - Same person who handled before
- **Separation of duties** - Can't review own work
- **Four-eyes principle** - Different person must approve

### 8. Queue Overflow Handling
- **Primary queue full** → Route to overflow queue
- **No one available** → Escalate to broader group
- **SLA approaching** → Add senior staff to candidates

### 9. Audit & Compliance
- **Who saw the task** - Track queue visibility
- **Who could have claimed** - Point-in-time entitlements
- **Why user couldn't see** - Explain missing tasks

### 10. External System Integration
- **HR system** - Real-time role updates
- **Schedule system** - Who's working today
- **Skills database** - Current certifications

## Design Implications

### Entitlement Service Needs
```javascript
{
  // Basic check
  canUserSeeQueue(userId, queueId, context)
  
  // Complex check
  canUserClaimTask(userId, taskId, {
    userSkills: [],
    workload: 5,
    timeOfDay: "14:00",
    coveringFor: null
  })
  
  // Audit
  getPointInTimeEntitlements(userId, timestamp)
}
```

### Queue Assignment Needs
```javascript
{
  // Beyond simple candidate groups
  task: {
    candidateGroups: ["legal", "senior-legal"],
    requiredSkills: ["contract-law", "m&a"],
    requiredLevel: "senior",
    excludeUsers: ["original-requester"],
    slaEscalationGroups: ["legal-managers"],
    overflowGroups: ["external-counsel"]
  }
}
```

### Event Scenarios
- Task claimed → Check workload limits
- User goes OOO → Reassign active tasks
- SLA warning → Add escalation groups
- Skill expired → Remove from skill queues

## Policy-Based Task Claims (Cerbos Integration)

### The Problem
Flowable only knows about candidate groups - it can't enforce complex rules like:
- **Four-eyes principle** - Different person must review than who created
- **Maker/Checker** - Can't check your own work
- **Segregation of duties** - Can't do both approval steps
- **Conflict of interest** - Can't review your own department's requests

### Solution: Policy Engine at Claim Time

#### Claim Task Flow with Cerbos
```
User attempts to claim task
    ↓
Wrapper API intercepts
    ↓
Query Process History + Current State
    ↓
Call Cerbos with context
    ↓
Cerbos evaluates policies
    ↓
Allow/Deny claim
    ↓
If allowed, claim in Flowable
```

#### Example Cerbos Policy
```yaml
# Four-eyes principle policy
- actions: ["claim"]
  effect: EFFECT_DENY
  roles: ["*"]
  condition:
    match:
      expr: |
        # User cannot claim review task if they submitted the request
        R.attr.processVariables.requester == P.id ||
        # User cannot claim checker task if they were the maker
        R.attr.completedTasks.contains({"userId": P.id, "taskKey": "makerTask"})

# Segregation of duties
- actions: ["claim"]
  effect: EFFECT_DENY  
  roles: ["*"]
  condition:
    match:
      expr: |
        # If user approved legal review, they can't approve finance review
        R.attr.completedTasks.contains({
          "userId": P.id, 
          "taskKey": "legalReview",
          "decision": "approved"
        }) && R.attr.currentTaskKey == "financeReview"
```

#### Context Data for Cerbos
```javascript
// When user tries to claim task
const claimContext = {
  principal: {
    id: "john.doe",
    roles: ["manager", "finance"],
    attr: {
      department: "finance",
      level: "senior",
      skills: ["approvals", "budgets"]
    }
  },
  resource: {
    kind: "task",
    id: taskId,
    attr: {
      // Current task info
      currentTaskKey: "checkerTask",
      candidateGroups: ["finance", "senior-staff"],
      processInstanceId: "proc-123",
      
      // Process history
      processVariables: {
        requester: "john.doe",
        amount: 50000,
        department: "IT"
      },
      
      // Who did what already
      completedTasks: [
        {
          taskKey: "makerTask",
          userId: "john.doe",
          decision: "submit",
          completedAt: "2024-01-15T10:00:00Z"
        }
      ],
      
      // Active assignments
      activeTasks: [
        {
          taskKey: "parallelReview1",
          assignee: "jane.smith"
        }
      ]
    }
  },
  action: "claim"
};
```

### Implementation in Wrapper

```javascript
async function claimTask(taskId, userId) {
  // 1. Get task details
  const task = await flowableApi.getTask(taskId);
  
  // 2. Get process history
  const processHistory = await getProcessHistory(task.processInstanceId);
  
  // 3. Build Cerbos context
  const context = buildCerbosContext(userId, task, processHistory);
  
  // 4. Check with Cerbos
  const decision = await cerbos.checkResource(context);
  
  if (decision.isAllowed()) {
    // 5. Claim in Flowable
    await flowableApi.claimTask(taskId, userId);
    
    // 6. Update wrapper state
    await updateQueueState(taskId, userId);
  } else {
    // Return policy violation
    throw new PolicyError(decision.getDenyReason());
  }
}
```

### Benefits of This Approach

1. **Policies outside BPMN** - Business rules don't clutter workflows
2. **Dynamic rules** - Change policies without redeploying processes
3. **Audit trail** - Log why claims were denied
4. **Complex logic** - Express rules that Flowable can't handle
5. **Reusable** - Same policies across different workflows

### Example Scenarios

1. **Maker/Checker on Same Process**
   - User completes "submitRequest" task
   - Cannot claim "approveRequest" task in same process

2. **Cross-Process Conflicts**
   - User approved vendor in Process A
   - Cannot review invoice from same vendor in Process B

3. **Time-Based Restrictions**
   - User reviewed document version 1
   - Must wait 24 hours before reviewing version 2

4. **Workload Limits**
   - User has 5 active high-priority tasks
   - Cannot claim more high-priority tasks

### Key Design Decision
**Flowable doesn't know about these rules** - it just has the task assigned to groups. Your wrapper enforces all the business policies before allowing the claim to go through to Flowable.

## Separation of Concerns

### Inside Flowable (DMN/Drools)
- Gateway routing decisions (approved/rejected/needs-info)
- Amount-based routing (>$5000 goes to finance)
- Business process flow logic
- Which groups get assigned to tasks

### Outside Flowable (Cerbos)
- Can this specific user claim this task?
- Maker/checker validations
- Four-eyes principle
- Workload limits
- Conflict of interest checks

### Benefits
- BPMN stays clean and focused on process flow
- Complex authorization rules are centralized in Cerbos
- You can change claim policies without touching workflows

## Architecture Decision: Embedded Flowable Engine

### Using Embedded Engine in Spring Boot
The wrapper API will use Flowable as an embedded engine, not as a separate REST service.

### Benefits of Embedded Approach
1. **Performance** - Direct Java method calls, no HTTP overhead
2. **Transactions** - Wrapper logic + Flowable updates in single transaction
3. **Events** - Spring events fire immediately for real-time queue updates
4. **Deployment** - Single application to deploy and manage
5. **Full API Access** - All Flowable features available, not limited by REST

### Architecture
```
Spring Boot Wrapper Application
├── REST Controllers (User-facing APIs)
├── Queue Management Service
├── Cerbos Integration (Claims policies)
├── Flowable Engine (Embedded)
│   ├── RuntimeService
│   ├── TaskService
│   ├── RepositoryService
│   └── HistoryService
├── Event Listeners
│   └── Task Created/Completed → Update Queues
└── PostgreSQL Database
    ├── Wrapper Tables (queues, metadata)
    └── Flowable Tables (ACT_*)
```

### Key Implementation Points
- Use `flowable-spring-boot-starter` dependency
- Flowable services available as `@Autowired` Spring beans
- Share same database and transaction context
- Use Spring events for real-time task tracking
- No separate Flowable server needed