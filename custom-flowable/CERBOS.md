# Custom Flowable Architecture Comprehensive Analysis

## Executive Summary

The custom-flowable system is a sophisticated queue-centric workflow management platform built on Spring Boot that wraps the Flowable BPMN engine with a custom abstraction layer. It implements a Four-Eyes Principle authorization system using Cerbos for fine-grained access control, maintains event-driven synchronization between Flowable and custom database tables, and provides a multi-tenant business application structure with regional user organization.

## 1. Core Architecture & Design Patterns

### Spring Boot Application Structure
- **Main Application**: `/Users/srirajkadimisetty/projects/flowable-documentation/custom-flowable/flowable-wrapper-v2/src/main/java/com/flowable/wrapper/FlowableWrapperApplication.java`
- **Technology Stack**: Spring Boot 3.3.4, Java 21, Flowable 7.1.0, PostgreSQL, Cerbos SDK
- **Architecture Pattern**: Layered architecture with clear separation between controllers, services, repositories, and entities

### Service Layer Architecture
The system follows a clean service-oriented architecture:

1. **TaskService** - Orchestrates task operations and bridges Flowable with queue management
2. **QueueTaskService** - Manages the custom queue abstraction over Flowable tasks
3. **ProcessInstanceService** - Handles workflow instance lifecycle
4. **WorkflowMetadataService** - Manages workflow registration and deployment
5. **UserManagementService** - Handles user and role management
6. **CerbosService** - Provides authorization through Cerbos integration

### Database Schema Design
The system uses a hybrid approach with both Flowable's native tables and custom tables:

**Custom Tables:**
- `users` - User management with JSONB attributes for flexibility
- `business_applications` - Multi-tenant business app structure
- `business_app_roles` - Role definitions per business application
- `user_business_app_roles` - User-role associations
- `workflow_metadata` - Workflow registration and queue mappings
- `queue_tasks` - Custom queue abstraction over Flowable tasks

**Key Design Features:**
- JSONB columns for flexible metadata storage
- Foreign key relationships maintaining referential integrity
- Indexed queries for performance optimization
- Support for multi-tenancy through business applications

## 2. Cerbos Integration & Authorization

### Authorization Architecture
The system implements a sophisticated authorization model using Cerbos:

**Principal Context:**
- User ID, roles, and attributes (region, department, business apps)
- Dynamic role extraction from database
- Regional access control support

**Resource Context:**
- Business application and process definition key
- Process instance context with variables
- Task-specific context including queue information
- Historical task states for Four-Eyes Principle enforcement

### Four-Eyes Principle Implementation
Located in `/Users/srirajkadimisetty/projects/flowable-documentation/custom-flowable/docker/cerbos/policies/resource_policies/workflow/sanctions-management/sanctionsCaseManagement/sanctions-management_sanctions-case-management.yaml`:

```yaml
# Four-Eyes Principle: Bidirectional check
- expr: >
    !(
      (request.resource.attr.currentTask.taskDefinitionKey == "l1_checker_review_task" && 
       has(request.resource.attr.taskStates.l1_maker_review_task) &&
       request.resource.attr.taskStates.l1_maker_review_task.assignee == request.principal.id) ||
      (request.resource.attr.currentTask.taskDefinitionKey == "l1_maker_review_task" && 
       has(request.resource.attr.taskStates.l1_checker_review_task) &&
       request.resource.attr.taskStates.l1_checker_review_task.assignee == request.principal.id)
    )
```

### Authorization Enforcement Points
- **Controller Level**: All endpoints validate authorization before processing
- **Service Level**: CerbosService provides centralized authorization logic
- **Fine-grained Actions**: `claim_task`, `complete_task`, `view_task`, `view_queue`, `start_workflow_instance`

## 3. Queue Management System

### Queue Abstraction Architecture
The system creates a queue-centric view over Flowable's task management:

**Core Components:**
- **QueueTask Entity** (`/Users/srirajkadimisetty/projects/flowable-documentation/custom-flowable/flowable-wrapper-v2/src/main/java/com/flowable/wrapper/entity/QueueTask.java`)
- **QueueTaskService** - Manages queue operations and task lifecycle
- **TaskService** - Orchestrates between Flowable and queue abstraction

### Task Lifecycle Management
```
OPEN -> CLAIMED -> COMPLETED
```

**Key Features:**
- Priority-based task ordering (DESC priority, ASC creation time)
- Assignee tracking with claim/unclaim operations
- Task completion with next task population
- Validation failure detection and retry mechanism

### Candidate Group to Queue Mapping
**Workflow Metadata Structure:**
```json
{
  "candidateGroupMappings": {"managers": "default", "finance": "finance-queue"},
  "taskQueueMappings": [
    {
      "taskId": "approvalTask",
      "taskName": "Approval Task", 
      "candidateGroups": ["managers"],
      "queue": "default"
    }
  ]
}
```

### Event-Driven Synchronization
- **Process Start**: `populateQueueTasksForProcessInstance()` creates queue tasks
- **Task Completion**: Updates existing task and creates new tasks for next steps
- **Validation Failures**: Detects loopback scenarios and provides retry mechanisms

## 4. Workflow Management

### BPMN Workflow Registration Process
1. **Registration**: Store workflow metadata with queue mappings
2. **Deployment**: Deploy BPMN to Flowable engine
3. **Task Mapping Population**: Extract tasks from BPMN and map to queues
4. **Activation**: Mark workflow as available for process instances

