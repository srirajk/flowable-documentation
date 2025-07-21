# Project Summary and Architectural Decisions

This document summarizes the conversation and the final architectural decisions for the **BPMN Builder Agent**.

## 1. Project Goal
The primary goal is to build a conversational AI agent that can assist a user in creating, deploying, and testing simple business workflows. The agent should understand a user's request in natural language, translate it into a valid BPMN 2.0 XML file, and use the existing `flowable-wrapper-v2` API to bring that workflow to life.

## 2. The Evolution of Our Ideas
Our conversation explored several architectural patterns, each refining our approach:

1.  **Initial Idea: The Analyst Agent.** A read-only agent using Retrieval-Augmented Generation (RAG) to answer questions about existing BPMN files and documentation. This was discarded as it did not meet the goal of *building* new workflows.

2.  **Second Idea: The Builder Agent Script.** A Python script that would take a prompt and run from top-to-bottom to generate, deploy, and test a workflow. This was refined because it lacked the desired conversational and interactive nature.

3.  **Third Idea: The Conversational Agent with a Local Knowledge Base.** We decided the agent must be conversational. The initial plan was to create a local knowledge base (a `knowledge.json` file and/or a Vector DB) from the project's documentation to solve the LLM context window limit.

4.  **Final Idea: The ADK Agent with a Centralized KG.** This is our final, agreed-upon architecture. It refines the previous idea by leveraging the user's existing infrastructure (the MCP Server/KG) and a professional agent framework (Google ADK).

## 3. Final Architecture: A Tri-Component System
We have decided on a hub-and-spoke architecture where the new agent is the central orchestrator.

```
                               +---------------------------------+
                               |                                 |
                               |      MCP Server / KG API        |
                               |      (The Source of Truth)      |
                               |                                 |
                               +---------------------------------+
                                               ^
                                               | (Tool #1: knowledge_lookup)
                                               |
+-----------+      (Input/Output)    +------------------+      (Tool #2: workflow_actions)
|           | <--------------------> |                  | ----------------------> +----------------------+
| End User  |                        |  Google ADK      |                         |                      |
|           | <--------------------> |      Agent       | ----------------------> | Flowable Wrapper API |
+-----------+                        +------------------+                         |                      |
                                     (The "Brain")                                +----------------------+
```

### Key Components:

*   **The Agent (The "Brain"):**
    *   **Framework:** To be built using the **Google Agent Development Kit (ADK)**.
    *   **Function:** Acts as the central orchestrator. It manages the conversation with the user, formulates plans, and uses its tools to execute those plans.
    *   **Core Logic:** It operates on a "Think -> Act" loop, driven by a Gemini LLM.

*   **The Existing MCP Server (The "Source of Truth"):**
    *   **Function:** Serves as the agent's external knowledge base. It is the single source of truth for all rules, constraints, and API specifications.
    *   **Knowledge Strategy:** We will **not** create a new, separate knowledge base (like a Vector DB or local JSON file). Instead, we will perform a one-time extraction of knowledge from the project's documentation and **load it into this existing MCP Server**.

*   **The Flowable Wrapper API (The "Hands"):**
    *   **Function:** The execution environment. The agent will interact with this API to perform all real-world actions like deploying BPMN files and running process instances.

### Agent Workflow & Tools:

The ADK agent will be equipped with two primary tools:

1.  **`knowledge_lookup(...)`**: This tool is responsible for all communication with the **MCP Server**. When the agent needs to know a rule (e.g., "What BPMN elements am I allowed to use?"), it will call this tool to query the KG.

2.  **`workflow_actions(...)`**: This tool is responsible for all communication with the **Flowable Wrapper API**. When the agent needs to perform an action (e.g., "Deploy this BPMN content"), it will call this tool.

This architecture is robust, scalable, and leverages existing infrastructure, preventing the creation of redundant knowledge stores.
