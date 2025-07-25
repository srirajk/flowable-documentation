# Metadata and Cerbos: A Deep Dive

This document provides a detailed analysis of how different types of metadata within the Flowable Wrapper are collected and transformed into a rich authorization context for Cerbos. Understanding this flow is critical to writing effective authorization policies.

## 1. Understanding the Three Levels of Metadata

Before diving into Cerbos integration, it's essential to understand the three distinct levels of metadata that power our authorization system:

### Business Application Metadata
**What it is**: High-level configuration about business applications and their capabilities.

**Source**: `business_applications` table and related configuration
**Scope**: Applies to entire business domains
**Examples**:
- Business App: "Sanctions-Management", "Procurement", "HR-Workflows"  
- Description: "Level 1 and Level 2 sanctions case management workflow"
- Owner: "compliance-team", "finance-team"
- Active status, version information

**Purpose**: Defines the business context and boundaries for all workflows within an application.

```json
{
  "businessAppMetadata": {
    "id": 1,
    "name": "Sanctions-Management", 
    "description": "Level 1 and Level 2 sanctions case management workflow",
    "owner": "compliance-team",
    "version": "1.0",
    "isActive": true
  }
}
```

### Workflow Metadata  
**What it is**: Configuration about specific workflow types and their task routing rules.

**Source**: `workflow_metadata` table and task queue mappings
**Scope**: Applies to all instances of a specific workflow type
**Examples**:
- Process Definition Key: "sanctionsCaseManagement", "expenseApproval"
- Task Queue Mappings: Which tasks go to which queues
- Candidate Group Mappings: Role-to-queue assignments
- SLA definitions, categories, deployment information

**Purpose**: Defines how tasks are routed, what roles can access them, and workflow-level policies.

```json
{
  "workflowMetadata": {
    "id": 1,
    "processDefinitionKey": "sanctionsCaseManagement",
    "taskQueueMappings": [
      {
        "taskId": "l1_maker_review_task",
        "queue": "level1-queue", 
        "candidateGroups": ["level1-maker"]
      }
    ],
    "version": 1,
    "deploymentId": "abc-123"
  }
}
```

### Process Instance Metadata
**What it is**: Live, dynamic data about a specific running workflow instance.

**Source**: Flowable engine variables, task states, execution context
**Scope**: Applies to one specific workflow execution
**Examples**:
- Process Variables: caseId, customerName, amount, riskLevel
- Task States: Who worked on what tasks, when they were completed
- Current Task: Which task is active, who it's assigned to
- Execution State: Is process active, suspended, completed

**Purpose**: Provides the real-time business data and context for authorization decisions.

```json
{
  "processInstanceId": "12b23432-6989-11f0-a69a-8a52b726ab7d",
  "processVariables": {
    "caseId": "SC002",
    "customerName": "Jane Smith", 
    "amount": 250000.0,
    "riskLevel": "HIGH"
  },
  "currentTask": {
    "taskDefinitionKey": "l1_maker_review_task",
    "queue": "level1-queue",
    "assignee": "us-l1-operator-1"
  },
  "taskStates": {
    "l1_maker_review_task": {
      "assignee": "us-l1-operator-1",
      "status": "CLAIMED",
      "createdAt": "2025-07-25T18:56:29Z"
    }
  }
}
```

### How These Three Levels Work Together in Authorization

**Hierarchical Context**: Business App → Workflow → Process Instance
- **Business App**: "Am I authorized to work in Sanctions-Management?"
- **Workflow**: "Can I access sanctionsCaseManagement workflows?" 
- **Process Instance**: "Can I claim this specific task for customer Jane Smith?"

**Authorization Granularity**: Each level provides different policy controls
- **Business App Level**: Department access, regional restrictions
- **Workflow Level**: Role-based task access, queue permissions
- **Process Instance Level**: Four-Eyes enforcement, business rule validation

### Critical Distinction: Entitlements vs Business Rules

**Entitlements (User-Based Authorization)**:
- **What it is**: "Can this user perform this action?" 
- **Examples**: Role-based access, queue permissions, regional restrictions
- **Policy Focus**: User attributes, roles, departmental access
- **Cerbos Implementation**: Principal-based conditions using `P.attr.*`

**Business Rules (Workflow Transition Logic)**:
- **What it is**: "Should this workflow transition happen based on data?"
- **Examples**: Amount thresholds, risk level escalations, SLA violations
- **Policy Focus**: Process variables, business data validation
- **Cerbos Implementation**: Resource-based conditions using `R.attr.processVariables.*`

**Example - Entitlement Policy**:
```yaml
# User-based: Can user claim tasks in this queue?
- actions: ["claim_task"]
  roles: ["level1-operator"] 
  condition:
    match:
      all:
        - expr: request.resource.attr.currentTask.queue in request.principal.attr.queues
        - expr: request.resource.attr.businessApp in request.principal.attr.businessApps
```

**Example - Business Rule Policy**:
```yaml
# Business logic: High-risk cases require L2 escalation regardless of user
- actions: ["complete_task"]
  effect: EFFECT_DENY
  condition:
    match:
      all:
        - expr: request.resource.attr.processVariables.riskLevel == "HIGH"
        - expr: request.resource.attr.processVariables.amount > 100000
        - expr: request.resource.attr.currentTask.taskDefinitionKey == "l1_final_decision"
  # This prevents L1 users from closing high-risk cases, forces L2 escalation
```

**Why This Separation Matters**:
- **Entitlements**: Change when users change roles/departments
- **Business Rules**: Change when business policies change (compliance, risk management)
- **Policy Maintenance**: Different teams manage different policy types
- **Audit Requirements**: Entitlements vs business rule violations have different compliance implications

