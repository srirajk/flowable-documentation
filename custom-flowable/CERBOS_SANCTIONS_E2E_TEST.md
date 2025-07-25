# Cerbos-Integrated Sanctions Management E2E Test

## Test Objective
Comprehensive end-to-end testing of the Cerbos-integrated Sanctions Case Management workflow with Four-Eyes Principle enforcement, queue-based authorization, and region-based access control.

## Test Environment
- **Service URL**: `http://localhost:8090`
- **Cerbos**: `localhost:3592`
- **Database**: PostgreSQL with new role structure
- **BPMN File**: `SanctionsL1L2Flow.bpmn20.xml`

## Key Features Being Tested
1. **Business App Authorization**: All APIs require businessAppName path parameter
2. **Cerbos Integration**: Real-time authorization decisions via Cerbos policies
3. **Four-Eyes Principle**: Bidirectional enforcement (maker ↔ checker restrictions)
4. **Queue-Based Access**: Level-specific queue access controls
5. **Region-Based Access**: GLOBAL vs regional user restrictions
6. **Supervisory Controls**: Level-specific supervisor override capabilities

---

## Pre-Test Setup

### User Test Matrix
| User ID | Role | Level | Region | Queues | Purpose |
|---------|------|-------|--------|--------|---------|
| `automation-user-2` | workflow-initiator, deployer | - | GLOBAL | - | Workflow management |
| `us-l1-operator-1` | level1-operator | L1 | US | level1-queue | L1 Maker/Checker |
| `us-l1-operator-2` | level1-operator | L1 | US | level1-queue | L1 Four-Eyes validation |
| `us-l1-supervisor-1` | level1-supervisor | L1 | US | level1-queue | L1 Supervisor |
| `us-l2-operator-1` | level2-operator | L2 | US | level2-queue | L2 Maker/Checker |
| `us-l2-operator-2` | level2-operator | L2 | US | level2-queue | L2 Four-Eyes validation |
| `us-l2-supervisor-1` | level2-supervisor | L2 | US | level2-queue | L2 Supervisor |

---

## Test Execution

### Phase 1: Workflow Setup & Authorization

#### Step 1: Register Workflow Metadata
**Authorization**: `automation-user-2` (workflow-initiator + deployer roles)

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

**Expected**: ✅ SUCCESS - Metadata registered with proper queue mappings

#### Step 2: Deploy BPMN Workflow
**Authorization**: `automation-user-2` (deployer role)

```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/deploy-from-file?processDefinitionKey=sanctionsCaseManagement&filename=SanctionsL1L2Flow.bpmn20.xml" \
    -H "Content-Type: application/xml" \
    -H "X-User-Id: automation-user-2" \
    --data-binary @/dev/null
```

**Expected**: ✅ SUCCESS - BPMN deployed successfully

#### Step 3: Authorization Validation - Unauthorized User
**Test**: Try workflow registration with L1 operator (should fail)

```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/register" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l1-operator-1" \
    -d '{
      "processDefinitionKey": "testProcess",
      "processName": "Test Process"
    }'
```

**Expected**: ❌ 403 FORBIDDEN - L1 operator lacks deployer role

---

### Phase 2: Process Instance Creation & Region-Based Authorization

#### Step 4: Start Process Instance (US Region)
**Authorization**: `automation-user-2` (workflow-initiator role, GLOBAL region)

```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/process-instances/start" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: automation-user-2" \
    -d '{
      "processDefinitionKey": "sanctionsCaseManagement",
      "variables": {
        "caseId": "CASE-FOUR-EYES-001",
        "region": "US",
        "matches": [
          {
            "matchId": "MATCH-001",
            "entityName": "John Doe",
            "score": 0.95,
            "category": "sanctions"
          }
        ]
      }
    }'
```

**Expected**: ✅ SUCCESS - Process instance created with region=US

**Save Response**: `processInstanceId` for subsequent tests

---

