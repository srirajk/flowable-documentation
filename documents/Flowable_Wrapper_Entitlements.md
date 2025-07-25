# A Deep Dive into Entitlements with Cerbos

This document explains the powerful and flexible authorization model implemented in the Flowable Wrapper using Cerbos. The system is designed to be secure, auditable, and easy to manage by externalizing all authorization logic from the application code.

## 1. Core Philosophy: Centralized, Policy-Based Control

The fundamental principle is that the application code should **never** make authorization decisions. The application's only responsibility is to **ask for permission**. The decision-making logic is delegated entirely to Cerbos, a dedicated, standalone authorization service.

This approach answers three questions for every operation:

1.  **Who is the Principal?** (The user and their associated roles and attributes)
2.  **What is the Resource?** (The specific workflow or task they are interacting with, including its data)
3.  **What is the Action?** (The operation they are attempting, e.g., `claim_task`, `start_workflow_instance`)

## 2. The Authorization Flow: A Step-by-Step Guide

When a user attempts an action, the following sequence occurs:

1.  **API Request:** The Flowable Wrapper receives an API call (e.g., `POST /api/tasks/{taskId}/complete`).
2.  **Context Gathering:** The `CerbosService` is invoked. It acts as an orchestrator, gathering a rich set of data:
    -   It calls the `UserManagementService` to fetch the user's profile, including their assigned roles and attributes for the relevant Business Application.
    -   It queries the application's database and the Flowable engine to build a detailed picture of the resource, including process variables, task status, and workflow metadata.
3.  **Permission Check:** The `CerbosService` sends this entire context—Principal, Resource, and Action—to the Cerbos engine.
4.  **Policy Evaluation:** Cerbos evaluates the request against its set of human-readable YAML policies. It finds the relevant policy for the resource and checks if any rules allow the principal to perform the requested action.
5.  **Decision Enforcement:** Cerbos returns a simple `ALLOW` or `DENY` decision. The application code does not know *why* the decision was made, only what it is. If the decision is `DENY`, the API call is immediately rejected with a `403 Forbidden` status. If it is `ALLOW`, the application proceeds with the business logic.

## 3. The Cerbos Policy Structure: A Guided Tour

The power of this system lies in the structure of the Cerbos policies, located in `custom-flowable/docker/cerbos/policies/`.

#### a. `derived_roles`

This directory contains definitions for roles that are not static but are calculated dynamically at runtime. For example, `workflow-management-roles.yaml` defines roles like `business_app_member_deployer`. A user is only granted this role *if* the business application they are trying to deploy to is present in their list of assigned applications (`request.resource.attr.businessApp in request.principal.attr.businessApps`). This allows for powerful, attribute-based access control.

#### b. `resource_policies`

This is the core of the policy set, containing the rules for specific resources. The folder structure is designed to be modular and easy to navigate.

-   **`workflow/default/workflow-management-resource.yaml`**: This policy governs actions related to the lifecycle of a workflow *definition* itself, such as registering or deploying it. It uses the derived roles to grant permissions. For example, a user with the `deployer` role can perform the `deploy` action only on workflows belonging to their assigned business applications.

-   **Future Structure (Example):** The structure is extensible. For a sanctions screening workflow, you might have:
    -   `resource_policies/workflow/sanctions-management/sanctionsCaseManagement.yaml`

    This file would contain policies specific to the `sanctionsCaseManagement` resource, with rules like:
    -   An `L1_analyst` can only claim a task if it hasn't been touched by another `L1_analyst`.
    -   An `L2_manager` can only approve a case if the `amount` is within their approval limit.
    -   A user cannot approve a case they previously worked on (the Four-Eyes Principle).

## 4. The User Management Foundation

The entitlement system is underpinned by a flexible user management model defined in the database schema:

-   **`users`**: Stores the core user profile and a flexible `attributes` JSONB column for storing details like department, approval limits, etc.
-   **`business_applications`**: Defines the different applications or tenants in the system.
-   **`business_app_roles`**: Defines the available roles within a specific business application (e.g., `approver`, `submitter`).
-   **`user_business_app_roles`**: The mapping table that assigns a user a specific role within a specific business application.

The `UserManagementService` queries these tables to provide the `CerbosService` with the necessary Principal context to make its decisions.

## 5. Key Benefits of this Approach

-   **Centralization & Auditability:** All authorization logic is in one place, making it easy to understand, audit, and manage.
-   **Decoupling:** The application code is free of `if/else` authorization logic, making it cleaner and more maintainable.
-   **Flexibility:** Business rules can be changed by editing human-readable YAML files, without requiring a full application redeployment.
-   **Fine-Grained Control:** The rich context sent to Cerbos allows for extremely detailed, attribute-based access control that can model complex business requirements like the Four-Eyes Principle or time-based access.
