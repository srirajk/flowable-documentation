
# Flowable Wrapper API: Project Summary

This document provides a comprehensive overview of the Flowable Wrapper API project, including its architecture, key concepts, and a detailed breakdown of the API endpoints.

## 1. Architecture & Key Concepts

The primary goal of this project is to provide a simplified, queue-centric REST API for interacting with the Flowable BPMN engine. It abstracts away the complexity of the native Flowable API and introduces a more intuitive, user-friendly way to manage workflows.

### Core Architectural Decisions:

*   **Embedded Flowable Engine:** The Flowable engine runs directly inside the Spring Boot application. This provides significant performance benefits, enables transactional integrity between the wrapper and the engine, and allows for real-time event handling.
*   **Queue-Centric Abstraction:** The API is designed around the concept of "queues," which map to Flowable's "candidate groups." This is a more natural way for users to think about and interact with their work.
*   **Event-Driven:** The system is highly event-driven. The wrapper listens for events from the Flowable engine (e.g., task created, task completed) and uses them to update its own queue management tables in real-time.
*   **Separation of Concerns:**
    *   **Flowable:** Manages the core workflow execution, state, and variables.
    *   **Wrapper:** Handles user-facing concerns like queue management, task distribution, and providing a simplified REST API.

## 2. API Endpoint Reference

The API is organized into three main controllers:

### 2.1. Workflow Metadata Controller

**Base Path:** `/api/workflow-metadata`

This controller is responsible for managing the metadata associated with your workflows, including their definitions and deployments.

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/register` | Registers a new workflow with the system, including its task-to-queue mappings. |
| `POST` | `/deploy` | Deploys a BPMN workflow to the Flowable engine. |
| `GET` | `/{processDefinitionKey}` | Retrieves the metadata for a specific workflow. |
| `POST` | `/deploy-from-file` | Deploys a BPMN workflow from a file located in the definitions directory. |

### 2.2. Process Instance Controller

**Base Path:** `/api/process-instances`

This controller is used to manage the lifecycle of your workflow instances.

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/start` | Starts a new instance of a deployed workflow. |
| `GET` | `/{processInstanceId}` | Retrieves the details of a specific process instance. |

### 2.3. Task Controller

**Base Path:** `/api/tasks`

This controller provides a rich set of endpoints for managing tasks and interacting with queues.

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/queue/{queueName}` | Retrieves all open tasks from a specific queue. |
| `GET` | `/my-tasks` | Retrieves all tasks assigned to a specific user. |
| `GET` | `/{taskId}` | Retrieves detailed information about a specific task, including its form data. |
| `POST` | `/{taskId}/claim` | Claims an unassigned task. |
| `POST` | `/{taskId}/complete` | Completes a task, optionally passing in variables. |
| `POST` | `/{taskId}/unclaim` | Releases a claimed task back to the queue. |
| `GET` | `/queue/{queueName}/next` | Retrieves the next available task from a queue (highest priority, oldest first). |

## 3. How It Works: The Lifecycle of a Task

1.  **Registration & Deployment:**
    *   You first register your workflow's metadata, defining how tasks are mapped to queues.
    *   You then deploy the BPMN file to the Flowable engine.

2.  **Process Initiation:**
    *   A client application sends a request to the `/api/process-instances/start` endpoint to create a new instance of the workflow.

3.  **Task Creation & Queuing:**
    *   The Flowable engine creates the first task(s) in the workflow.
    *   The wrapper's event listener detects the new task(s) and adds them to the appropriate queues based on the metadata you registered.

4.  **Task Management:**
    *   Users can view the tasks in their queues using the `/api/tasks/queue/{queueName}` or `/api/tasks/my-tasks` endpoints.
    *   A user can claim a task using the `/api/tasks/{taskId}/claim` endpoint.
    *   Once a task is claimed, the user can complete it using the `/api/tasks/{taskId}/complete` endpoint.

5.  **Workflow Progression:**
    *   When a task is completed, the Flowable engine moves the workflow to the next state.
    *   If new tasks are created, the event listener will add them to the appropriate queues, and the cycle continues until the workflow is complete.

This combination of a powerful workflow engine and a well-designed wrapper API provides a robust and flexible platform for building sophisticated, human-centric workflow applications.
