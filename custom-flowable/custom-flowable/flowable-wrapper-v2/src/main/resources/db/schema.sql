-- Workflow Metadata Table
-- Stores metadata about workflow types and their queue mappings
CREATE TABLE IF NOT EXISTS workflow_metadata (
    id SERIAL PRIMARY KEY,
    process_definition_key VARCHAR(255) NOT NULL UNIQUE,
    process_name VARCHAR(255) NOT NULL,
    description TEXT,
    version INTEGER NOT NULL DEFAULT 1,
    
    -- JSONB column for candidate group to queue mappings
    -- Format: {"managers": "default", "finance": "finance-queue"}
    candidate_group_mappings JSONB NOT NULL,
    
    -- JSONB column for task queue mappings (populated after deployment)
    -- Format: [{"taskId": "approvalTask", "taskName": "Approval Task", "candidateGroups": ["managers"], "queue": "default"}]
    task_queue_mappings JSONB,
    
    -- Additional metadata
    metadata JSONB,
    
    -- Deployment tracking
    deployed BOOLEAN NOT NULL DEFAULT false,
    deployment_id VARCHAR(255),
    
    -- Audit fields
    active BOOLEAN NOT NULL DEFAULT true,
    created_by VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster lookups
CREATE INDEX idx_workflow_metadata_process_key ON workflow_metadata(process_definition_key);
CREATE INDEX idx_workflow_metadata_active ON workflow_metadata(active);

-- Queue Tasks Table (simplified for now)
-- Will be populated by event listeners when tasks are created
CREATE TABLE IF NOT EXISTS queue_tasks (
    task_id VARCHAR(255) PRIMARY KEY,
    process_instance_id VARCHAR(255) NOT NULL,
    process_definition_key VARCHAR(255) NOT NULL,
    task_definition_key VARCHAR(255) NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    queue_name VARCHAR(255) NOT NULL,
    assignee VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    priority INTEGER DEFAULT 50,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claimed_at TIMESTAMP,
    completed_at TIMESTAMP,
    task_data JSONB
);

-- Indexes for queue queries
CREATE INDEX idx_queue_tasks_queue_name ON queue_tasks(queue_name);
CREATE INDEX idx_queue_tasks_status ON queue_tasks(status);
CREATE INDEX idx_queue_tasks_assignee ON queue_tasks(assignee);
CREATE INDEX idx_queue_tasks_process_instance ON queue_tasks(process_instance_id);