# Flowable Wrapper Entitlement Tables - Database Design

## Overview
This document outlines the database schema for user entitlements and authorization in the Flowable Wrapper API with Cerbos integration.

## Database Schema ASCII Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                             ENTITLEMENT SYSTEM DESIGN                               │
└─────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────┐    ┌──────────────────────────┐    ┌─────────────────────────┐
│        USERS            │    │   BUSINESS_APPLICATIONS  │    │  BUSINESS_APP_ROLES     │
├─────────────────────────┤    ├──────────────────────────┤    ├─────────────────────────┤
│ id (PK)                 │    │ id (PK)                  │    │ id (PK)                 │
│ username                │    │ business_app_name (UK)   │    │ business_app_id (FK)    │
│ email                   │    │ description              │    │ role_name               │
│ first_name              │    │ metadata (JSON)          │    │ role_display_name       │
│ last_name               │    │ is_active                │    │ description             │
│ is_active               │    │ created_at               │    │ metadata (JSON)         │
│ attributes (JSON)       │    │ updated_at               │    │ is_active               │
│ created_at              │    └──────────────────────────┘    │ created_at              │
│ updated_at              │                │                   │ updated_at              │
└─────────────────────────┘                │                   └─────────────────────────┘
           │                               │                              │
           │                               └──────────────┐               │
           │                                              │               │
           └────────────────┐                             │               │
                            │                             │               │
                            ▼                             ▼               ▼
                ┌─────────────────────────────────────────────────────────────────┐
                │               USER_BUSINESS_APP_ROLES                          │
                ├─────────────────────────────────────────────────────────────────┤
                │ id (PK)                                                        │
                │ user_id (FK) → USERS.id                                       │
                │ business_app_role_id (FK) → BUSINESS_APP_ROLES.id             │
                │ is_active                                                      │
                │ assigned_at                                                    │
                │ assigned_by                                                    │
                └─────────────────────────────────────────────────────────────────┘
```

## Table Relationships

### 1. USERS Table
**Purpose**: Core user identity and attributes
```
┌─────────────────────────┐
│ USERS                   │
├─────────────────────────┤
│ id: "us-l1-operator-1"  │
│ username: "us-l1-op-1"  │
│ email: "user@comp.com"  │
│ attributes: {           │
│   "department": "comp", │
│   "region": "US",       │
│   "queues": ["l1-q"],   │
│   "level": "L1"         │
│ }                       │
└─────────────────────────┘
```

### 2. BUSINESS_APPLICATIONS Table  
**Purpose**: Define business applications (workflow domains)
```
┌────────────────────────────────┐
│ BUSINESS_APPLICATIONS          │
├────────────────────────────────┤
│ business_app_name:             │
│   "Sanctions-Management"       │
│   "Expense-Reimbursement"      │
│   "Vacation-Request"           │
│                                │
│ metadata: {                    │
│   "processDefinitionKey":      │
│     "sanctionsCaseManagement", │
│   "owner": "compliance-team"   │
│ }                              │
└────────────────────────────────┘
```

### 3. BUSINESS_APP_ROLES Table
**Purpose**: Define roles within each business application
```
┌─────────────────────────────────────────────────────────────────────────┐
│ BUSINESS_APP_ROLES                                                      │
├─────────────────────────────────────────────────────────────────────────┤
│ Sanctions-Management:                                                   │
│   ├── workflow-admin     (deploy, manage, admin, view, claim, complete) │
│   ├── workflow-initiator (start_workflow_instance, read_workflow_inst.) │  
│   ├── deployer          (deploy, register)                              │
│   ├── business-user     (view)                                          │
│   ├── level1-operator   (view, claim, complete) + L1 queues             │
│   ├── level1-supervisor (view, claim, complete, escalate) + L1 sup.     │
│   ├── level2-operator   (view, claim, complete) + L2 queues             │
│   └── level2-supervisor (view, claim, complete, finalize) + L2 sup.     │
│                                                                         │
│ Expense-Reimbursement:                                                  │
│   ├── workflow-admin, deployer, workflow-initiator, business-user       │
│   └── (future expense-specific roles)                                   │
│                                                                         │
│ Vacation-Request:                                                       │
│   ├── workflow-admin, deployer, workflow-initiator, business-user       │
│   └── (future vacation-specific roles)                                  │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4. USER_BUSINESS_APP_ROLES Table (Junction)
**Purpose**: Assign users to specific roles within business applications
```
┌──────────────────────────────────────────────────────────────────────────┐
│ USER_BUSINESS_APP_ROLES (Junction Table)                               │
├──────────────────────────────────────────────────────────────────────────┤
│ Examples:                                                                │
│                                                                          │
│ operation-user-1 → Sanctions-Management.workflow-admin                  │
│ automation-user-2 → Sanctions-Management.deployer                       │
│ automation-user-2 → Sanctions-Management.workflow-initiator             │
│                                                                          │
│ us-l1-operator-1 → Sanctions-Management.level1-operator                 │
│ us-l1-supervisor-1 → Sanctions-Management.level1-supervisor             │
│ us-l2-operator-1 → Sanctions-Management.level2-operator                 │
│ us-l2-supervisor-1 → Sanctions-Management.level2-supervisor             │
│                                                                          │
│ eu-l1-operator-1 → Sanctions-Management.level1-operator                 │
│ apac-l2-supervisor-1 → Sanctions-Management.level2-supervisor           │
└──────────────────────────────────────────────────────────────────────────┘
```

