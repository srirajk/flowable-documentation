## Register Workflow Metadata API

### Request

```sh
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/register" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: automation-user-2" \
  -d '{
    "processDefinitionKey": "sanctionsCaseManagement",
    "processName": "Sanctions L1-L2 Flow",
    "businessAppName": "Sanctions-Management",
    "description": "Level 1 and Level 2 sanctions case management workflow",
    "candidateGroupMappings": {
      "level1-maker": "level1-queue",
      "level1-checker": "level1-queue",
      "level1-supervisor": "level1-supervisor-queue",
      "level2-maker": "level2-queue",
      "level2-checker": "level2-queue",
      "level2-supervisor": "level2-supervisor-queue"
    }
  }'
```

Response:
```json
{
  "id": 1,
  "processDefinitionKey": "sanctionsCaseManagement",
  "processName": "Sanctions L1-L2 Flow",
  "description": "Level 1 and Level 2 sanctions case management workflow",
  "version": 1,
  "businessAppName": "Sanctions-Management",
  "candidateGroupMappings": {
    "level1-maker": "level1-queue",
    "level1-checker": "level1-queue",
    "level1-supervisor": "level1-supervisor-queue",
    "level2-maker": "level2-queue",
    "level2-checker": "level2-queue",
    "level2-supervisor": "level2-supervisor-queue"
  },
  "taskQueueMappings": null,
  "metadata": null,
  "active": true,
  "createdBy": "system",
  "createdAt": 1753453752.867924,
  "updatedAt": 1753453752.867945,
  "deployed": false,
  "deploymentId": null
}
```

## Deploy from File

### Request

```sh
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/deploy-from-file?processDefinitionKey=sanctionsCaseManagement&filename=SanctionsL1L2Flow.bpmn20.xml" \
      -H "X-User-Id: automation-user-2"
```

Response:
```json
{
  "id": 1,
  "processDefinitionKey": "sanctionsCaseManagement",
  "processName": "Sanctions L1-L2 Flow",
  "description": "Level 1 and Level 2 sanctions case management workflow",
  "version": 1,
  "businessAppName": "Sanctions-Management",
  "candidateGroupMappings": {
    "level1-maker": "level1-queue",
    "level2-maker": "level2-queue",
    "level1-checker": "level1-queue",
    "level2-checker": "level2-queue",
    "level1-supervisor": "level1-supervisor-queue",
    "level2-supervisor": "level2-supervisor-queue"
  },
  "taskQueueMappings": [
    {
      "taskId": "l1_maker_review_task",
      "taskName": "Level 1 Maker Review",
      "candidateGroups": [
        "level1-maker"
      ],
      "queue": "level1-queue",
      "metadata": {
        "documentation": "Review all matches for Case ID: ${caseId}.\n                Provide a decision (true_match / false_positive) and comments for every match.",
        "formKey": null,
        "priority": null,
        "category": null
      }
    },
    {
      "taskId": "l1_checker_review_task",
      "taskName": "Level 1 Checker Review",
      "candidateGroups": [
        "level1-checker"
      ],
      "queue": "level1-queue",
      "metadata": {
        "documentation": "Review all matches for Case ID: ${caseId}.\n                Provide a decision (true_match / false_positive) and comments for every match.",
        "formKey": null,
        "priority": null,
        "category": null
      }
    },
    {
      "taskId": "l1_supervisor_task",
      "taskName": "Level 1 Supervisor Review",
      "candidateGroups": [
        "level1-supervisor"
      ],
      "queue": "level1-supervisor-queue",
      "metadata": {
        "documentation": "Review L1 maker/checker decisions for Case ID: ${caseId}.\n                Decide whether to close the case or escalate to Level 2.",
        "formKey": null,
        "priority": null,
        "category": null
      }
    },
    {
      "taskId": "l2_maker_review_task",
      "taskName": "Level 2 Maker Review",
      "candidateGroups": [
        "level2-maker"
      ],
      "queue": "level2-queue",
      "metadata": {
        "documentation": "Review all matches for Case ID: ${caseId} at Level 2.",
        "formKey": null,
        "priority": null,
        "category": null
      }
    },
    {
      "taskId": "l2_checker_review_task",
      "taskName": "Level 2 Checker Review",
      "candidateGroups": [
        "level2-checker"
      ],
      "queue": "level2-queue",
      "metadata": {
        "documentation": "Review all matches for Case ID: ${caseId} at Level 2.",
        "formKey": null,
        "priority": null,
        "category": null
      }
    },
    {
      "taskId": "l2_supervisor_task",
      "taskName": "Level 2 Supervisor Review",
      "candidateGroups": [
        "level2-supervisor"
      ],
      "queue": "level2-supervisor-queue",
      "metadata": {
        "documentation": "Review L2 maker/checker decisions for Case ID: ${caseId}.\n                Make a final decision to close the case as a True Positive or False Positive.",
        "formKey": null,
        "priority": null,
        "category": null
      }
    }
  ],
  "metadata": null,
  "active": true,
  "createdBy": "system",
  "createdAt": 1753453752.867924,
  "updatedAt": 1753453752.867945,
  "deployed": true,
  "deploymentId": "596d327d-6964-11f0-b919-7225c1a185c2"
}
```

