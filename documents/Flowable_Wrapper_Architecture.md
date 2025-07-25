# The Flowable Wrapper: A Deep Dive into the Architecture

This document provides a detailed explanation of the Flowable Wrapper application, its architecture, how it achieves scalability, and the lifecycle of workflows and tasks within the system.

## 1. Core Philosophy: Simplicity Through Abstraction

The primary goal of this wrapper is to abstract away the inherent complexity of the Flowable BPMN engine. It achieves this by providing a clean, RESTful, and business-oriented API that is much easier to work with than Flowable's native Java API.

The central innovation is the **Queue-Based Task Model**. Instead of forcing developers and front-end applications to understand Flowable's concept of "candidate groups," the wrapper presents tasks in simple, named queues (e.g., `manager-queue`, `finance-queue`), which is a more intuitive "inbox" paradigm.

## 2. The Data Model: The Foundation of the Queue Abstraction

The system's functionality is built on a clever database schema that creates a query-optimized layer on top of Flowable.

-   **`workflow_metadata` Table:** This is the master configuration table for all workflow types. For any given workflow (e.g., "purchaseOrderApproval"), this table stores:
    -   A link to its parent **Business Application**.
    -   The crucial **`candidate_group_mappings`**: A simple JSON object provided at registration that maps technical group names to business-friendly queue names (e.g., `{"managers": "manager-queue"}`).
    -   The auto-generated **`task_queue_mappings`**: A detailed JSON array, created during deployment, that serves as a definitive routing map, linking every specific User Task ID from the BPMN diagram to its designated queue.

-   **`queue_tasks` Table:** This table is a **denormalized read-model** of active tasks. It acts as the live, queryable "inbox" for all queues. When a front-end needs to display tasks, it makes a simple and fast query to this table instead of the complex and slower Flowable task tables. It stores the `queue_name`, `status` (`OPEN`, `CLAIMED`, `COMPLETED`), `assignee`, and other relevant task information.

## 3. The Workflow Lifecycle: From Concept to Execution

A workflow progresses through a two-stage lifecycle, managed by the `WorkflowMetadataService`.

#### Stage 1: Registration (The "What")

-   **Action:** A developer registers a new workflow definition via the API.
-   **Result:** A new record is created in the `workflow_metadata` table. The system now knows conceptually that this workflow exists and how its candidate groups map to queues. The BPMN file itself has not yet been processed.

#### Stage 2: Deployment (The "How")

-   **Action:** The developer deploys the BPMN 2.0 XML file via the API.
-   **Result:**
    1.  The XML is deployed to the Flowable engine.
    2.  The wrapper then immediately uses the Flowable API to **introspect the deployed model**.
    3.  It programmatically iterates through every `UserTask` in the diagram, checks its `candidateGroups`, and uses the mappings defined during registration to build the definitive `task_queue_mappings`.
    4.  This mapping is saved to the `workflow_metadata` table, and the workflow is marked as `deployed = true`.

This two-step process brilliantly decouples the business-level queue configuration from the technical BPMN implementation.

## 4. The Task Lifecycle: The Synchronization Loop

The `TaskService` and `QueueTaskService` work in tandem to keep the `queue_tasks` table perfectly synchronized with the Flowable engine.

1.  **Creation:** When Flowable creates a new task, the wrapper's `populateQueueTasksForProcessInstance` method is triggered. It finds the new task, looks up its queue in the `task_queue_mappings`, and inserts a corresponding row into the `queue_tasks` table with a status of `OPEN`.
2.  **Claiming:** A user claims a task. The `TaskService` tells Flowable to assign the task, then updates the corresponding row in `queue_tasks` with the `assignee` and sets the `status` to `CLAIMED`.
3.  **Completion & Synchronization:** This is the most critical part of the loop.
    -   The `TaskService` tells Flowable to complete the task.
    -   It marks the task as `COMPLETED` in the `queue_tasks` table.
    -   It **immediately** checks if the process instance is still active. If it is, it re-runs the `populateQueueTasksForProcessInstance` method. This ensures that any new tasks created as a result of the completion are instantly discovered and added to their correct queues. This closed-loop process guarantees the queue table is always up-to-date.

## 5. Scalability and Performance

The architecture is designed to be highly scalable:

-   **Stateless Application:** The wrapper application itself is stateless. All state is managed in the PostgreSQL database, allowing you to run multiple instances behind a load balancer.
-   **Optimized Read Model:** The `queue_tasks` table is a highly optimized read model. Front-end applications query this simple table, avoiding slow, complex queries to the underlying Flowable engine tables.
-   **Asynchronous Potential:** The queue-based nature decouples task creation from completion, enabling asynchronous processing and better system resilience.
-   **Containerized Deployment:** The entire stack is managed with Docker, making it easy to deploy, manage, and scale in modern cloud environments like Kubernetes.
