# Concept: The BPMN Analyst Agent

## Overview

An intelligent, conversational agent designed to streamline the creation, validation, and deployment of BPMN workflows within the Flowable ecosystem. The agent acts as an expert Flowable process architect, enabling users to design, test, and deploy complex business processes using plain English, without needing to write XML or complex Groovy scripts directly.

This concept is born from the iterative, real-world debugging and enhancement of the Sanctions Case Management workflow.

---

## Core Capabilities

1.  **Conversational Flow Design:**
    *   Users describe their business process in natural language (e.g., *"I need a two-level approval for purchase orders. If the amount is over $5,000, it needs a third level of approval from the finance director."*).
    *   The agent interprets the requirements and asks clarifying questions to resolve ambiguity.

2.  **Automated BPMN Generation:**
    *   The agent translates the user's description into a valid, well-structured `.bpmn20.xml` file.
    *   It automatically includes standard elements like parallel gateways for concurrent tasks, user tasks for human interaction, and script tasks for data manipulation.

3.  **Logical Validation & Auto-Correction:**
    *   **Race Condition Detection:** Automatically identifies and fixes race conditions in parallel data processing paths by isolating script task outputs and merging them safely after a join gateway.
    *   **Gateway Logic Integrity:** Ensures all exclusive and inclusive gateways have valid conditional expressions and a safe default path.
    *   **Dead End & Orphan Detection:** Scans the workflow to ensure all paths lead to a valid end event and no tasks are orphaned.
    *   **Best Practice Scripting:** Injects robust logging, error handling (try/catch blocks), and defensive data handling (null checks, type casting) into all generated Groovy scripts.

4.  **Automated Metadata & Queue Setup:**
    *   The agent parses the final BPMN file to identify all user tasks and their `flowable:candidateGroups`.
    *   It automatically generates the JSON structure required for the `workflow-metadata-service` to register task-to-queue mappings.
    *   It can generate the `curl` command needed to post this metadata to the registration endpoint.

5.  **Guided Testing & Deployment:**
    *   For each user task, the agent generates a sample `curl` command, pre-filled with the correct task ID placeholder and example variables. This allows the user to easily test every path of the workflow.
    *   Once testing is complete, the agent provides the final `curl` command to deploy the BPMN file to the Flowable engine via the wrapper service.

---

## Proposed Development Plan

### Phase 1: The Validator (Proof of Concept)

*   **Goal:** Generalize the validation and correction logic used during the sanctions workflow debugging.
*   **Features:**
    *   Input: A user-provided requirements document (Markdown) and a BPMN file.
    *   Function: Analyze the gap between requirements and the BPMN, identify logical flaws (race conditions, bad gateway logic), propose corrections, and apply them to the BPMN file.
    *   Output: A corrected BPMN file and the necessary deployment/testing commands.

### Phase 2: The Architect (Interactive Agent)

*   **Goal:** Enable conversational BPMN generation from scratch.
*   **Features:**
    *   A user starts with a simple prompt or an empty directory.
    *   The agent interactively gathers requirements through conversation.
    *   The agent generates the complete set of artifacts: the BPMN file, queue metadata JSON, and testing/deployment commands.

### Phase 3: The Partner (Full-Fledged Assistant)

*   **Goal:** Add advanced features and deep project integration.
*   **Features:**
    *   **Git Integration:** Automatically creates new branches for workflows and commits changes.
    *   **Visual Diagram Generation:** Creates visual `SVG` or `PNG` diagrams of the generated workflow.
    *   **Process Optimization:** Suggests improvements, such as converting sequential tasks to parallel where appropriate or simplifying complex gateway logic.
    *   **Pattern Library:** Maintains a library of pre-built, validated workflow patterns (e.g., "Standard 4-eyes Approval," "Dynamic Expert Routing") that can be used as templates.
