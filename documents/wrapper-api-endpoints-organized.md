# Wrapper API Endpoints - Organized by Controller

## 1. Process Controller
**Tag**: `processes`

### Start Process
`POST /api/processes`
- Start new workflow instance
- Returns process ID and initial tasks

### Get Process Instance
`GET /api/processes/{processInstanceId}`
- Get current state of a process
- Shows active tasks, variables, status

### List Process Definitions
`GET /api/processes/definitions`
- Get available workflow types user can start
- Filtered by user permissions

### Cancel Process
`DELETE /api/processes/{processInstanceId}`
- Cancel/terminate a running process
- Admin/owner only

## 2. Task Controller
**Tag**: `tasks`

### Get Available Tasks (Pull Model)
`GET /api/tasks/available`
- Get OPEN tasks in user's queues
- Query params: queue, processType, priority

### Get My Tasks
`GET /api/tasks/my-tasks`
- Get tasks assigned to current user
- Includes claimed and assigned tasks

### Get Task Details
`GET /api/tasks/{taskId}`
- Get full task details including form data

### Claim Task (Pull Model)
`POST /api/tasks/{taskId}/claim`
- User claims task from queue

### Assign Task (Push Model)
`POST /api/tasks/{taskId}/assign`
- Manager assigns task to specific user
- Body: `{"assignee": "userId"}`

### Complete Task
`POST /api/tasks/{taskId}/complete`
- Complete task with variables
- Returns next tasks created

### Release Task
`POST /api/tasks/{taskId}/release`
- Return claimed task back to queue
- Sets assignee to null

### Get Task Form
`GET /api/tasks/{taskId}/form`
- Get form definition/variables needed

## 3. User Controller
**Tag**: `users`

### Get My Queues
`GET /api/users/me/queues`
- Get queues current user has access to

### Get My Groups
`GET /api/users/me/groups`
- Get groups current user belongs to

### Get My Permissions
`GET /api/users/me/permissions`
- Get what processes user can start
- Get special permissions (assign, cancel, etc.)

### Get User Workload
`GET /api/users/{userId}/workload`
- Get task count by status
- Manager view of team member

### Get Team Members
`GET /api/users/teams/{teamId}/members`
- Get users in a specific team/queue
- For assignment dropdown

## 4. Queue Controller
**Tag**: `queues`

### Get Queue Statistics
`GET /api/queues/{queueName}/stats`
- Task counts by status
- Average processing time
- SLA metrics

### Get Queue Tasks
`GET /api/queues/{queueName}/tasks`
- All tasks in specific queue
- Manager/supervisor view

### Get All Queues
`GET /api/queues`
- List all queues in system
- Admin view

## 5. History Controller
**Tag**: `history`

### Get Completed Tasks
`GET /api/history/tasks`
- Query completed tasks
- Filters: user, date range, process

### Get Process History
`GET /api/history/processes/{processInstanceId}`
- Full audit trail of a process
- Who did what when

### Get Task History
`GET /api/history/tasks/{taskId}`
- When created, claimed, completed
- Variables at each stage

### Get User Activity
`GET /api/history/users/{userId}/activity`
- Tasks completed by user
- Performance metrics

### Search History
`POST /api/history/search`
- Complex queries across history
- Business key, variables, participants

## 6. Admin Controller
**Tag**: `admin`

### Deploy Workflow
`POST /api/admin/workflows/deploy`
- Upload new BPMN file
- Triggers metadata extraction

### List Deployments
`GET /api/admin/workflows/deployments`
- All deployed workflows and versions

### Migrate Process Instances
`POST /api/admin/processes/migrate`
- Move instances to new version

### Reassign Tasks Bulk
`POST /api/admin/tasks/reassign`
- Reassign all tasks from one user to another

### Archive Old Data
`POST /api/admin/archive`
- Move completed items to archive

## Additional Considerations

### Analytics Endpoints (Future)
- `GET /api/analytics/process-metrics`
- `GET /api/analytics/bottlenecks`
- `GET /api/analytics/sla-compliance`

### Integration Endpoints (Future)
- `POST /api/webhooks/register`
- `GET /api/events/subscribe`

### What This Covers
1. **Self-service teams** - claim from queue
2. **Managed teams** - assignment model  
3. **Supervisors** - queue/team views
4. **Admins** - deployment/migration
5. **Audit** - complete history
6. **Analytics** - performance metrics

This should handle any workflow type!