## Start Process Instance

### Request

```sh
curl -X POST "http://localhost:8090/api/Sanctions-Management/process-instances/start" \
      -H "Content-Type: application/json" \
      -H "X-User-Id: automation-user-2" \
      -d '{
        "processDefinitionKey": "sanctionsCaseManagement",
        "businessKey": "SC001",
        "variables": {
          "caseId": "SC001",
          "customerName": "John Doe",
          "sanctionType": "PEP",
          "riskLevel": "HIGH",
          "region": "US",
          "amount": 150000.00,
          "currency": "USD",
          "description": "High-risk PEP customer requiring L1 and L2 review",
          "matches": [
            {
              "matchId": "M001",
              "matchType": "NAME",
              "confidence": 95.0,
              "sanctionList": "OFAC_SDN",
              "matchDetails": "John Doe - OFAC SDN List"
            }
          ]
        }
      }'
```

### Response
```json
{
  "processInstanceId": "247fbd30-6965-11f0-b919-7225c1a185c2",
  "processDefinitionId": "sanctionsCaseManagement:1:597dd44f-6964-11f0-b919-7225c1a185c2",
  "processDefinitionKey": "sanctionsCaseManagement",
  "processDefinitionName": "Sanctions Case Management (L1 & L2)",
  "businessKey": "SC001",
  "startTime": 1753454357.472186,
  "startedBy": null,
  "suspended": false,
  "variables": {
    "sanctionType": "PEP",
    "amount": 150000.0,
    "riskLevel": "HIGH",
    "caseId": "SC001",
    "description": "High-risk PEP customer requiring L1 and L2 review",
    "currency": "USD",
    "region": "US",
    "matches": [
      {
        "matchId": "M001",
        "matchType": "NAME",
        "confidence": 95.0,
        "sanctionList": "OFAC_SDN",
        "matchDetails": "John Doe - OFAC SDN List"
      }
    ],
    "customerName": "John Doe"
  },
  "active": null,
  "endTime": null,
  "durationInMillis": null
}


```

## Task List for User
### Request

```sh
curl -X GET "http://localhost:8090/api/Sanctions-Management/tasks/queue/level1-queue" \
      -H "X-User-Id: us-l1-operator-1"
```

### Response
```json

[
  {
    "taskId": "24853b8e-6965-11f0-b919-7225c1a185c2",
    "processInstanceId": "247fbd30-6965-11f0-b919-7225c1a185c2",
    "processDefinitionKey": "sanctionsCaseManagement",
    "taskDefinitionKey": "l1_maker_review_task",
    "taskName": "Level 1 Maker Review",
    "queueName": "level1-queue",
    "assignee": null,
    "status": "OPEN",
    "priority": 50,
    "createdAt": 1753454357.465093,
    "claimedAt": null,
    "completedAt": null,
    "taskData": {
      "owner": null,
      "dueDate": null,
      "formKey": null,
      "createTime": 1753454357368,
      "description": "Review all matches for Case ID: SC001.\n                Provide a decision (true_match / false_positive) and comments for every match.",
      "taskDefinitionKey": "l1_maker_review_task"
    },
    "businessKey": "SC001"
  },
  {
    "taskId": "2485b0c1-6965-11f0-b919-7225c1a185c2",
    "processInstanceId": "247fbd30-6965-11f0-b919-7225c1a185c2",
    "processDefinitionKey": "sanctionsCaseManagement",
    "taskDefinitionKey": "l1_checker_review_task",
    "taskName": "Level 1 Checker Review",
    "queueName": "level1-queue",
    "assignee": null,
    "status": "OPEN",
    "priority": 50,
    "createdAt": 1753454357.470778,
    "claimedAt": null,
    "completedAt": null,
    "taskData": {
      "owner": null,
      "dueDate": null,
      "formKey": null,
      "createTime": 1753454357384,
      "description": "Review all matches for Case ID: SC001.\n                Provide a decision (true_match / false_positive) and comments for every match.",
      "taskDefinitionKey": "l1_checker_review_task"
    },
    "businessKey": "SC001"
  }
]

```

