# Validation Pattern Examples

## Example 1: Expense Report Workflow

### BPMN Definition
```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://www.flowable.org/processdef">
             
  <process id="expenseApproval" name="Expense Approval Process">
    
    <!-- Employee submits expense -->
    <startEvent id="start" />
    
    <sequenceFlow sourceRef="start" targetRef="submitExpense" />
    
    <userTask id="submitExpense" name="Submit Expense Report">
      <documentation>Submit your expense report with receipts</documentation>
      <extensionElements>
        <flowable:candidateGroups>employees</flowable:candidateGroups>
        <flowable:formProperty id="amount" name="Total Amount" type="long" required="true"/>
        <flowable:formProperty id="description" name="Description" type="string" required="true"/>
        <flowable:formProperty id="hasReceipt" name="Receipt Attached?" type="boolean" required="true"/>
      </extensionElements>
    </userTask>
    
    <sequenceFlow sourceRef="submitExpense" targetRef="validateExpense" />
    
    <!-- Automatic validation -->
    <serviceTask id="validateExpense" name="Validate Expense"
                 flowable:delegateExpression="${expenseValidator}" />
    
    <sequenceFlow sourceRef="validateExpense" targetRef="expenseValidationGateway" />
    
    <!-- Validation gateway -->
    <exclusiveGateway id="expenseValidationGateway" name="Is expense valid?" />
    
    <!-- If valid, go to manager -->
    <sequenceFlow sourceRef="expenseValidationGateway" targetRef="managerApproval">
      <conditionExpression>${submitExpenseValid == true}</conditionExpression>
    </sequenceFlow>
    
    <!-- If invalid, loop back -->
    <sequenceFlow sourceRef="expenseValidationGateway" targetRef="submitExpense">
      <conditionExpression>${submitExpenseValid == false}</conditionExpression>
    </sequenceFlow>
    
    <!-- Manager approval -->
    <userTask id="managerApproval" name="Approve Expense">
      <documentation>
        Employee: ${employeeName}
        Amount: $${amount}
        Description: ${description}
      </documentation>
      <extensionElements>
        <flowable:candidateGroups>managers</flowable:candidateGroups>
      </extensionElements>
    </userTask>
    
    <!-- ... rest of the process ... -->
    
  </process>
</definitions>
```

### Validator Implementation
```java
@Component("expenseValidator")
public class ExpenseValidator implements JavaDelegate {
    
    @Override
    public void execute(DelegateExecution execution) {
        Long amount = (Long) execution.getVariable("amount");
        Boolean hasReceipt = (Boolean) execution.getVariable("hasReceipt");
        String description = (String) execution.getVariable("description");
        
        List<String> errors = new ArrayList<>();
        boolean isValid = true;
        
        // Validation rules
        if (amount > 5000) {
            errors.add("Expenses over $5000 require director approval");
            isValid = false;
        }
        
        if (amount > 100 && !Boolean.TRUE.equals(hasReceipt)) {
            errors.add("Receipt required for expenses over $100");
            isValid = false;
        }
        
        if (description == null || description.trim().length() < 10) {
            errors.add("Description must be at least 10 characters");
            isValid = false;
        }
        
        // Set required variables
        execution.setVariable("submitExpenseValid", isValid);
        execution.setVariable("submitExpenseValidationError", errors);
        execution.setVariable("submitExpenseAttemptCount", 
            ((Integer) execution.getVariable("submitExpenseAttemptCount") ?? 0) + 1);
    }
}
```

## Example 2: Parallel Maker-Checker Pattern