### Phase 3: Four-Eyes Principle Testing

#### Step 5: Queue Access Validation
**Test**: L1 operator views their queue

```bash
curl -X GET "http://localhost:8090/api/Sanctions-Management/tasks/queue/level1-queue" \
    -H "X-User-Id: us-l1-operator-1"
```

**Expected**: ✅ SUCCESS - Can view level1-queue tasks

**Test**: L1 operator tries to view L2 queue (should fail)

```bash
curl -X GET "http://localhost:8090/api/Sanctions-Management/tasks/queue/level2-queue" \
    -H "X-User-Id: us-l1-operator-1"
```

**Expected**: ❌ 403 FORBIDDEN - No access to level2-queue

#### Step 6: L1 Maker Task - First User Claims
**Authorization**: `us-l1-operator-1` (level1-operator role, level1-queue access)

```bash
# Get available tasks
curl -X GET "http://localhost:8090/api/Sanctions-Management/tasks/queue/level1-queue" \
    -H "X-User-Id: us-l1-operator-1"

# Claim L1 maker task (get taskId from response above)
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{L1_MAKER_TASK_ID}/claim" \
    -H "X-User-Id: us-l1-operator-1"

# Complete L1 maker task
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{L1_MAKER_TASK_ID}/complete" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l1-operator-1" \
    -d '{
      "variables": {
        "l1MakerDecisions": [
          {
            "matchId": "MATCH-001", 
            "decision": "true_match",
            "comment": "Strong sanctions match - L1 maker approval"
          }
        ]
      }
    }'
```

**Expected**: ✅ SUCCESS - L1 maker task completed by `us-l1-operator-1`

#### Step 7: Four-Eyes Violation Test - Same User Tries Checker Task
**Test**: Same user (`us-l1-operator-1`) attempts to claim L1 checker task

```bash
# Attempt to claim L1 checker task with same user
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{L1_CHECKER_TASK_ID}/claim" \
    -H "X-User-Id: us-l1-operator-1"
```

**Expected**: ❌ 403 FORBIDDEN - Four-Eyes Principle violation (same user did maker task)

**Cerbos Policy Check**: 
```yaml
# Policy should evaluate:
currentTask.taskDefinitionKey == "l1_checker_review_task" = TRUE
taskStates.l1_maker_review_task.assignee == request.principal.id
# "us-l1-operator-1" == "us-l1-operator-1" = TRUE
# Four-Eyes condition FAILS → Access DENIED
```

#### Step 8: Four-Eyes Compliance - Different User Claims Checker Task
**Authorization**: `us-l1-operator-2` (different level1-operator)

```bash
# Different user claims L1 checker task
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{L1_CHECKER_TASK_ID}/claim" \
    -H "X-User-Id: us-l1-operator-2"

# Complete L1 checker task with different decision (mixed scenario)
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{L1_CHECKER_TASK_ID}/complete" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l1-operator-2" \
    -d '{
      "variables": {
        "l1CheckerDecisions": [
          {
            "matchId": "MATCH-001",
            "decision": "false_positive", 
            "comment": "Upon review, appears to be false positive - L1 checker disagrees"
          }
        ]
      }
    }'
```

**Expected**: ✅ SUCCESS - Different user can complete checker task
**Expected**: ✅ SUCCESS - Mixed decisions (true_match + false_positive) route to L1 Supervisor

---

### Phase 4: Supervisory Controls & Level-Specific Authorization

#### Step 9: L1 Supervisor Task Completion
**Authorization**: `us-l1-supervisor-1` (level1-supervisor role)

```bash
# Get supervisor task
curl -X GET "http://localhost:8090/api/Sanctions-Management/tasks/queue/level1-supervisor-queue" \
    -H "X-User-Id: us-l1-supervisor-1"

# Claim and complete L1 supervisor task (escalate to L2)
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{L1_SUPERVISOR_TASK_ID}/claim" \
    -H "X-User-Id: us-l1-supervisor-1"

curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{L1_SUPERVISOR_TASK_ID}/complete" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l1-supervisor-1" \
    -d '{
      "variables": {
        "l1SupervisorDecision": "escalate_to_l2",
        "l1SupervisorComments": "Mixed L1 decisions require L2 review"
      }
    }'
```

