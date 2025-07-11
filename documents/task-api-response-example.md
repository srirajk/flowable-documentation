# Task API Response - Candidate Groups

## Query Tasks API Response

When you query tasks, Flowable does NOT return candidate groups by default in the task list. Here's what you get:

### Basic Task Query
```bash
GET /runtime/tasks
```

**Response:**
```json
{
  "data": [{
    "id": "task-123",
    "name": "Manager Review",
    "assignee": null,
    "createTime": "2024-01-15T10:00:00Z",
    "dueDate": null,
    "priority": 50,
    "suspended": false,
    "taskDefinitionKey": "managerReview",
    "processInstanceId": "proc-456",
    "processDefinitionId": "purchaseOrder:1:789",
    "variables": []
    // NOTE: candidateGroups NOT included here!
  }]
}
```

## Getting Candidate Groups

### Option 1: Get Individual Task Details
```bash
GET /runtime/tasks/{taskId}
```

**Response includes identity links:**
```json
{
  "id": "task-123",
  "name": "Manager Review",
  "assignee": null,
  "identityLinks": [
    {
      "type": "candidate",
      "groupId": "managers"
    },
    {
      "type": "candidate", 
      "groupId": "supervisors"
    }
  ]
}
```

### Option 2: Query Identity Links
```bash
GET /runtime/tasks/{taskId}/identitylinks
```

**Response:**
```json
[
  {
    "url": "http://localhost:8080/flowable-rest/service/runtime/tasks/task-123/identitylinks/groups/managers/candidate",
    "user": null,
    "group": "managers",
    "type": "candidate"
  },
  {
    "url": "http://localhost:8080/flowable-rest/service/runtime/tasks/task-123/identitylinks/groups/supervisors/candidate",
    "user": null,
    "group": "supervisors", 
    "type": "candidate"
  }
]
```

### Option 3: Query Tasks by Candidate Group
```bash
GET /runtime/tasks?candidateGroup=managers
```

This returns all tasks where "managers" is a candidate group, but still doesn't show ALL candidate groups in the response.

## Important Notes

1. **Task List Queries** don't return candidate groups for performance reasons
2. **Individual Task Details** include identity links (which contain groups)
3. **Identity Links API** gives you the full list of candidate users and groups

## Workaround for Your Wrapper API

Your wrapper could:

1. **Enrich task data** by fetching identity links:
```javascript
async function getTasksWithGroups(query) {
  // Get tasks
  const tasks = await flowableApi.getTasks(query);
  
  // Enrich each with groups
  for (const task of tasks.data) {
    const identityLinks = await flowableApi.getTaskIdentityLinks(task.id);
    task.candidateGroups = identityLinks
      .filter(link => link.type === 'candidate' && link.group)
      .map(link => link.group);
  }
  
  return tasks;
}
```

2. **Cache group information** when tasks are created via events

3. **Use process variables** to track groups:
```xml
<!-- Store groups as process variable too -->
<scriptTask id="storeGroups">
  <script>
    execution.setVariable('taskGroups_managerReview', 'managers,supervisors');
  </script>
</scriptTask>
```

## Best Practice for Your Wrapper

Since you're building a queue-centric wrapper:
1. Listen to task creation events
2. Query identity links when task is created
3. Store in your wrapper's database with the queue assignment
4. Users query YOUR API which has all the enriched data

This way you don't need to make multiple Flowable API calls every time someone views their queue!