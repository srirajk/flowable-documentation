# Variables and Retry Scenarios

## Retry Scenarios

### 1. Task Retry (User Changes Their Mind)
When a user needs to redo a task:

```
Manager Review → Reject → Notify Rejection → END
      ↑                                         
      └── User appeals, process restarted ──────┘
```

**What happens to variables:**
- Process variables remain unchanged until task is completed again
- Previous task variables are gone
- It's like the task is brand new

**Example:**
```json
// First attempt - Manager rejects
{
  "managerDecision": "rejected",
  "managerComments": "Budget not available"
}

// Process restarted/retried - Manager now approves
{
  "managerDecision": "approved",  // Overwrites previous
  "managerComments": "Budget found from Q2"  // Overwrites
}
```

### 2. Technical Retry (System Failure)

#### Service Task Retry
```xml
<serviceTask id="callPaymentAPI" 
             flowable:class="com.example.PaymentService"
             flowable:failedJobRetryTimeCycle="R3/PT10M">
  <!-- Retry 3 times, wait 10 minutes between -->
</serviceTask>
```

**What happens:**
- Process variables are NOT lost
- Task starts fresh each retry
- Can track retry count

**Example:**
```json
// Process Variables (persist across retries)
{
  "orderId": "ORD-123",
  "amount": 1000,
  "paymentAttempts": 0  // Can increment this
}

// First attempt fails
// Variables still there!
// Second attempt fails  
// Variables STILL there!
// Third attempt succeeds
{
  "orderId": "ORD-123",
  "amount": 1000,
  "paymentAttempts": 3,
  "paymentConfirmation": "PAY-98765"  // Added on success
}
```

### 3. Compensation (Undo and Redo)

When you need to undo completed work:

```xml
<transaction id="bookingTransaction">
  <userTask id="bookFlight" />
  <userTask id="bookHotel" />
  <serviceTask id="chargeCard" />
</transaction>

<!-- If card charge fails, compensate -->
<compensateEventDefinition />
```

**Variables during compensation:**
- Original process variables preserved
- Compensation handlers can access them
- Can add "compensation" variables

```json
{
  "bookingId": "BK-123",
  "flightBooked": "FL-789",
  "hotelBooked": "HT-456",
  "compensationReason": "Payment failed",
  "flightCancelled": true,
  "hotelCancelled": true
}
```

### 4. Loop Back Pattern (Like "Need More Info")

```
Manager Review → Need Info → Provide Info → Manager Review
        ↑                                          ↓
        └────────────────────────────────────────┘
```

**Variables accumulate:**
```json
// First review
{
  "amount": 5000,
  "managerDecision": "needsInfo",
  "managerComments": "Need vendor comparison"
}

// After info provided
{
  "amount": 5000,
  "managerDecision": "needsInfo",  // Still there
  "managerComments": "Need vendor comparison",  // Still there
  "additionalInfo": "Vendor A: $5000, Vendor B: $5500",  // Added
  "updatedAmount": 4800  // Maybe they negotiated
}

// Second review
{
  "amount": 4800,  // Updated
  "managerDecision": "approved",  // Overwritten
  "managerComments": "Good negotiation, approved",  // Overwritten
  "additionalInfo": "Vendor A: $5000, Vendor B: $5500"  // Preserved
}
```

## Best Practices for Retries

### 1. Version Your Updates
```json
{
  "decision_v1": "rejected",
  "comments_v1": "Over budget",
  "decision_v2": "approved",
  "comments_v2": "Found additional budget",
  "currentVersion": 2
}
```

### 2. Track Attempts
```json
{
  "attempts": [
    {
      "attemptNumber": 1,
      "timestamp": "2024-01-15T10:00:00Z",
      "decision": "rejected",
      "reason": "Budget"
    },
    {
      "attemptNumber": 2,
      "timestamp": "2024-01-15T14:00:00Z",
      "decision": "approved",
      "reason": "Budget found"
    }
  ]
}
```

### 3. Use Arrays for History
```json
{
  "reviewHistory": [
    {
      "reviewer": "manager1",
      "decision": "needsInfo",
      "timestamp": "2024-01-15T10:00:00Z"
    },
    {
      "reviewer": "manager1",
      "decision": "approved",
      "timestamp": "2024-01-15T11:00:00Z"
    }
  ]
}
```

## Retry Configuration

### Async Retry (Service Tasks)
```xml
<serviceTask id="sendEmail" 
             flowable:async="true"
             flowable:failedJobRetryTimeCycle="R5/PT10M">
  <!-- Retry 5 times, 10 minutes apart -->
</serviceTask>
```

### Manual Retry (User Tasks)
```java
// No automatic retry - user must explicitly restart
// But you can design your workflow to loop back
```

### Dead Letter Queue
```java
// After max retries, job goes to dead letter queue
// Admin can manually retry or investigate
managementService.moveJobToDeadLetterJob(jobId);
managementService.moveDeadLetterJobToExecutableJob(jobId, 3); // 3 more retries
```

## Key Points

1. **Process variables persist** through all retries
2. **Task variables are lost** on retry
3. **Updates overwrite** unless you explicitly preserve history
4. **Design for retries** - use arrays/versioning if history matters
5. **Flowable tracks attempts** in job execution history

## Important Architecture Note: Variables and History

### Active Process vs History

**By Design in Flowable:**
- Process variables are **overwritten** on retry/loop back
- The active process only keeps the **latest value**
- Previous values are NOT stored in the active process

**Example:**
```java
// Loop: Manager Review → Needs Info → Manager Review
// First review:
processVariables.put("managerDecision", "needsInfo");

// After loop back and second review:
processVariables.put("managerDecision", "approved");
// "needsInfo" is GONE from active process - overwritten!
```

### But History Service Has Everything

```java
// Query historical values
List<HistoricVariableInstance> history = historyService
    .createHistoricVariableInstanceQuery()
    .processInstanceId(processInstanceId)
    .variableName("managerDecision")
    .orderByVariableRevision().asc()
    .list();

// Returns:
// Revision 1: "needsInfo" at 10:00 AM
// Revision 2: "approved" at 2:00 PM
```

### Design Implications

1. **Default Behavior**: Variables overwrite (simple and clean)
   ```json
   {
     "decision": "approved",  // Only latest value
     "comments": "Looks good"  // Only latest value
   }
   ```

2. **If You Need History in Process**: Design it explicitly
   ```json
   {
     "currentDecision": "approved",
     "decisionHistory": [
       {"timestamp": "10:00", "decision": "needsInfo", "by": "manager1"},
       {"timestamp": "14:00", "decision": "approved", "by": "manager1"}
     ]
   }
   ```

3. **Best Practice**: 
   - Use simple variables (let them overwrite)
   - Rely on History Service for audit trails
   - Only track history in process if business logic needs it

### Your Wrapper API Considerations

Your API could offer both patterns:
```javascript
// Option 1: Simple (default) - overwrites
POST /api/tasks/{taskId}/complete
{
  "variables": {
    "decision": "approved"  // Overwrites any previous value
  }
}

// Option 2: With history tracking - your API handles the array
POST /api/tasks/{taskId}/complete
{
  "variables": {
    "decision": "approved"
  },
  "preserveHistory": true  // Your API adds to array instead
}
```