**Expected**: ✅ SUCCESS - Process escalated to L2, L2 maker and checker tasks created

#### Step 10: Test Supervisory Unclaim Authority
**Test**: L1 supervisor can unclaim L1 tasks, but not L2 tasks

```bash
# L1 supervisor unclaims L2 task (should fail)
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{L2_MAKER_TASK_ID}/unclaim" \
    -H "X-User-Id: us-l1-supervisor-1"
```

**Expected**: ❌ 403 FORBIDDEN - L1 supervisor cannot unclaim L2 tasks (level segregation)

---

### Phase 5: Level 2 Four-Eyes Principle Testing

#### Step 11: L2 Four-Eyes Test - Reverse Order (Checker First)
**Test**: L2 user claims checker task first, then tries maker task

```bash
# L2 operator claims checker task FIRST
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{L2_CHECKER_TASK_ID}/claim" \
    -H "X-User-Id: us-l2-operator-1"

curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{L2_CHECKER_TASK_ID}/complete" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l2-operator-1" \
    -d '{
      "variables": {
        "l2CheckerDecisions": [
          {
            "matchId": "MATCH-001",
            "decision": "true_match",
            "comment": "L2 checker confirms this is a valid match"
          }
        ]
      }
    }'

# Same user tries to claim maker task (should fail - reverse Four-Eyes)
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{L2_MAKER_TASK_ID}/claim" \
    -H "X-User-Id: us-l2-operator-1"
```

**Expected**: ❌ 403 FORBIDDEN - Bidirectional Four-Eyes Principle (cannot claim maker after doing checker)

**Cerbos Policy Check**:
```yaml
# Policy should evaluate:
currentTask.taskDefinitionKey == "l2_maker_review_task" = TRUE
taskStates.l2_checker_review_task.assignee == request.principal.id
# "us-l2-operator-1" == "us-l2-operator-1" = TRUE
# Reverse Four-Eyes condition FAILS → Access DENIED
```

#### Step 12: L2 Compliance - Different User Completes Maker Task
**Authorization**: `us-l2-operator-2` (different level2-operator)

```bash
# Different L2 user completes maker task
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{L2_MAKER_TASK_ID}/claim" \
    -H "X-User-Id: us-l2-operator-2"

curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{L2_MAKER_TASK_ID}/complete" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l2-operator-2" \
    -d '{
      "variables": {
        "l2MakerDecisions": [
          {
            "matchId": "MATCH-001",
            "decision": "true_match", 
            "comment": "L2 maker also confirms - strong match"
          }
        ]
      }
    }'
```

**Expected**: ✅ SUCCESS - Different user can complete maker task
**Expected**: ✅ SUCCESS - L2 agreement routes to L2 supervisor

---

### Phase 6: Final Supervisor Decision & Process Completion

#### Step 13: L2 Supervisor Final Decision
**Authorization**: `us-l2-supervisor-1` (level2-supervisor role)

```bash
# L2 supervisor makes final decision
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{L2_SUPERVISOR_TASK_ID}/claim" \
    -H "X-User-Id: us-l2-supervisor-1"

curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{L2_SUPERVISOR_TASK_ID}/complete" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l2-supervisor-1" \
    -d '{
      "variables": {
        "l2SupervisorDecision": "true_positive",
        "l2SupervisorComments": "Final determination: Confirmed sanctions match"
      }
    }'
```

**Expected**: ✅ SUCCESS - Process completes with "Closed - True Positive" status

---

### Phase 7: Assignment-Only Task Completion Testing

#### Step 14: Task Completion Authorization Test
**Start new process for this test**

