Here’s the clean, professional markdown version of your Sanctions Case Management Workflow API Documentation (without checkmarks, emojis, or any casual formatting):

⸻

Sanctions Case Management Workflow - API Documentation

1. Use Case Overview

The Sanctions Case Management Workflow is a comprehensive L1→L2 escalation process designed for financial compliance and sanctions screening. The system implements a multi-level maker/checker approval process with automatic routing based on decision outcomes.

Business Process Flow
•	Level 1: Maker/Checker parallel review with supervisor escalation for mixed decisions
•	Level 2: Enhanced maker/checker review for escalated cases with final supervisor decision
•	Decision Logic: Automatic routing based on agreement/disagreement patterns
•	Audit Trail: Complete decision history preserved throughout the process

⸻

2. Wrapper API Endpoints

The Flowable wrapper provides a simplified, queue-based API for workflow management.

2.1 Workflow Management

Method	Endpoint	Description
POST	/api/workflow-metadata/register	Register workflow metadata and queue mappings
POST	/api/workflow-metadata/deploy-from-file	Deploy BPMN from the file system
GET	/api/workflow-metadata/{processDefinitionKey}	Get workflow metadata

2.2 Process Instance Management

Method	Endpoint	Description
POST	/api/process-instances/start	Start a new process instance
GET	/api/process-instances/{processInstanceId}	Get process instance details and variables

2.3 Task Management

Method	Endpoint	Description
GET	/api/tasks/queue/{queueName}	Get tasks by queue
GET	/api/tasks/{taskId}	Get detailed task information with form data
POST	/api/tasks/{taskId}/claim?userId={userId}	Claim task for user
POST	/api/tasks/{taskId}/complete	Complete task with variables
POST	/api/tasks/{taskId}/unclaim	Release claimed task back to the queue


⸻

3. Metadata Registration

Request

curl -X POST "http://localhost:8090/api/workflow-metadata/register" \
-H "Content-Type: application/json" \
-d '{
"processDefinitionKey": "sanctionsCaseManagement",
"processName": "Sanctions L1-L2 Flow",
"description": "Level 1 and Level 2 sanctions case management workflow",
"candidateGroupMappings": {
"level1-maker": "default",
"level1-checker": "default",
"level1-supervisor": "default",
"level2-maker": "default",
"level2-checker": "default",
"level2-supervisor": "default"
}
}'

Response

{
"id": 1,
"processDefinitionKey": "sanctionsCaseManagement",
"processName": "Sanctions L1-L2 Flow",
"deployed": false,
"candidateGroupMappings": {
"level1-maker": "default",
"level1-checker": "default",
"level1-supervisor": "default",
"level2-maker": "default",
"level2-checker": "default",
"level2-supervisor": "default"
}
}


⸻

4. Workflow Deployment

Request

curl -X POST "http://localhost:8090/api/workflow-metadata/deploy-from-file?processDefinitionKey=sanctionsCaseManagement&filename=SanctionsL1L2Flow.bpmn20.xml" \
--data-binary @/dev/null \
-H "Content-Type: application/xml"

Response

{
"id": 1,
"processDefinitionKey": "sanctionsCaseManagement",
"deployed": true,
"deploymentId": "abc123-def456",
"taskQueueMappings": [
{
"taskId": "l1_maker_review_task",
"taskName": "Level 1 Maker Review",
"candidateGroups": ["level1-maker"],
"queue": "default"
}
]
}


⸻

5. Task Management

5.1 Get Tasks by Queue

curl -X GET "http://localhost:8090/api/tasks/queue/default"

Response:

[
{
"taskId": "abc123-task-id",
"processInstanceId": "process-instance-id",
"taskDefinitionKey": "l1_maker_review_task",
"taskName": "Level 1 Maker Review",
"queueName": "default",
"assignee": null,
"status": "OPEN",
"priority": 50,
"createdAt": 1753121963.202957,
"description": "Review all matches for Case ID: ${caseId}...",
"businessKey": null
}
]

5.2 Get Task by ID

curl -X GET "http://localhost:8090/api/tasks/{taskId}"

Response:

{
"taskId": "abc123-task-id",
"taskName": "Level 1 Maker Review",
"queueName": "default",
"assignee": null,
"status": "OPEN",
"formData": {
"caseId": "CASE-123",
"matches": [
{
"matchId": "MATCH-001",
"entityName": "John Doe",
"score": 0.95,
"category": "sanctions"
}
]
},
"processVariables": {
"caseId": "CASE-123",
"matches": [...],
"autoToLevel2": false
}
}

5.3 Claim Task

curl -X POST "http://localhost:8090/api/tasks/{taskId}/claim?userId=john.doe"

Response:

{
"taskId": "abc123-task-id",
"taskName": "Level 1 Maker Review",
"assignee": "john.doe",
"status": "CLAIMED",
"claimedAt": 1753122010.597582
}

5.4 Complete Task

curl -X POST "http://localhost:8090/api/tasks/{taskId}/complete" \
-H "Content-Type: application/json" \
-d '{
"variables": {
"l1MakerDecisions": [
{
"matchId": "MATCH-001",
"decision": "true_match",
"comment": "High confidence match"
}
]
}
}'

Response:

{
"status": "COMPLETED",
"message": "Task completed successfully",
"taskId": "abc123-task-id",
"completedAt": 1753122039.619,
"processActive": true,
"nextTaskId": "next-task-id",
"nextTaskName": "Level 1 Checker Review",
"nextTaskQueue": "default"
}

5.5 Process Flow Navigation
1.	Start process → Get initial L1 tasks
2.	Complete L1 tasks → Route to supervisor or L2
3.	Handle supervisor decision → Continue or end
4.	Complete L2 tasks → Final supervisor decision
5.	Process completion → Case closed

Example:

# Start process
PROCESS_ID=$(curl -s -X POST "/api/process-instances/start" -d '{...}' | jq -r .processInstanceId)

# Get current tasks
TASKS=$(curl -s "/api/tasks/queue/default")

# Complete tasks in sequence following nextTaskId/nextTaskName


⸻

6. Test Scenarios

Scenario 1: Mixed L1 Decisions → L1 Supervisor Escalation → L2 Processing
•	Objective: Test mixed L1 decisions that escalate through supervisor to Level 2
•	Flow:
1.	Start process
2.	L1 Maker: true_match
3.	L1 Checker: false_positive → Mixed decisions escalate to L1 Supervisor
4.	L1 Supervisor: escalate → Routes to L2
5.	L2 Maker/Checker: true_match → L2 Supervisor
6.	L2 Supervisor: true_positive → Process closes

⸻

Scenario 2: L1 Agreement → Direct L2 Route → False Positive Closure
•	Objective: Test L1 agreement that auto-routes to L2, ending as false positive
•	Flow:
1.	L1 Maker/Checker: true_match → Automatic L2 routing
2.	L2 Supervisor: false_positive → Process closes

⸻

Scenario 3: L1 Supervisor Closes Case (No L2)
•	Objective: Test L1 supervisor decision to close case without L2 escalation
•	Flow:
1.	Mixed L1 decisions → L1 Supervisor
2.	L1 Supervisor: close → Process ends at L1 level

⸻

7. Summary

This API provides a complete workflow management solution with:
•	Self-documenting tasks via form properties
•	Queue-based task distribution
•	Comprehensive decision tracking
•	Flexible routing logic
•	Complete audit trails

The wrapper simplifies Flowable integration while maintaining full workflow capabilities for complex business processes.

⸻

✅ Next Step: I can also generate this as a .md file for you. Do you want me to create the downloadable markdown file now?