## Data Flow for Authorization

### Step 1: User Request
```
HTTP Request: X-User-Id: us-l1-operator-1
Action: start_workflow_instance
Business App: Sanctions-Management
```

### Step 2: Database Query
```sql
SELECT u.*, ba.business_app_name, bar.role_name, bar.metadata
FROM users u
JOIN user_business_app_roles ubar ON u.id = ubar.user_id  
JOIN business_app_roles bar ON ubar.business_app_role_id = bar.id
JOIN business_applications ba ON bar.business_app_id = ba.id
WHERE u.id = 'us-l1-operator-1' 
  AND ba.business_app_name = 'Sanctions-Management'
  AND ubar.is_active = true;
```

### Step 3: Cerbos Principal Construction
```json
{
  "principal": {
    "id": "us-l1-operator-1",
    "roles": ["level1-operator"],
    "attr": {
      "businessApps": ["Sanctions-Management"],
      "department": "compliance", 
      "region": "US",
      "queues": ["level1-queue"],
      "level": "L1"
    }
  }
}
```

### Step 4: Resource Context
```json
{
  "resource": {
    "kind": "Sanctions-Management::sanctionsCaseManagement",
    "attr": {
      "businessApp": "Sanctions-Management",
      "processDefinitionKey": "sanctionsCaseManagement",
      "createRequest": {
        "region": "US",
        "caseId": "CASE-123"
      }
    }
  }
}
```

### Step 5: Policy Evaluation
```yaml
# Policy: sanctions-management_sanctions-case-management.yaml
condition:
  match:
    all:
      of:
        - expr: request.resource.attr.businessApp in request.principal.attr.businessApps
        - expr: request.principal.attr.region == "GLOBAL" || 
                request.principal.attr.region == request.resource.attr.createRequest.region
```

## Key Design Principles

### 1. **Multi-Tenancy via Business Applications**
- Each business application is a separate authorization domain
- Users can have different roles in different business applications
- Policies are specific to business app + workflow combinations

### 2. **Role-Based Access Control (RBAC)**
- Default roles: workflow-admin, deployer, workflow-initiator, business-user
- Workflow-specific roles: level1-operator, level2-supervisor, etc.
- Roles define permissions within business applications

### 3. **Attribute-Based Access Control (ABAC)**
- User attributes: region, department, queues, level
- Resource attributes: businessApp, processDefinitionKey, createRequest
- Dynamic authorization based on attribute matching

### 4. **Regional Access Control**
- GLOBAL users can access any region
- Regional users can only access their assigned region
- Region validation during workflow instance creation

### 5. **Queue-Based Task Assignment**
- Users have queue access defined in attributes
- Tasks are routed to queues based on workflow metadata
- Authorization checks queue membership for task operations

## Future Extensibility

### Additional Business Applications
```
Business Apps:
├── Contract-Management
├── Risk-Assessment  
├── Customer-Onboarding
└── Audit-Workflow
```

### Additional Role Types
```
Role Categories:
├── System Roles (workflow-admin, deployer)
├── Workflow Roles (level1-operator, approver) 
├── Regional Roles (us-manager, eu-supervisor)
└── Functional Roles (analyst, reviewer, auditor)
```

### Additional Attributes
```
User Attributes:
├── costCenter, managerId, clearanceLevel
├── businessUnit, team, location
└── workingHours, timezone, languages

Resource Attributes:  
├── priority, amount, riskLevel
├── customerType, geography, sensitivity
└── complianceFlags, auditRequired
```