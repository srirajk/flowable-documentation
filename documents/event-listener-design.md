# Event Listener Design for Queue Management

## Core Requirement
Maintain queue consistency with Flowable tasks using same-transaction event listeners.

## Queue Event Listeners (Same Transaction)

```java
@Component
@Slf4j
public class TaskQueueEventListener {
    
    @Autowired
    private QueueRepository queueRepository;
    
    @Autowired
    private ProcessMetadataService metadataService;
    
    // CRITICAL: Runs in SAME transaction as Flowable operation
    @EventListener
    public void handleTaskCreated(FlowableEngineEntityEvent event) {
        if (event.getEntity() instanceof TaskEntity) {
            TaskEntity task = (TaskEntity) event.getEntity();
            
            // Get candidate groups from cached metadata
            List<String> groups = metadataService.getCandidateGroups(
                task.getProcessDefinitionId(), 
                task.getTaskDefinitionKey()
            );
            
            // Create queue entries - if this fails, task creation rolls back
            for (String group : groups) {
                QueueTask queueTask = new QueueTask();
                queueTask.setTaskId(task.getId());
                queueTask.setQueueName(group);
                queueTask.setStatus("OPEN");
                queueRepository.save(queueTask);
            }
        }
    }
    
    @EventListener
    public void handleTaskCompleted(FlowableActivityCompletedEvent event) {
        if ("userTask".equals(event.getActivityType())) {
            // Remove from queue - same transaction
            queueRepository.deleteByTaskId(event.getActivityId());
        }
    }
}
```

## Audit/Logging Listeners (After Commit)

```java
@Component
@Slf4j
public class AuditEventListener {
    
    // Runs AFTER transaction commits - won't affect business process
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void logTaskCreated(FlowableEngineEntityEvent event) {
        if (event.getEntity() instanceof TaskEntity) {
            TaskEntity task = (TaskEntity) event.getEntity();
            log.info("Task created: {} in process {}", 
                task.getName(), task.getProcessInstanceId());
        }
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void logProcessCompleted(FlowableEngineEntityEvent event) {
        // Log for audit, send notifications, etc.
        // If this fails, process is already completed
    }
}
```

## Why This Works

With embedded Flowable:
1. Your service method starts Spring transaction
2. Flowable operations join this transaction
3. `@EventListener` runs in same transaction
4. Everything commits or rolls back together

```java
@Service
@Transactional
public class TaskService {
    public void completeTask(String taskId, Map<String, Object> variables) {
        // 1. Spring transaction starts
        // 2. Complete task in Flowable
        taskService.complete(taskId, variables);
        // 3. Event fires - queue updated in SAME transaction
        // 4. All commits together or all rolls back
    }
}
```

## Key Points
- Queue updates MUST be in same transaction (consistency)
- Logging/audit can be after commit (won't break process)
- No dual-write problem - it's all or nothing
- Pod crashes won't leave inconsistent state