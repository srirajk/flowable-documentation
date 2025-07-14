
Below is a summary of the core architectural decisions and the rationale behind them, followed by an overview of the implemented API endpoints.

## Architectural Thought Process and Design Decisions

The design of the Flowable Wrapper API V2 is rooted in several key architectural decisions intended to improve performance, maintainability, and real-world applicability compared to traditional BPM implementations.

### 1. Embedded Flowable Engine

**Decision:** To embed the Flowable engine directly within the Spring Boot application rather than running it as a separate REST service.

**Rationale:**

* **Performance:** Embedding allows for direct Java method calls, eliminating HTTP overhead between the wrapper and the engine.
* **Transaction Management:** This approach enables wrapper logic and Flowable updates to occur within a single, shared transaction, ensuring data consistency.
* **Deployment Simplicity:** The embedded engine allows for a single application to be deployed and managed.

### 2. Wrapper-Managed Queues (Queue-Centric Approach)

**Decision:** To manage task queues and state primarily within the wrapper application's database, shifting away from a task-centric model to a queue-centric one.

**Rationale:**

* **Real-World Applicability:** Users typically work from designated queues ("My Queues," "Team Queue") rather than searching for specific task IDs.
* **Performance and Scalability:** Storing queue state in a separate wrapper table (`queue_tasks`) avoids constant polling of the Flowable database for task lists, improving responsiveness.
* **Data Enrichment:** The wrapper can enrich task data with business metadata that isn't typically stored in Flowable.
* **Dynamic Routing:** The `WorkflowMetadataService` builds `task_queue_mappings` during deployment by analyzing the BPMN file's candidate groups and mapping them to defined queues.

### 3. Event-Driven Queue Population

**Decision:** To update the `queue_tasks` table and manage task distribution using an event-driven approach.

**Rationale:**

* When a task is completed, an event is triggered. The wrapper queries Flowable for the next active tasks and populates them into the internal `queue_tasks` table.
* This ensures that users see real-time updates and new tasks immediately appear in the correct queues.

---

## API Endpoints and Structure

The API is organized by controller, providing specific functionalities for managing workflows, processes, and tasks.

### 1. Workflow Metadata Endpoints

These endpoints manage the definition and deployment of workflows, mapping candidate groups defined in BPMN to specific queues.

* **`POST /api/workflow-metadata/register`:** Registers a new workflow in the `workflow_metadata` table, defining `candidateGroupMappings` (e.g., "managers" -> "manager-queue").
* **`POST /api/workflow-metadata/deploy`:** Deploys a BPMN XML file to the Flowable engine.
* **`GET /api/workflow-metadata/{processDefinitionKey}`:** Retrieves registered workflow metadata.

### 2. Process Management Endpoints

These endpoints are used for starting and monitoring process instances.

* **`POST /api/process-instances/start`:** Starts a new instance of a deployed workflow using its definition key and initial variables. The system automatically populates the initial tasks into the corresponding queues upon starting.
* **`GET /api/process-instances/{processInstanceId}`:** Retrieves details and the current state of a process instance.

### 3. Task Management Endpoints

The task endpoints focus on enabling users to interact with their assigned queues.

* **`GET /api/tasks/queue/{queueName}`:** Retrieves tasks from a specific queue.
* **`GET /api/tasks/queue/{queueName}/next`:** Retrieves the next available (unassigned) task from a queue, prioritized by highest priority and oldest creation time.
* **`GET /api/tasks/my-tasks`:** Retrieves all tasks assigned to a specific user.
* **`GET /api/tasks/{taskId}`:** Provides detailed information about a specific task, including form data.
* **`POST /api/tasks/{taskId}/claim`:** Claims an unassigned task, updating the assignee in both Flowable and the wrapper's `queue_tasks` table.
* **`POST /api/tasks/{taskId}/complete`:** Completes a task, forwarding process variables to Flowable and triggering the event-driven update for subsequent tasks.
* **`POST /api/tasks/{taskId}/unclaim`:** Releases a claimed task back to the queue.