BPMN Development Knowledge Graph & Standards
This document represents the final, consolidated set of standards for building production-grade BPMN workflows. It is based on the iterative development and debugging of the Sanctions Case Management process and is intended to serve as a knowledge base for both human developers and automated BPMN generation agents.

1. Core Principle: Data Integrity is Paramount

The entire workflow depends on the quality and structure of its data. All standards are in service of this principle.

1.1. Use Native Objects for Process Data

Rule: Complex data structures (e.g., a list of case matches) MUST be stored and passed as native, serializable objects (like java.util.List<Map>), not as a JSON string.

Bad Practice: Using a matches_json variable and constantly parsing it with JsonSlurper. This introduces unnecessary dependencies and performance overhead.

Best Practice: Use a matches variable that is a native List. This simplifies all Groovy scripts to perform direct object manipulation.

1.2. Strict Input Validation is Non-Negotiable

Rule: Every script that processes user input MUST perform two levels of validation immediately:

Completeness: Ensure the input collection is not null and matches the expected size (e.g., decisions.size() == matches.size()).

Correctness: Loop through the input and verify that every value is one of the explicitly allowed options (e.g., the decision must be either 'true_match' or 'false_positive').

Why: This prevents invalid or corrupt data from entering the process, which is the most common cause of failures in later stages. It enforces the API contract at the entry point.

2. Workflow Logic: Design for Resilience

The flow of the process must be robust and handle unexpected scenarios gracefully.

2.1. Gateway Logic MUST Be Foolproof

Exclusive gateways are common points of failure if not configured defensively. A process should never end unexpectedly because a condition wasn't met.

Rule 1: Always Define a Default Flow. Every <exclusiveGateway> MUST have one of its outgoing sequence flows designated as the default. This is the single most effective way to prevent a process from getting "stuck" and ending unexpectedly if no conditions are met.

Rule 2: The Default Path is the "Safest" Path. The default flow should always lead to a safe, non-destructive state. For example, if a supervisor's decision is unclear, the default should be to escalate for more review, not to close the case.

Rule 3: Use Robust, Case-Insensitive String Comparisons. For reliability, all conditional expressions on sequence flows MUST use the .equalsIgnoreCase() method for string comparisons (e.g., ${l1SupervisorDecision.equalsIgnoreCase('close')}). Avoid using == for string comparisons in conditions as it can be less reliable depending on the variable type at runtime.

Rule 4: Simplify Logic with the "Explicit Exception" Pattern. The most robust pattern for a decision gateway is to have only one explicit condition for the exceptional or terminal path, and use the default flow for the primary or "safe" path. Avoid multiple complex conditions.

Why this pattern is the standard: This combination prevents errors from unexpected input (e.g., typos, wrong case) by funneling anything that isn't a perfect match for the "exception" path into the safe, default path for human review. It makes the workflow's behavior predictable and resilient.

Example of the Standard Gateway Pattern:

<!-- 1. The Gateway defines a default flow -->
<exclusiveGateway id="l1_supervisor_decision_gate" name="L1 Supervisor Action?" default="flow_l1_supervisor_escalates_default">
  <documentation>Defaults to escalating if the input is not explicitly 'close'.</documentation>
</exclusiveGateway>

<!-- 2. The "exception" path has one, explicit, case-insensitive condition -->
<sequenceFlow id="flow_l1_supervisor_closes" name="Close Case" sourceRef="l1_supervisor_decision_gate" targetRef="endEvent_closed_by_l1">
  <conditionExpression xsi:type="tFormalExpression">${l1SupervisorDecision.equalsIgnoreCase('close')}</conditionExpression>
</sequenceFlow>

<!-- 3. The "safe" or primary path is the default and has NO condition -->
<sequenceFlow id="flow_l1_supervisor_escalates_default" name="Escalate to L2 (Default)" sourceRef="l1_supervisor_decision_gate" targetRef="prepare_for_l2_script"/>

2.2. Parallel Execution Requires Data Isolation

Rule: When tasks run in parallel, they MUST write their output to uniquely named process variables (e.g., l1MakerDecisions and l1CheckerDecisions).

