# Onboarding Guide for New Developers

Welcome to the Flowable Wrapper project! This guide will walk you through the core concepts, with a special focus on the different types of "metadata" used throughout the system, to get you up and running quickly.

## 1. Quick Start: Getting the System Running

1.  **Prerequisites:** Ensure you have Docker and Docker Compose installed.
2.  **Navigate:** Open a terminal and go to the `custom-flowable/docker` directory.
3.  **Launch:** Run the command `docker-compose up -d`.
4.  **Verify:** The entire stack (PostgreSQL, Cerbos, and the Flowable Wrapper app) is now running. You can access the API documentation (Swagger UI) at `http://localhost:8090/swagger-ui.html` to explore the endpoints.

## 2. The Core Concept: Metadata-Driven Workflows

The key to understanding this project is to grasp that it is **metadata-driven**. The behavior of workflows, queues, and permissions is not hardcoded but is defined by data stored in the database. Let's break down the different kinds of metadata you will encounter.

### Type 1: Workflow Metadata (`workflow_metadata` table)

This is the most important metadata. It's the high-level configuration for an entire workflow *type*.

-   **What it is:** A record that defines a workflow like "VacationRequest" or "ExpenseApproval".
-   **Key Fields:**
    -   `process_definition_key`: The ID that links this record to the BPMN diagram.
    -   `business_app_name`: The business application this workflow belongs to.
    -   `candidate_group_mappings`: **(User-provided metadata)**. This is a simple JSON map you provide when you first register the workflow. It tells the system how to translate the technical `candidateGroup` from the BPMN file into a friendly `queueName`. Example: `{"managers": "manager-queue", "hr_team": "hr-processing-queue"}`.
    -   `task_queue_mappings`: **(System-generated metadata)**. After you deploy a BPMN file, the system inspects it and generates this detailed JSON map. It links every single User Task ID from the diagram to a specific queue, creating a definitive routing guide.

### Type 2: Task Metadata (`queue_tasks` table)

This is the metadata associated with a single, live, running task.

-   **What it is:** A row in the `queue_tasks` table representing an item in a user's work inbox.
-   **Key Fields:**
    -   `queue_name`: The name of the queue this task currently resides in.
    -   `status`: The task's current state (`OPEN`, `CLAIMED`, `COMPLETED`).
    -   `assignee`: The user who has claimed the task.
    -   `task_data` (JSONB): A flexible field to hold any extra, non-critical information about the task, such as the original description or due date from Flowable.

### Type 3: User and Role Metadata (`users`, `business_app_roles` tables)

This metadata defines the users and their permissions, which is critical for the entitlement system.

-   **What it is:** The attributes and roles associated with a user.
-   **Key Fields:**
    -   `users.attributes` (JSONB): Stores flexible, key-value data about a user that can be used in authorization policies. For example: `{"department": "Finance", "approval_limit": 5000}`.
    -   `business_app_roles.metadata` (JSONB): Can store extra information about a specific role within a business application.

### Type 4: Cerbos Policy Metadata (YAML Files)

This is the metadata that defines the authorization rules.

-   **What it is:** The collection of `.yaml` files inside the `custom-flowable/docker/cerbos/policies` directory.
-   **How it works:** These files contain the rules that Cerbos uses to make decisions. When the application asks for permission, it sends the user metadata (from Type 3) and the resource metadata (from Type 1 and 2) to Cerbos. Cerbos then uses its policy metadata to determine if the action is allowed.

## 3. Your First Workflow: A Step-by-Step Guide

1.  **Create a BPMN Diagram:** Create a simple BPMN 2.0 XML file. Define at least one `userTask` and assign it a `flowable:candidateGroups` (e.g., `flowable:candidateGroups="developers"`).

2.  **Register the Workflow:** Use the Swagger UI to call `POST /api/workflow-metadata/register`. In the request body, provide:
    -   The `processDefinitionKey` from your BPMN file.
    -   The `businessAppName` you want to associate it with.
    -   The `candidateGroupMappings`, where you map the group from your BPMN file to a queue name. Example: `{"developers": "developer-review-queue"}`.

3.  **Deploy the Workflow:** Call `POST /api/workflow-metadata/deploy`. Provide the `processDefinitionKey` and the full XML content of your BPMN file.
    -   At this point, the system will generate the `task_queue_mappings` automatically.

4.  **Start an Instance:** Call `POST /api/{businessAppName}/process-instances/start` to create a running instance of your workflow.

5.  **Check the Queue:** Call `GET /api/tasks/queue/{queueName}` (using the queue name you defined in step 2). You should see your new task waiting in the queue.

By following these steps, you have successfully participated in the full lifecycle of a workflow, from definition to execution, and have interacted with the core metadata concepts of the system.