### BPMN Definition
```xml
<process id="parallelMakerChecker" name="Parallel Maker-Checker Process">
  
  <!-- Fork into parallel paths -->
  <parallelGateway id="fork" />
  
  <!-- Path 1: Document A -->
  <sequenceFlow sourceRef="fork" targetRef="prepareDocumentA" />
  
  <userTask id="prepareDocumentA" name="Prepare Document A">
    <extensionElements>
      <flowable:candidateGroups>documentPrepTeam</flowable:candidateGroups>
    </extensionElements>
  </userTask>
  
  <serviceTask id="validateDocumentA" name="Validate Document A"
               flowable:delegateExpression="${documentValidator}" />
  
  <exclusiveGateway id="docAValidationGateway" />
  
  <sequenceFlow sourceRef="docAValidationGateway" targetRef="join">
    <conditionExpression>${prepareDocumentAValid == true}</conditionExpression>
  </sequenceFlow>
  
  <sequenceFlow sourceRef="docAValidationGateway" targetRef="prepareDocumentA">
    <conditionExpression>${prepareDocumentAValid == false}</conditionExpression>
  </sequenceFlow>
  
  <!-- Path 2: Document B (similar structure) -->
  <sequenceFlow sourceRef="fork" targetRef="prepareDocumentB" />
  <!-- ... similar pattern ... -->
  
  <!-- Join when both valid -->
  <parallelGateway id="join" />
  
  <!-- Final validation after both complete -->
  <serviceTask id="finalValidation" name="Cross-Document Validation"
               flowable:delegateExpression="${crossDocumentValidator}" />
  
</process>
```

## Example 3: Multi-Level Validation

### Scenario
- Level 1: Basic data validation (format, required fields)
- Level 2: Business rule validation (limits, policies)
- Level 3: External system validation (credit check, inventory)

### BPMN Pattern
```xml
<process id="multiLevelValidation">
  
  <userTask id="submitApplication" name="Submit Application">
    <extensionElements>
      <flowable:candidateGroups>applicants</flowable:candidateGroups>
    </extensionElements>
  </userTask>
  
  <!-- Level 1: Basic validation -->
  <serviceTask id="basicValidation" name="Basic Validation"
               flowable:class="com.flowable.validators.BasicValidator" />
  
  <exclusiveGateway id="basicValidationGateway" />
  
  <!-- If basic validation fails, return immediately -->
  <sequenceFlow sourceRef="basicValidationGateway" targetRef="submitApplication">
    <conditionExpression>${basicValid == false}</conditionExpression>
  </sequenceFlow>
  
  <!-- Level 2: Business rules (only if basic passed) -->
  <serviceTask id="businessRuleValidation" name="Business Rule Validation"
               flowable:class="com.flowable.validators.BusinessRuleValidator" />
  
  <exclusiveGateway id="businessRuleGateway" />
  
  <!-- If business rules fail, return with detailed reasons -->
  <sequenceFlow sourceRef="businessRuleGateway" targetRef="submitApplication">
    <conditionExpression>${businessRulesValid == false}</conditionExpression>
  </sequenceFlow>
  
  <!-- Level 3: External validation (only if L1 & L2 passed) -->
  <serviceTask id="externalValidation" name="External System Validation"
               flowable:class="com.flowable.validators.ExternalValidator" />
  
  <!-- ... continue ... -->
  
</process>
```

## Common Validation Scenarios

### 1. Amount Limits
```java
if (amount > maxLimit) {
    errors.add(String.format("Amount $%.2f exceeds limit of $%.2f", 
        amount, maxLimit));
}
```

### 2. Required Attachments
```java
if (amount > 100 && attachments.isEmpty()) {
    errors.add("Attachments required for amounts over $100");
}
```

### 3. Date Validations
```java
if (startDate.isAfter(endDate)) {
    errors.add("Start date must be before end date");
}
```

### 4. Cross-Field Validation
```java
if ("URGENT".equals(priority) && dueDate.isAfter(LocalDate.now().plusDays(7))) {
    errors.add("Urgent requests must have due date within 7 days");
}
```

### 5. External System Validation
```java
try {
    boolean creditCheckPassed = creditService.checkCredit(customerId, amount);
    if (!creditCheckPassed) {
        errors.add("Credit check failed. Please contact finance department.");
    }
} catch (Exception e) {
    errors.add("Unable to verify credit. Please try again later.");
}
```

## Testing Checklist

For each validation pattern, test:

- [ ] Valid data passes through
- [ ] Each validation rule triggers correctly
- [ ] Error messages are clear and actionable
- [ ] Attempt counter increments
- [ ] Multiple validation errors are captured
- [ ] Task appears in correct queue after rejection
- [ ] Process variables are preserved on retry
- [ ] Parallel paths validate independently
- [ ] Gateway routing works correctly
- [ ] Edge cases (null values, extreme values)