Why: This is a simple and effective way to prevent race conditions where parallel branches could overwrite each other's data. The data is only merged into a common object after the parallel join gateway.

3. The BPMN-API Contract: Be Explicit and Discoverable

A workflow's interface must be clear and self-documenting. Developers (or agents) should not have to read the source code of a script to know what payload to send.

3.1. Declare All Inputs with Form Properties

Rule: Every <userTask> that requires data to be submitted upon completion MUST formally declare those variables using <flowable:formProperty>.

Correction: The type attribute for complex objects like a list of decisions should be type="string". The client application is responsible for serializing its object into a valid JSON string to pass. Standard Flowable does not have a type="json". For simple variables, use string, long, boolean, etc.

Why: This makes the BPMN file its own API documentation. A client can query a task for its form properties to discover what variables are expected, their names, and their types.

Example of a Correct Form Property:

<userTask id="l1_maker_review_task" ...>
<extensionElements>
<flowable:formProperty id="l1MakerDecisions" name="Level 1 Maker Decisions" type="string" required="true"/>
</extensionElements>
</userTask>

4. Technical Integrity: The XML Must Be Perfect

The BPMN file is an XML document first and foremost.

4.1. Escape All Special Characters

Rule: All special XML characters in attributes and documentation MUST be escaped. The most common error is using & instead of &amp;.

Why: Failure to do this will cause the XML parser to fail, and the workflow will not deploy.

4.2. Ensure Structural Integrity

Rule: The XML file must be well-formed with no hidden characters, breaks, or missing closing tags.

Why: A corrupted XML structure can cause the engine to fail parsing the file, leading to errors where parts of the workflow (like the entire L2 section in our case) are simply not loaded, causing the process to stop dead. Always validate the final XML file with a linter if issues are suspected.

5. Operability: Design for Debugging

5.1. Log Everything in Scripts

Rule: Every script task MUST include println statements at the beginning, end, and at critical decision points.

Why: These logs are invaluable for debugging live processes. They allow operators to trace the execution path and inspect the state of variables at each step without needing a debugger.

5.2. Fail Loudly with BpmnError

Rule: When a script encounters a critical, unrecoverable data error, it MUST throw new org.flowable.engine.delegate.BpmnError().

Why: This creates a formal "incident" in the engine that can be tracked and investigated, which is far superior to a generic NullPointerException. It makes failures visible and actionable.

6. Task Assignment: Using Candidate Groups for Queues

Assigning tasks correctly is fundamental to a queue-based system.

Rule: Any <userTask> that can be worked on by any member of a team MUST be assigned using the flowable:candidateGroups attribute. Tasks assigned to a specific individual should use flowable:assignee.

Why: candidateGroups is the core mechanism that enables a queue-based workflow. Systems like the "Flowable Wrapper" rely on this attribute to map a task to a specific work queue (e.g., level1-maker maps to the level1-maker-queue). This decouples the business process from the specific users, allowing for flexible team management.

Best Practice: Keep group names consistent, descriptive, and aligned with the business function (e.g., level1-supervisor, l2-compliance-officer).

Example:

<userTask id="l1_maker_review_task"
name="Level 1 Maker Review"
flowable:candidateGroups="level1-maker" />

7. Script Task Best Practices

Script tasks are powerful but should be used judiciously and for specific purposes. They are the "glue" of the workflow, not the business logic engine.

Rule: Script tasks MUST ONLY be used for the following purposes:

Validation: Validating user input immediately after a user task (as defined in Standard 1.2).

Transformation/Merging: Merging data from a parallel branch back into a main process variable.

Preparation: Preparing data for a subsequent gateway by setting a decision variable (e.g., calculating autoToLevel2 before a routing gateway).

Bad Practice: Placing complex business logic, external API calls, or long-running calculations inside a script task. These should be handled by serviceTask elements with dedicated Java delegates.

Why: Keeping script tasks small and focused on data manipulation makes the workflow easier to read, debug, and maintain. It clearly separates the process flow from the underlying business logic implementation.

