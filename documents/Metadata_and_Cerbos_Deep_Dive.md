# Metadata and Cerbos: A Deep Dive

This document provides a detailed analysis of how different types of metadata within the Flowable Wrapper are collected and transformed into a rich authorization context for Cerbos. Understanding this flow is critical to writing effective authorization policies.

## 1. The Goal: Building a Rich Authorization Context

Every time an authorization check is made, the `CerbosService` acts as an orchestrator to build a comprehensive request object. This object contains two main parts: the `Principal` (the "Who") and the `Resource` (the "What"). The effectiveness of your Cerbos policies is directly proportional to the quality and depth of the metadata you provide in these objects.

## 2. The `Principal`: Who is the User?

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

## 3. The `Resource`: What is being acted on?

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

## 4. How the Cerbos Policy Folder Structure Relates

The `Resource.kind` field in the authorization request is dynamically generated, often as `businessAppName::processDefinitionKey`. This `kind` directly determines which policy file Cerbos will use.

- A request with `"kind": "Procurement::purchaseOrderApproval"` would cause Cerbos to look for a policy file named `purchaseOrderApproval.yaml` inside a `resource_policies/Procurement/` directory.
- A request with `"kind": "workflow-management"` maps to the `resource_policies/workflow/default/workflow-management-resource.yaml` policy.

This structure ensures that policies are modular, organized by business application and resource type, making the entire system easy to navigate and manage as it grows.
