# Parallel Execution and Variables - Complete Guide

## The Problem Context

When you have parallel branches in a workflow (using parallel gateways), multiple tasks execute simultaneously. This raises a question: **How do you handle variables when multiple branches might need to store similar data?**

### Example Scenario
Imagine a document approval process where Legal, Finance, and Technical teams review in parallel:
- Each team needs to store their "approval decision"
- Each team needs to store their "comments"
- Each team needs to store their "reviewer name"

### The Concern
If all branches use the same variable names:
- Will they overwrite each other?
- Do you need special "local" variables?
- Do you need Java code to handle this?

## The Key Question: Do You Need Local Variables?

**Short Answer: NO!** You can use different variable names instead of local variables.

## Two Approaches for Branch-Specific Data

### Approach 1: Using Local Variables (Requires Java/Script)
```java
// Branch A
execution.setVariableLocal("reviewNotes", "Legal concerns found");

// Branch B  
execution.setVariableLocal("reviewNotes", "Finance approved");
// No conflict! Each branch has its own "reviewNotes"
```

### Approach 2: Using Different Variable Names (Works in BPMN!)
```xml
<!-- Branch A: Legal Review -->
<userTask id="legalReview">
  <extensionElements>
    <flowable:formProperty id="legalReviewNotes" type="string"/>
    <flowable:formProperty id="legalDecision" type="boolean"/>
  </extensionElements>
</userTask>

<!-- Branch B: Finance Review -->
<userTask id="financeReview">
  <extensionElements>
    <flowable:formProperty id="financeReviewNotes" type="string"/>
    <flowable:formProperty id="financeDecision" type="boolean"/>
  </extensionElements>
</userTask>
```

## Why Different Variable Names is Often Better

### 1. Works with Pure BPMN (No Java Required!)
```xml
<parallelGateway id="split"/>

<!-- Each branch uses unique variable names -->
<userTask id="legalReview" name="Legal Review">
  <extensionElements>
    <!-- These become process variables with unique names -->
    <flowable:formProperty id="legalStatus" type="enum">
      <flowable:value id="approved" name="Approved"/>
      <flowable:value id="rejected" name="Rejected"/>
    </flowable:formProperty>
    <flowable:formProperty id="legalComments" type="string"/>
    <flowable:formProperty id="legalReviewer" type="string"/>
  </extensionElements>
</userTask>

<userTask id="financeReview" name="Finance Review">
  <extensionElements>
    <!-- Different names = no conflicts -->
    <flowable:formProperty id="financeStatus" type="enum">
      <flowable:value id="approved" name="Approved"/>
      <flowable:value id="rejected" name="Rejected"/>
    </flowable:formProperty>
    <flowable:formProperty id="financeComments" type="string"/>
    <flowable:formProperty id="financeReviewer" type="string"/>
  </extensionElements>
</userTask>
```

### 2. Easier to Debug
```javascript
// After parallel execution, you can see all values:
{
  "legalStatus": "approved",
  "legalComments": "No issues found",
  "legalReviewer": "john.doe",
  "financeStatus": "rejected",
  "financeComments": "Over budget",
  "financeReviewer": "jane.smith"
}
// vs local variables which disappear after merge!
```

### 3. Better for Reporting
```xml
<!-- Final task can reference all branch results -->
<userTask id="finalDecision">
  <documentation>
    Legal Review: ${legalStatus} - ${legalComments}
    Finance Review: ${financeStatus} - ${financeComments}
    Technical Review: ${technicalStatus} - ${technicalComments}
  </documentation>
</userTask>
```

## When Would You Actually Need Local Variables?

### 1. Temporary Calculations
```java
// Only needed during branch execution
execution.setVariableLocal("tempCalculation", complexMath());
execution.setVariableLocal("intermediateResult", step1Result);
// Don't want to clutter process with temporary data
```

### 2. Large Data That's Not Needed Later
```java
// Branch downloads large file for processing
execution.setVariableLocal("largeFileContent", fileData); // 10MB
// Process it and only store result as process variable
execution.setVariable("processedResult", processFile(fileData)); // 10KB
```

