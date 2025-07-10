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