### Process Instance Lifecycle
**Workflow Example - Sanctions Case Management:**
```xml
<process id="sanctionsCaseManagement" name="Sanctions Case Management (L1 & L2)">
  <parallelGateway id="l1_parallel_split" name="L1 Split (Maker/Checker)"/>
  <userTask id="l1_maker_review_task" name="Level 1 Maker Review" 
            flowable:candidateGroups="level1-maker"/>
  <userTask id="l1_checker_review_task" name="Level 1 Checker Review" 
            flowable:candidateGroups="level1-checker"/>
</process>
```

### Task Completion and Validation Patterns
**Validation Script Integration:**
- Groovy scripts for business rule validation
- Loopback detection for failed validations
- Attempt counting and error tracking
- Dynamic queue task creation for retry scenarios

## 5. Business Application Context

### Multi-Tenant Structure
**Business Applications:**
- `Sanctions-Management` - L1/L2 sanctions case management
- `Expense-Reimbursement` - Employee expense approval
- `Vacation-Request` - Employee vacation approval

### Role Hierarchy and Permissions
**Role Structure per Business App:**
- **Default Roles**: `deployer`, `workflow-initiator`, `business-user`, `workflow-admin`
- **Specific Roles**: `level1-operator`, `level1-supervisor`, `level2-operator`, `level2-supervisor`

**Permission Model:**
```json
{
  "permissions": ["view", "claim", "complete", "escalate"],
  "level": "L1",
  "queue": "level1-queue"
}
```

### User Management and Regional Organization
**User Attributes:**
```json
{
  "department": "compliance",
  "region": "US",
  "queues": ["level1-queue", "level2-queue"],
  "level": "L1",
  "businessApps": ["Sanctions-Management"]
}
```

**Regional Access Control:**
- Global users can access all regions
- Regional users restricted to their specific region
- Embedded in Cerbos authorization policies

## 6. Key Configuration & Setup

### Docker Compose Architecture
- **Cerbos**: Authorization server on port 3592
- **PostgreSQL**: Database server on port 5430
- **Application**: Spring Boot app on port 8090
- **Network**: Isolated bridge network for service communication

### Database Initialization
**Schema Setup** (`/Users/srirajkadimisetty/projects/flowable-documentation/custom-flowable/flowable-wrapper-v2/src/main/resources/db/schema.sql`):
- Workflow metadata tables with JSONB support
- Queue task management tables
- User and role management tables
- Proper indexing for performance

**Data Seeding** (`/Users/srirajkadimisetty/projects/flowable-documentation/custom-flowable/flowable-wrapper-v2/src/main/resources/db/data-new.sql`):
- Business applications and roles
- Regional users (US, EU, APAC)
- Role assignments based on regional structure

### Application Configuration
**Key Settings** (`/Users/srirajkadimisetty/projects/flowable-documentation/custom-flowable/flowable-wrapper-v2/src/main/resources/application.yml`):
- Flowable engine configuration
- Cerbos endpoint configuration
- Workflow definitions path
- Database connection pooling

### Integration Points
- **Flowable Engine**: Native BPMN processing and task management
- **Cerbos**: External authorization service
- **PostgreSQL**: Persistent storage with JSONB support
- **External Services**: Ready for Keycloak integration (currently disabled)

## 7. Request Flow Architecture

### Complete Request Flow: API → Authorization → Business Logic → Flowable → Database

**Example: Task Claim Operation**

1. **Controller Layer** (`TaskController.claimTask()`):
   - Extract user ID from `X-User-Id` header
   - Path parameters: `businessAppName`, `taskId`

2. **Authorization Layer** (`CerbosService.isAuthorizedForTask()`):
   - Build principal context with user roles and attributes
   - Build resource context with task and process information
   - Evaluate Four-Eyes Principle and queue access rules
   - Return authorization decision

3. **Business Logic Layer** (`TaskService.claimTask()`):
   - Validate task exists and is unassigned
   - Call Flowable engine to claim task
   - Update queue_tasks table with assignee and status

4. **Database Updates**:
   - Flowable updates native task tables
   - Custom queue_tasks table updated with claim information
   - Maintains consistency between both systems

### Event-Driven Synchronization Pattern

**Process Instance Creation:**
```
StartProcessRequest → Authorization Check → Flowable.startProcess() → 
QueueTaskService.populateQueueTasksForProcessInstance() → Queue Tasks Created
```

**Task Completion with Validation:**
```
CompleteTaskRequest → Authorization Check → Flowable.complete() → 
QueueTaskService.completeTask() → Check for Validation Failures → 
Create Retry Tasks if Needed → Return Appropriate Response
```

## 8. Security and Compliance Features

### Authorization Security
- **Fail-Closed Policy**: All authorization failures deny access
- **Fine-Grained Permissions**: Action-specific authorization checks
- **Regional Data Isolation**: Users can only access data from their region
- **Four-Eyes Principle**: Prevents same user from handling maker/checker roles

### Data Security
- **Header-Based Authentication**: User ID passed via HTTP headers
- **Business Application Isolation**: Multi-tenant data separation
- **Audit Trail**: Comprehensive logging of all operations
- **JSONB Security**: Flexible metadata without SQL injection risks

### Compliance Features
- **Workflow Versioning**: Track workflow definition changes
- **Task History**: Complete audit trail of task operations
- **Process Variables**: Store compliance-related data
- **Role-Based Access**: Granular permission system

## Conclusion

The custom-flowable system represents a sophisticated enterprise workflow platform that successfully bridges the gap between Flowable's powerful BPMN engine and business-specific requirements. The queue-centric abstraction, combined with Cerbos-based authorization and Four-Eyes Principle implementation, creates a robust, secure, and compliant workflow management system suitable for complex business processes in regulated industries.

The architecture demonstrates excellent separation of concerns, event-driven design patterns, and comprehensive security measures, making it a solid foundation for enterprise workflow management with advanced authorization requirements.