### 3. Security/Privacy
```java
// Sensitive data only for this branch
execution.setVariableLocal("internalAuditNotes", "Confidential findings");
// Public result
execution.setVariable("auditPassed", true);
```

## Real Example: Parallel Approval Without Java

```xml
<process id="parallelApprovalNoJava" name="Parallel Approval - BPMN Only">
  
  <startEvent id="start">
    <extensionElements>
      <flowable:formProperty id="documentId" type="string"/>
      <flowable:formProperty id="requester" type="string"/>
    </extensionElements>
  </startEvent>
  
  <!-- Split into 3 parallel branches -->
  <parallelGateway id="split" name="Start Reviews"/>
  
  <!-- Branch 1: Legal -->
  <userTask id="legalTask" name="Legal Review" flowable:candidateGroups="legal">
    <extensionElements>
      <flowable:formProperty id="legalApproved" type="boolean" required="true"/>
      <flowable:formProperty id="legalNotes" type="string"/>
      <flowable:formProperty id="legalRiskLevel" type="enum">
        <flowable:value id="low" name="Low Risk"/>
        <flowable:value id="medium" name="Medium Risk"/>
        <flowable:value id="high" name="High Risk"/>
      </flowable:formProperty>
    </extensionElements>
  </userTask>
  
  <!-- Branch 2: Finance -->
  <userTask id="financeTask" name="Finance Review" flowable:candidateGroups="finance">
    <extensionElements>
      <flowable:formProperty id="financeApproved" type="boolean" required="true"/>
      <flowable:formProperty id="financeNotes" type="string"/>
      <flowable:formProperty id="budgetImpact" type="long"/>
    </extensionElements>
  </userTask>
  
  <!-- Branch 3: Technical -->
  <userTask id="technicalTask" name="Technical Review" flowable:candidateGroups="technical">
    <extensionElements>
      <flowable:formProperty id="technicalApproved" type="boolean" required="true"/>
      <flowable:formProperty id="technicalNotes" type="string"/>
      <flowable:formProperty id="complexityScore" type="long"/>
    </extensionElements>
  </userTask>
  
  <!-- Merge -->
  <parallelGateway id="merge" name="Merge Reviews"/>
  
  <!-- Final decision can see all variables -->
  <userTask id="finalTask" name="Final Decision">
    <documentation>
      Legal: ${legalApproved} (Risk: ${legalRiskLevel})
      Finance: ${financeApproved} (Budget Impact: $${budgetImpact})
      Technical: ${technicalApproved} (Complexity: ${complexityScore})
    </documentation>
  </userTask>
  
  <!-- Flows -->
  <sequenceFlow sourceRef="start" targetRef="split"/>
  <sequenceFlow sourceRef="split" targetRef="legalTask"/>
  <sequenceFlow sourceRef="split" targetRef="financeTask"/>
  <sequenceFlow sourceRef="split" targetRef="technicalTask"/>
  <sequenceFlow sourceRef="legalTask" targetRef="merge"/>
  <sequenceFlow sourceRef="financeTask" targetRef="merge"/>
  <sequenceFlow sourceRef="technicalTask" targetRef="merge"/>
  <sequenceFlow sourceRef="merge" targetRef="finalTask"/>
</process>
```

## Best Practices

### 1. Default to Different Variable Names
- Easier to implement
- No Java required
- Better visibility
- Simpler debugging

### 2. Use Naming Conventions
```
Branch-specific: [branch]_[variable]
- legal_approval
- finance_approval
- technical_approval

Shared data: [entity]_[attribute]
- document_id
- request_status
```

### 3. Only Use Local Variables When
- You have Java/Script tasks AND
- Data is truly temporary AND
- You want to hide it from other branches/history

## Common Misconception

**"I need local variables for parallel branches"** ❌

**Reality**: You need unique variable names! ✅

## The Bottom Line

For 99% of cases with parallel branches:
1. Use different variable names per branch
2. Keep it simple with pure BPMN
3. All data visible in process history
4. No Java required

Local variables are an advanced feature for specific scenarios, not a requirement for parallel execution!