# Complete E2E Test Run - Fresh Environment

## Register Workflow Metadata (Fresh)

### Request
```sh
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/register" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: automation-user-2" \
  -d '{
    "processDefinitionKey": "sanctionsCaseManagement",
    "processName": "Sanctions L1-L2 Flow",
    "businessAppName": "Sanctions-Management",
    "description": "Level 1 and Level 2 sanctions case management workflow",
    "candidateGroupMappings": {
      "level1-maker": "level1-queue",
      "level1-checker": "level1-queue",
      "level1-supervisor": "level1-supervisor-queue",
      "level2-maker": "level2-queue",
      "level2-checker": "level2-queue",
      "level2-supervisor": "level2-supervisor-queue"
    }
  }'
```

### Response
```json
{"id":1,"processDefinitionKey":"sanctionsCaseManagement","processName":"Sanctions L1-L2 Flow","description":"Level 1 and Level 2 sanctions case management workflow","version":1,"businessAppName":"Sanctions-Management","candidateGroupMappings":{"level1-maker":"level1-queue","level1-checker":"level1-queue","level1-supervisor":"level1-supervisor-queue","level2-maker":"level2-queue","level2-checker":"level2-queue","level2-supervisor":"level2-supervisor-queue"},"taskQueueMappings":null,"metadata":null,"active":true,"createdBy":"system","createdAt":1753468257.346013000,"updatedAt":1753468257.346030000,"deployed":false,"deploymentId":null}
```

## Deploy BPMN (Fresh)

### Request
```sh
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/deploy-from-file?processDefinitionKey=sanctionsCaseManagement&filename=SanctionsL1L2Flow.bpmn20.xml" \
  -H "X-User-Id: automation-user-2"
```

### Response
```json
{"id":1,"processDefinitionKey":"sanctionsCaseManagement","processName":"Sanctions L1-L2 Flow","description":"Level 1 and Level 2 sanctions case management workflow","version":1,"businessAppName":"Sanctions-Management","candidateGroupMappings":{"level1-maker":"level1-queue","level2-maker":"level2-queue","level1-checker":"level1-queue","level2-checker":"level2-queue","level1-supervisor":"level1-supervisor-queue","level2-supervisor":"level2-supervisor-queue"},"taskQueueMappings":[{"taskId":"l1_maker_review_task","taskName":"Level 1 Maker Review","candidateGroups":["level1-maker"],"queue":"level1-queue","metadata":{"documentation":"Review all matches for Case ID: ${caseId}.\n                Provide a decision (true_match / false_positive) and comments for every match.","formKey":null,"priority":null,"category":null}},{"taskId":"l1_checker_review_task","taskName":"Level 1 Checker Review","candidateGroups":["level1-checker"],"queue":"level1-queue","metadata":{"documentation":"Review all matches for Case ID: ${caseId}.\n                Provide a decision (true_match / false_positive) and comments for every match.","formKey":null,"priority":null,"category":null}},{"taskId":"l1_supervisor_task","taskName":"Level 1 Supervisor Review","candidateGroups":["level1-supervisor"],"queue":"level1-supervisor-queue","metadata":{"documentation":"Review L1 maker/checker decisions for Case ID: ${caseId}.\n                Decide whether to close the case or escalate to Level 2.","formKey":null,"priority":null,"category":null}},{"taskId":"l2_maker_review_task","taskName":"Level 2 Maker Review","candidateGroups":["level2-maker"],"queue":"level2-queue","metadata":{"documentation":"Review all matches for Case ID: ${caseId} at Level 2.","formKey":null,"priority":null,"category":null}},{"taskId":"l2_checker_review_task","taskName":"Level 2 Checker Review","candidateGroups":["level2-checker"],"queue":"level2-queue","metadata":{"documentation":"Review all matches for Case ID: ${caseId} at Level 2.","formKey":null,"priority":null,"category":null}},{"taskId":"l2_supervisor_task","taskName":"Level 2 Supervisor Review","candidateGroups":["level2-supervisor"],"queue":"level2-supervisor-queue","metadata":{"documentation":"Review L2 maker/checker decisions for Case ID: ${caseId}.\n                Make a final decision to close the case as a True Positive or False Positive.","formKey":null,"priority":null,"category":null}}],"metadata":null,"active":true,"createdBy":"system","createdAt":1753468257.346013000,"updatedAt":1753468257.346030000,"deployed":true,"deploymentId":"efb5727f-6988-11f0-a69a-8a52b726ab7d"}
```