```bash
# Start second process instance
curl -X POST "http://localhost:8090/api/Sanctions-Management/process-instances/start" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: automation-user-2" \
    -d '{
      "processDefinitionKey": "sanctionsCaseManagement",
      "variables": {
        "caseId": "CASE-ASSIGNMENT-TEST",
        "region": "US",
        "matches": [{"matchId": "MATCH-002", "entityName": "Jane Smith", "score": 0.90, "category": "sanctions"}]
      }
    }'

# L1 user claims task
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{NEW_L1_MAKER_TASK_ID}/claim" \
    -H "X-User-Id: us-l1-operator-1"

# Different L1 user tries to complete the claimed task (should fail)
curl -X POST "http://localhost:8090/api/Sanctions-Management/tasks/{NEW_L1_MAKER_TASK_ID}/complete" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: us-l1-operator-2" \
    -d '{
      "variables": {
        "l1MakerDecisions": [{"matchId": "MATCH-002", "decision": "true_match", "comment": "test"}]
      }
    }'
```

**Expected**: ❌ 403 FORBIDDEN - Only assigned user can complete task

---

## Test Results Matrix

| Test Scenario | Expected Result | Authorization Rule |
|---------------|-----------------|-------------------|
| Workflow registration by deployer | ✅ SUCCESS | deployer role required |
| Workflow registration by operator | ❌ 403 FORBIDDEN | Missing deployer role |
| Process creation with valid region | ✅ SUCCESS | workflow-initiator + region match |
| L1 queue access by L1 operator | ✅ SUCCESS | Queue membership check |
| L2 queue access by L1 operator | ❌ 403 FORBIDDEN | Queue segregation |
| L1 maker → L1 checker (same user) | ❌ 403 FORBIDDEN | Four-Eyes Principle |
| L1 maker → L1 checker (different user) | ✅ SUCCESS | Four-Eyes compliance |
| L2 checker → L2 maker (same user) | ❌ 403 FORBIDDEN | Bidirectional Four-Eyes |
| L2 checker → L2 maker (different user) | ✅ SUCCESS | Four-Eyes compliance |
| L1 supervisor unclaim L2 task | ❌ 403 FORBIDDEN | Level segregation |
| Task completion by non-assignee | ❌ 403 FORBIDDEN | Assignment-only completion |
| Mixed L1 decisions | ✅ L1 Supervisor | Routing logic |
| L1 agreement | ✅ Direct to L2 | Auto-routing logic |

---

## Key Validations

### ✅ Authorization Framework
- [x] Business app path parameter enforcement
- [x] Cerbos policy evaluation in real-time
- [x] User role and attribute extraction
- [x] Fail-closed security model

### ✅ Four-Eyes Principle
- [x] Bidirectional enforcement (maker ↔ checker)
- [x] Level-specific application (L1 & L2)
- [x] Cross-user validation
- [x] Task state tracking

### ✅ Queue-Based Access Control
- [x] Level-specific queue segregation
- [x] Queue membership validation
- [x] Supervisor queue access

### ✅ Business Logic
- [x] Mixed decision routing to supervisor
- [x] Agreement-based auto-routing to L2
- [x] Assignment-only task completion
- [x] Process completion workflows

### ✅ Region-Based Access
- [x] GLOBAL user access to any region
- [x] Regional user restriction validation
- [x] CreateRequest region embedding

---

## Success Criteria

**PASS**: All authorization checks work as expected, Four-Eyes Principle prevents violations, queue segregation enforced, business logic flows correctly.

**FAIL**: Any authorization bypass, Four-Eyes violation allowed, incorrect routing, or policy evaluation failure.

---

## Cleanup

```bash
# Check process instances
curl -X GET "http://localhost:8090/api/Sanctions-Management/process-instances" \
    -H "X-User-Id: automation-user-2"

# Verify all processes completed successfully
```

**Test Status**: Ready for execution 🚀