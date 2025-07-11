# Variables in BPMN XML

## The Truth: BPMN Doesn't Declare Variable Types

In BPMN XML, you **don't explicitly declare** whether something is a process, task, or execution variable. Instead, it depends on **how and where** you use them.

## Where Variables Appear in BPMN

### 1. Form Properties (User Input)
```xml
<userTask id="managerReview" name="Manager Review">
  <extensionElements>
    <flowable:formProperty id="managerApproved" name="Decision" type="enum" required="true">
      <flowable:value id="approved" name="Approve" />
      <flowable:value id="rejected" name="Reject" />
    </flowable:formProperty>
    <flowable:formProperty id="managerComments" name="Comments" type="string"/>
  </extensionElements>
</userTask>
```
**What happens**: When task completes, these become **process variables** by default.

### 2. Variable References (Using ${})
```xml
<!-- In documentation -->
<documentation>
  Review request from ${requester}
  Amount: $${amount}
</documentation>

<!-- In conditions -->
<conditionExpression>${amount > 5000}</conditionExpression>

<!-- In assignments -->
<userTask flowable:assignee="${requester}">
```
**What happens**: These reference **existing process variables**.

### 3. Script Tasks (Setting Variables)
```xml
<scriptTask id="calculateTax" scriptFormat="javascript">
  <script>
    // These become process variables
    var subtotal = execution.getVariable('amount');
    var tax = subtotal * 0.08;
    execution.setVariable('tax', tax);
    execution.setVariable('total', subtotal + tax);
    
    // This would be execution variable (rarely used)
    execution.setVariableLocal('tempCalc', tax * 2);
  </script>
</scriptTask>
```

### 4. Service Tasks (Java Code)
```xml
<serviceTask id="processPayment" flowable:delegateExpression="${paymentService}"/>
```
```java
public class PaymentService implements JavaDelegate {
    public void execute(DelegateExecution execution) {
        // Process variable
        execution.setVariable("paymentStatus", "completed");
        
        // Execution variable (local to this branch)
        execution.setVariableLocal("retryCount", 0);
        
        // Task variable would be set differently (via TaskService)
    }
}
```

### 5. Start Event Variables
```xml
<startEvent id="start">
  <extensionElements>
    <flowable:formProperty id="priority" name="Priority" type="enum" defaultValue="normal">
      <flowable:value id="low" name="Low Priority" />
      <flowable:value id="normal" name="Normal Priority" />
      <flowable:value id="high" name="High Priority" />
    </flowable:formProperty>
  </extensionElements>
</startEvent>
```
**What happens**: These become **process variables** when process starts.

### 6. Listeners (Event-based Variables)
```xml
<userTask id="review">
  <extensionElements>
    <flowable:taskListener event="create" class="com.example.TaskCreateListener"/>
    <flowable:taskListener event="complete" class="com.example.TaskCompleteListener"/>
  </extensionElements>
</userTask>
```
```java
public class TaskCompleteListener implements TaskListener {
    public void notify(DelegateTask task) {
        // Task variable (only exists during task)
        task.setVariableLocal("completedAt", new Date());
        
        // Process variable
        task.setVariable("reviewComplete", true);
    }
}
```

## Default Behavior

### In BPMN XML:
1. **Form properties** → Become **process variables** when task completes
2. **${variable}** references → Expect **process variables**
3. **Script variables** → Become **process variables** unless you use `setVariableLocal`

### The Simple Rule:
- **Everything is a process variable by default**
- You have to explicitly make something else

## How to Control Variable Scope

### 1. In BPMN (Limited Control)
```xml
<!-- You can't directly specify scope in BPMN XML -->
<!-- But you can use scripts or Java code to control it -->
```

### 2. In Java/Scripts
```java
// Process variable (default)
execution.setVariable("name", value);

// Execution variable (branch-specific)
execution.setVariableLocal("name", value);

// Task variable (via TaskService)
taskService.setVariableLocal(taskId, "name", value);
```

### 3. Via API Calls
```json
// Completing a task
POST /tasks/{id}/complete
{
  "variables": [
    {"name": "decision", "value": "approved", "scope": "global"},  // Process
    {"name": "notes", "value": "temp", "scope": "local"}          // Task
  ]
}
```

## Variable Lifecycle Examples

### Example 1: Form Input
```xml
<flowable:formProperty id="approvalDecision" type="string"/>
```
- User fills form → Becomes process variable → Available everywhere

### Example 2: Parallel Gateway
```
     ├→ Branch A (execution 1)
Start┤
     └→ Branch B (execution 2)
```
- Variables set in Branch A with `setVariableLocal` → Only in Branch A
- Variables set with `setVariable` → Visible in both branches

### Example 3: Sub-Process
```xml
<subProcess id="reviewSubProcess">
  <!-- Variables here can be scoped to subprocess -->
</subProcess>
```

## Best Practices

1. **Assume Process Variables**: Unless you need something special
2. **Document Your Intent**: Use comments in BPMN
3. **Be Explicit in Code**: When scope matters, use Java/scripts
4. **Keep It Simple**: 95% of the time, process variables are what you want

## Summary

**You can't tell variable scope from BPMN XML alone!** 

The scope depends on:
- How the variable is set (form, script, Java)
- Which API method is used
- Where in the process it's created

BPMN XML shows WHERE variables are used, not WHAT TYPE they are.