## Start Process Instance (Fresh Case SC002)

### Request
```sh
curl -X POST "http://localhost:8090/api/Sanctions-Management/process-instances/start" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: automation-user-2" \
  -d '{
    "processDefinitionKey": "sanctionsCaseManagement",
    "businessKey": "SC002",
    "variables": {
      "caseId": "SC002",
      "customerName": "Jane Smith",
      "sanctionType": "PEP",
      "riskLevel": "HIGH",
      "region": "US",
      "amount": 250000.00,
      "currency": "USD",
      "description": "High-risk PEP customer requiring complete L1 and L2 review cycle",
      "matches": [
        {
          "matchId": "M002",
          "matchType": "NAME",
          "confidence": 98.0,
          "sanctionList": "OFAC_SDN",
          "matchDetails": "Jane Smith - OFAC SDN List"
        }
      ]
    }
  }'
```

### Response
```json
{"processInstanceId":"12b23432-6989-11f0-a69a-8a52b726ab7d","processDefinitionId":"sanctionsCaseManagement:1:efc948a1-6988-11f0-a69a-8a52b726ab7d","processDefinitionKey":"sanctionsCaseManagement","processDefinitionName":"Sanctions Case Management (L1 & L2)","businessKey":"SC002","startTime":1753469789.476366000,"startedBy":null,"suspended":false,"variables":{"sanctionType":"PEP","amount":250000.0,"riskLevel":"HIGH","caseId":"SC002","description":"High-risk PEP customer requiring complete L1 and L2 review cycle","currency":"USD","region":"US","matches":[{"matchId":"M002","matchType":"NAME","confidence":98.0,"sanctionList":"OFAC_SDN","matchDetails":"Jane Smith - OFAC SDN List"}],"customerName":"Jane Smith"},"active":null,"endTime":null,"durationInMillis":null}
```

## Check Level1 Queue

### Request
```sh
curl -X GET "http://localhost:8090/api/Sanctions-Management/tasks/queue/level1-queue" \
  -H "X-User-Id: us-l1-operator-1"
```

### Response
```json
[{"taskId":"12b6c830-6989-11f0-a69a-8a52b726ab7d","processInstanceId":"12b23432-6989-11f0-a69a-8a52b726ab7d","processDefinitionKey":"sanctionsCaseManagement","taskDefinitionKey":"l1_maker_review_task","taskName":"Level 1 Maker Review","queueName":"level1-queue","assignee":null,"status":"OPEN","priority":50,"createdAt":1753469789.468231000,"claimedAt":null,"completedAt":null,"taskData":{"owner":null,"dueDate":null,"formKey":null,"createTime":1753469789380,"description":"Review all matches for Case ID: SC002.\n                Provide a decision (true_match / false_positive) and comments for every match.","taskDefinitionKey":"l1_maker_review_task"},"businessKey":"SC002"},{"taskId":"12b6ef43-6989-11f0-a69a-8a52b726ab7d","processInstanceId":"12b23432-6989-11f0-a69a-8a52b726ab7d","processDefinitionKey":"sanctionsCaseManagement","taskDefinitionKey":"l1_checker_review_task","taskName":"Level 1 Checker Review","queueName":"level1-queue","assignee":null,"status":"OPEN","priority":50,"createdAt":1753469789.474347000,"claimedAt":null,"completedAt":null,"taskData":{"owner":null,"dueDate":null,"formKey":null,"createTime":1753469789390,"description":"Review all matches for Case ID: SC002.\n                Provide a decision (true_match / false_positive) and comments for every match.","taskDefinitionKey":"l1_checker_review_task"},"businessKey":"SC002"}]
```

