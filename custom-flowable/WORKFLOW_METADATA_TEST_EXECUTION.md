# Workflow Metadata Controller Test Execution Guide

## Overview
This document provides step-by-step testing for the WorkflowMetadataController with Cerbos authorization integration.

## API Structure
- **Base Path**: `/api/{businessAppName}/workflow-metadata`
- **Business App**: `Sanctions-Management`
- **Authorization**: X-User-Id header required
- **Cerbos Actions**: `register`, `deploy`, `view`

## Test Users and Expected Authorization

### Users for Testing
1. **operation-user-1** (workflow-admin) → Should have ALL permissions
2. **automation-user-2** (deployer) → Should have deploy permissions
3. **us-l1-supervisor-1** (level1-supervisor) → Should have limited/no permissions
4. **us-l1-operator-1** (level1-operator) → Should have no permissions

### Queue Mappings (New Structure)
- **level1-maker** → `level1-queue`
- **level1-checker** → `level1-queue`
- **level1-supervisor** → `level1-supervisor-queue`
- **level2-maker** → `level2-queue`
- **level2-checker** → `level2-queue`
- **level2-supervisor** → `level2-supervisor-queue`

## Test Execution Steps

### Step 1: Register Workflow Metadata

#### Test 1.1: Register as workflow-admin (Should Succeed)
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/register" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: operation-user-1" \
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
    },
    "metadata": {
      "version": "1.0",
      "owner": "compliance-team",
      "created_by": "operation-user-1"
    }
  }'
```
**Expected**: HTTP 201 Created with workflow metadata response

#### Test 1.2: Register as deployer (Should Fail - deployer can deploy but not register)
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
**Expected**: HTTP 403 Forbidden

#### Test 1.3: Register as level1-operator (Should Fail)
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/register" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: us-l1-operator-1" \
  -d '{
    "processDefinitionKey": "testWorkflow",
    "processName": "Test Workflow",
    "businessAppName": "Sanctions-Management", 
    "description": "Test workflow",
    "candidateGroupMappings": {
      "level1-maker": "level1-queue"
    }
  }'
```
**Expected**: HTTP 403 Forbidden

### Step 2: View Workflow Metadata

#### Test 2.1: View as workflow-admin (Should Succeed)
```bash
curl -X GET "http://localhost:8090/api/Sanctions-Management/workflow-metadata/sanctionsCaseManagement" \
  -H "X-User-Id: operation-user-1"
```
**Expected**: HTTP 200 OK with workflow metadata

#### Test 2.2: View as level1-operator (Should Fail)
```bash
curl -X GET "http://localhost:8090/api/Sanctions-Management/workflow-metadata/sanctionsCaseManagement" \
  -H "X-User-Id: us-l1-operator-1"
```
**Expected**: HTTP 403 Forbidden

### Step 3: Deploy Workflow

#### Test 3.1: Deploy as workflow-admin (Should Succeed)
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/deploy" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: operation-user-1" \
  -d '{
    "processDefinitionKey": "sanctionsCaseManagement",
    "bpmnXml": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><definitions><!-- Valid BPMN XML here --></definitions>",
    "deploymentName": "Sanctions Management v1.0"
  }'
```
**Expected**: HTTP 200 OK with deployment response

#### Test 3.2: Deploy as deployer (Should Succeed)
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/deploy" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: automation-user-2" \
  -d '{
    "processDefinitionKey": "sanctionsCaseManagement", 
    "bpmnXml": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><definitions><!-- Valid BPMN XML here --></definitions>",
    "deploymentName": "Sanctions Management v1.0"
  }'
```
**Expected**: HTTP 200 OK with deployment response

#### Test 3.3: Deploy as level1-supervisor (Should Fail)
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/deploy" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: us-l1-supervisor-1" \
  -d '{
    "processDefinitionKey": "sanctionsCaseManagement",
    "bpmnXml": "<?xml version=\"1.0\" encoding=\"UTF-8\"?><definitions><!-- Valid BPMN XML here --></definitions>",
    "deploymentName": "Sanctions Management v1.0"
  }'
```
**Expected**: HTTP 403 Forbidden

### Step 4: Deploy from File

#### Test 4.1: Deploy from file as workflow-admin (Should Succeed)
```bash
curl -X POST "http://localhost:8090/api/Sanctions-Management/workflow-metadata/deploy-from-file?processDefinitionKey=sanctionsCaseManagement&filename=simple-validation-script.bpmn20.xml" \
  -H "X-User-Id: operation-user-1"
```
**Expected**: HTTP 200 OK with deployment response

## Authorization Matrix

| User Role | Register | View | Deploy | Deploy-from-File |
|-----------|----------|------|--------|------------------|
| workflow-admin | ✅ | ✅ | ✅ | ✅ |
| deployer | ❌ | ❌ | ✅ | ✅ |
| level1-supervisor | ❌ | ❌ | ❌ | ❌ |
| level1-operator | ❌ | ❌ | ❌ | ❌ |
| level2-supervisor | ❌ | ❌ | ❌ | ❌ |
| level2-operator | ❌ | ❌ | ❌ | ❌ |

## Cerbos Policy Actions Tested

1. **register** - Registering new workflow metadata
2. **view** - Viewing existing workflow metadata  
3. **deploy** - Deploying BPMN workflows

## Notes

- All requests require `X-User-Id` header
- Business app name is part of the URL path: `/api/{businessAppName}/workflow-metadata`
- Cerbos authorization happens before business logic
- Queue mappings are now properly aligned with supervisor-specific queues
- RegisterWorkflowMetadataRequest requires `businessAppName` field in payload

## Expected Cerbos Policy Behavior

The Cerbos policies should:
1. Allow `workflow-admin` role to perform all actions
2. Allow `deployer` role to perform only `deploy` actions  
3. Deny all other roles for workflow management operations
4. Check business app membership and user attributes
5. Validate queue access permissions based on user queue assignments