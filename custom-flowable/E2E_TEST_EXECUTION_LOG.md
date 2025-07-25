# E2E Test Execution Log - Sanctions Management Workflow

## Test Environment
- **Date**: 2025-01-20
- **Application**: http://localhost:8090
- **Workflow**: sanctionsCaseManagement (Sanctions L1-L2 Flow)

---

## ✅ Successful Executions

### 1. Register Workflow Metadata
```bash
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

**Response:**
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
  "deployed": false
}
```

---

### 2. Deploy BPMN Workflow
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/deploy-from-file?processDefinitionKey=sanctionsCaseManagement&filename=SanctionsL1L2Flow.bpmn20.xml" \
    -H "X-User-Id: automation-user-2"
```

**Response:**
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
      "candidateGroups": ["level1-maker"],
      "queue": "level1-queue"
    },
    {
      "taskId": "l1_checker_review_task", 
      "taskName": "Level 1 Checker Review",
      "candidateGroups": ["level1-checker"],
      "queue": "level1-queue"
    },
    {
      "taskId": "l1_supervisor_task",
      "taskName": "Level 1 Supervisor Review", 
      "candidateGroups": ["level1-supervisor"],
      "queue": "level1-supervisor-queue"
    },
    {
      "taskId": "l2_maker_review_task",
      "taskName": "Level 2 Maker Review",
      "candidateGroups": ["level2-maker"],
      "queue": "level2-queue"  
    },
    {
      "taskId": "l2_checker_review_task",
      "taskName": "Level 2 Checker Review",
      "candidateGroups": ["level2-checker"],
      "queue": "level2-queue"
    },
    {
      "taskId": "l2_supervisor_task",
      "taskName": "Level 2 Supervisor Review",
      "candidateGroups": ["level2-supervisor"], 
      "queue": "level2-supervisor-queue"
    }
  ],
  "deployed": true,
  "deploymentId": "d18c1c13-68f2-11f0-9205-0242ac1b0004"
}
```

---

### 3. Start Process Instance
```bash
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

**Response:**
```json
{
  "processInstanceId": "57faef66-68f3-11f0-9205-0242ac1b0004",
  "processDefinitionId": "sanctionsCaseManagement:1:d1a65ad5-68f2-11f0-9205-0242ac1b0004",
  "processDefinitionKey": "sanctionsCaseManagement",
  "processDefinitionName": "Sanctions Case Management (L1 & L2)",
  "businessKey": "SC001",
  "startTime": 1753405481.352011305,
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
  }
}
```

---

### 4. View L1 Queue Tasks (Authorization Success)
```bash
curl -X GET "http://localhost:8090/api/Sanctions-Management/tasks/queue/level1-queue" \
    -H "X-User-Id: us-l1-operator-1"
```

**Response:**
```json
[
  {
    "taskId": "58063a24-68f3-11f0-9205-0242ac1b0004",
    "processInstanceId": "57faef66-68f3-11f0-9205-0242ac1b0004",
    "processDefinitionKey": "sanctionsCaseManagement",
    "taskDefinitionKey": "l1_maker_review_task",
    "taskName": "Level 1 Maker Review",
    "queueName": "level1-queue",
    "assignee": null,
    "status": "OPEN",
    "priority": 50,
    "createdAt": 1753405481.327288000,
    "claimedAt": null,
    "completedAt": null,
    "taskData": {
      "owner": null,
      "dueDate": null,
      "formKey": null,
      "createTime": 1753405481133,
      "description": "Review all matches for Case ID: SC001.\n                Provide a decision (true_match / false_positive) and comments for every match.",
      "taskDefinitionKey": "l1_maker_review_task"
    },
    "businessKey": "SC001"
  },
  {
    "taskId": "5806d667-68f3-11f0-9205-0242ac1b0004",
    "processInstanceId": "57faef66-68f3-11f0-9205-0242ac1b0004",
    "processDefinitionKey": "sanctionsCaseManagement",
    "taskDefinitionKey": "l1_checker_review_task",
    "taskName": "Level 1 Checker Review",
    "queueName": "level1-queue",
    "assignee": null,
    "status": "OPEN",
    "priority": 50,
    "createdAt": 1753405481.345220000,
    "claimedAt": null,
    "completedAt": null,
    "taskData": {
      "owner": null,
      "dueDate": null,
      "formKey": null,
      "createTime": 1753405481167,
      "description": "Review all matches for Case ID: SC001.\n                Provide a decision (true_match / false_positive) and comments for every match.",
      "taskDefinitionKey": "l1_checker_review_task"
    },
    "businessKey": "SC001"
  }
]
```

---

## ❌ Failed Authorization

### 5. Claim L1 Maker Task (Authorization Failed)
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/58063a24-68f3-11f0-9205-0242ac1b0004/claim" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l1-operator-1" \
    -d '{}'
```

**Response:** HTTP 403 (No response body)

**Authorization Logs:**
```
2025-07-24 21:07:01 - Built Cerbos resource context: kind=Sanctions-Management::sanctionsCaseManagement, id=57faef66-68f3-11f0-9205-0242ac1b0004
2025-07-24 21:07:02 - Principal: id=us-l1-operator-1, roles=[level1-operator]
2025-07-24 21:07:02 - Resource: kind=Sanctions-Management::sanctionsCaseManagement, id=57faef66-68f3-11f0-9205-0242ac1b0004
2025-07-24 21:07:02 - Action: claim_task
2025-07-24 21:07:02 - Business App: Sanctions-Management
2025-07-24 21:07:02 - Process Definition Key: sanctionsCaseManagement
2025-07-24 21:07:02 - Process Instance ID: 57faef66-68f3-11f0-9205-0242ac1b0004
2025-07-24 21:07:02 - Task States: [l1_checker_review_task, l1_maker_review_task]
2025-07-24 21:07:02 - User Roles: [level1-operator]
2025-07-24 21:07:02 - User Attributes: {level=L1, queues=[level1-queue], region=US, department=compliance}
2025-07-24 21:07:02 - Cerbos authorization result: allowed=false
2025-07-24 21:07:02 - User us-l1-operator-1 unauthorized to claim task: 58063a24-68f3-11f0-9205-0242ac1b0004
```

---

## Analysis of Authorization Failure

### Key Observations:
1. **Queue Access Works**: User can view tasks in `level1-queue` ✅
2. **Task Claim Fails**: Same user cannot claim `l1_maker_review_task` ❌
3. **User Context**: `us-l1-operator-1` has role `level1-operator` and queue access to `level1-queue`
4. **Task Context**: Task `l1_maker_review_task` has candidate group `level1-maker`

### Potential Issues:
1. **Role Mismatch**: User has `level1-operator` role but task requires `level1-maker` candidate group
2. **Cerbos Policy**: May require specific role mapping for task claiming vs queue viewing
3. **Current Task Context**: Missing `currentTask` attribute in resource context for Cerbos evaluation

### Next Steps:
1. Check if user needs `level1-maker` role assignment instead of `level1-operator`
2. Review Cerbos policy for `claim_task` action requirements
3. Verify task-to-role mapping in the database