## L1 Maker: Claim Task

### Request
```sh
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/12b6c830-6989-11f0-a69a-8a52b726ab7d/claim" \
  -H "X-User-Id: us-l1-operator-1" \
  -H "Content-Type: application/json" \
  -d '{}'
```

### Response
```json
{"taskId":"12b6c830-6989-11f0-a69a-8a52b726ab7d","processInstanceId":"12b23432-6989-11f0-a69a-8a52b726ab7d","processDefinitionKey":"sanctionsCaseManagement","taskDefinitionKey":"l1_maker_review_task","taskName":"Level 1 Maker Review","queueName":"level1-queue","assignee":"us-l1-operator-1","status":"CLAIMED","priority":50,"createdAt":1753469789.468231000,"claimedAt":1753469836.108657000,"completedAt":null,"taskData":{"owner":null,"dueDate":null,"formKey":null,"createTime":1753469789380,"description":"Review all matches for Case ID: SC002.\n                Provide a decision (true_match / false_positive) and comments for every match.","taskDefinitionKey":"l1_maker_review_task"},"businessKey":"SC002"}
```

## L1 Maker: Complete Task (VALIDATION_FAILED Examples)

### Request #1 (Failed - Wrong Variable Format)
```sh
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/12b6c830-6989-11f0-a69a-8a52b726ab7d/complete" \
  -H "X-User-Id: us-l1-operator-1" \
  -H "Content-Type: application/json" \
  -d '{
    "comment": "L1 Maker Review: Confirmed high-risk PEP match. Escalating to L1 Checker for validation.",
    "variables": {
      "l1MakerDecision": "TRUE_MATCH",
      "l1MakerComments": "98% confidence match on OFAC SDN List. Customer Jane Smith appears to be legitimate PEP match requiring checker validation.",
      "riskAssessment": "HIGH",
      "recommendedAction": "ESCALATE_TO_CHECKER"
    }
  }'
```

### Response #1 (VALIDATION_FAILED)
```json
{"status":"VALIDATION_FAILED","message":"Please correct the errors and resubmit","processInstanceId":"12b23432-6989-11f0-a69a-8a52b726ab7d","completedAt":1753469845.853339000,"completedBy":"us-l1-operator-1","processActive":true,"attemptNumber":1,"retryTaskId":"345b740a-6989-11f0-a69a-8a52b726ab7d"}
```

### Request #2 (Failed - Still Wrong Format)
```sh
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/345b740a-6989-11f0-a69a-8a52b726ab7d/complete" \
  -H "X-User-Id: us-l1-operator-1" \
  -H "Content-Type: application/json" \
  -d '{
    "comment": "L1 Maker Review: After detailed analysis, confirmed sanctions match. Ready for checker validation.",
    "variables": {
      "l1MakerDecision": "APPROVE",
      "l1MakerComments": "Detailed review completed. Customer matches OFAC SDN list with 98% confidence.",
      "validationStatus": "PASSED"
    }
  }'
```

### Response #2 (VALIDATION_FAILED)
```json
{"status":"VALIDATION_FAILED","message":"Please correct the errors and resubmit","processInstanceId":"12b23432-6989-11f0-a69a-8a52b726ab7d","completedAt":1753470478.233281000,"completedBy":"us-l1-operator-1","processActive":true,"attemptNumber":1,"retryTaskId":"ad4886aa-698a-11f0-a69a-8a52b726ab7d"}
```

### ⚠️ VALIDATION FAILURE ANALYSIS

The BPMN validation script expects:
- **Variable**: `l1MakerDecisions` (array of objects)
- **Format**: `[{"decision": "true_match|false_positive", "comment": "..."}]`
- **Count**: Must match the number of `matches` in the process (1 match = 1 decision)
- **Values**: Only `"true_match"` or `"false_positive"` are allowed

**Database Records Show Bug**: Multiple retry tasks created but L1 Checker task remains in wrong state.

## Claim Task 

### Request

```sh

  curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/24853b8e-6965-11f0-b919-7225c1a185c2/claim" \
      -H "Content-Type: application/json" \
      -H "X-User-Id: us-l1-operator-1" \
      -d '{}'

```