## 2. The Goal: Building a Rich Authorization Context

Every time an authorization check is made, the `CerbosService` acts as an orchestrator to build a comprehensive request object. This object contains two main parts: the `Principal` (the "Who") and the `Resource` (the "What"). The effectiveness of your Cerbos policies is directly proportional to the quality and depth of the metadata you provide in these objects.

## 3. The `Principal`: Who is the User?

The `Principal` object represents the user performing the action. It's constructed by the `CerbosService` by fetching data from the user management tables.

**Source of Metadata:**
- `users` table: Provides the user's ID and their `attributes` (e.g., department, approval limits).
- `user_business_app_roles`, `business_app_roles`, `business_applications` tables: Provide the user's assigned roles for the specific business application they are interacting with.

**Example `Principal` Object Sent to Cerbos:**

```json
{
  "id": "sally.jones",
  "roles": ["manager", "approver"],
  "attr": {
    "department": "Sales",
    "region": "EMEA",
    "approval_limit": 10000,
    "businessApps": ["Salesforce", "Procurement"]
  }
}
```

**Impact on Cerbos Policies:**

This rich `Principal` metadata allows you to write powerful, attribute-based policies. The `P` variable in a Cerbos policy refers to this Principal object.

- **Role-Based Access Control (RBAC):**
  ```yaml
  # Allows the action if the user has the 'approver' role.
  - actions: ["approve"]
    effect: EFFECT_ALLOW
    roles:
      - "approver"
  ```

- **Attribute-Based Access Control (ABAC):**
  ```yaml
  # Allows approval only if the user's limit is sufficient.
  condition:
    match:
      expr: "P.attr.approval_limit >= R.attr.processVariables.amount"
  ```

## 4. The `Resource`: What is being acted on?

The `Resource` object is where the system's flexibility truly shines. It represents the entity being accessed (a workflow, a task, a queue) and is populated with a deep set of metadata from multiple sources.

**Source of Metadata:**
- `workflow_metadata` table: Provides high-level configuration about the workflow type.
- `queue_tasks` table: Provides the current state of the specific task in the queue system (e.g., its assignee).
- **Flowable Engine API:** Provides live, real-time data, most importantly the `processVariables`.

**Example `Resource` Object Sent to Cerbos (for a `complete_task` action):**

```json
{
  "kind": "Procurement::purchaseOrderApproval", // Dynamic kind: businessApp::processDefinitionKey
  "id": "instance-12345",
  "attr": {
    "businessApp": "Procurement",
    "processDefinitionKey": "purchaseOrderApproval",
    "workflowMetadata": {
      "category": "finance",
      "sla": "48 hours"
    },
    "processVariables": {
      "orderId": "PO-2024-001",
      "amount": 7500,
      "requester": "bob.smith"
    },
    "currentTask": {
      "taskDefinitionKey": "managerApproval",
      "queue": "manager-queue",
      "assignee": "sally.jones"
    },
    "taskStates": {
      "submitterReview": {
        "assignee": "bob.smith",
        "status": "COMPLETED"
      }
    }
  }
}
```

**Impact of `Resource` Metadata on Cerbos Policies:**

The `R` variable in a Cerbos policy refers to this Resource object. This is where you can enforce very fine-grained business logic.

- **Impact of `processVariables`:** This is the most powerful feature. It allows your policies to be based on the **live data** inside the workflow.
  ```yaml
  # Deny if the expense amount is over $5000 and for the 'Marketing' department.
  - actions: ["approve"]
    effect: EFFECT_DENY
    condition:
      match:
        all:
          - expr: "R.attr.processVariables.amount > 5000"
          - expr: "R.attr.processVariables.department == 'Marketing'"
  ```

- **Impact of `taskStates` (for Four-Eyes Principle):** The `taskStates` attribute is a map of previously completed tasks in the instance. This is essential for enforcing segregation of duties.
  ```yaml
  # Deny approval if the current user was the assignee of the 'L1_Review' task.
  - actions: ["L2_Approve"]
    effect: EFFECT_DENY
    condition:
      match:
        expr: "P.id == R.attr.taskStates.L1_Review.assignee"
  ```

- **Impact of `currentTask`:** This provides context about the specific task being acted upon.
  ```yaml
  # Only allow a user to claim a task if it is unassigned.
  - actions: ["claim_task"]
    effect: EFFECT_ALLOW
    condition:
      match:
        expr: "R.attr.currentTask.assignee == null"
  ```

- **Impact of `createRequest` (for `start_workflow_instance`):** When starting a workflow, the initial variables are not yet in `processVariables`. They are passed in a special `createRequest` attribute, allowing policies to validate data *before* a workflow is even created.
  ```yaml
  # Deny starting a workflow if the initial request amount exceeds the user's spending limit.
  - actions: ["start_workflow_instance"]
    effect: EFFECT_DENY
    condition:
      match:
        expr: "R.attr.createRequest.amount > P.attr.spending_limit"
  ```

## 5. How the Cerbos Policy Folder Structure Relates

The `Resource.kind` field in the authorization request is dynamically generated, often as `businessAppName::processDefinitionKey`. This `kind` directly determines which policy file Cerbos will use.

- A request with `"kind": "Procurement::purchaseOrderApproval"` would cause Cerbos to look for a policy file named `purchaseOrderApproval.yaml` inside a `resource_policies/Procurement/` directory.
- A request with `"kind": "workflow-management"` maps to the `resource_policies/workflow/default/workflow-management-resource.yaml` policy.

This structure ensures that policies are modular, organized by business application and resource type, making the entire system easy to navigate and manage as it grows.
