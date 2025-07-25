-- BUSINESS APPLICATIONS
INSERT INTO business_applications (business_app_name, description, metadata, is_active, created_at, updated_at) VALUES
('Sanctions-Management', 'Level 1 and Level 2 sanctions case management workflow',
 '{"processDefinitionKey": "sanctionsCaseManagement", "version": "1.0", "owner": "compliance-team"}', true, NOW(), NOW()),
('Expense-Reimbursement', 'Employee expense reimbursement approval workflow',
 '{"processDefinitionKey": "expenseReimbursement", "version": "2.0", "owner": "finance-team"}', true, NOW(), NOW()),
('Vacation-Request', 'Employee vacation request approval workflow',
 '{"processDefinitionKey": "vacationRequest", "version": "1.5", "owner": "hr-team"}', true, NOW(), NOW())
ON CONFLICT (business_app_name) DO NOTHING;

-- DEFAULT ROLES FOR ALL BUSINESS APPS
INSERT INTO business_app_roles (business_app_id, role_name, role_display_name, description, metadata, is_active, created_at, updated_at) VALUES
-- Sanctions Management - Default Roles
((SELECT id FROM business_applications WHERE business_app_name = 'Sanctions-Management'), 'deployer', 'Deployer', 'Can deploy and register workflows', '{"permissions": ["deploy", "register"], "type": "default"}', true, NOW(), NOW()),
((SELECT id FROM business_applications WHERE business_app_name = 'Sanctions-Management'), 'workflow-initiator', 'Workflow Initiator', 'Can start and read workflow instances (service account)', '{"permissions": ["start_workflow_instance", "read_workflow_instance"], "type": "default"}', true, NOW(), NOW()),
((SELECT id FROM business_applications WHERE business_app_name = 'Sanctions-Management'), 'business-user', 'Business User', 'Basic business user access', '{"permissions": ["view"], "type": "default"}', true, NOW(), NOW()),
((SELECT id FROM business_applications WHERE business_app_name = 'Sanctions-Management'), 'workflow-admin', 'Workflow Administrator', 'Full administrative access', '{"permissions": ["deploy", "manage", "admin", "view", "claim", "complete"], "type": "default"}', true, NOW(), NOW()),

-- Sanctions Management - Specific Roles
((SELECT id FROM business_applications WHERE business_app_name = 'Sanctions-Management'), 'level1-operator', 'Level 1 Operator', 'Can process Level 1 sanctions cases', '{"permissions": ["view", "claim", "complete"], "level": "L1", "queue": "level1-queue"}', true, NOW(), NOW()),
((SELECT id FROM business_applications WHERE business_app_name = 'Sanctions-Management'), 'level1-supervisor', 'Level 1 Supervisor', 'Can supervise Level 1 operations and escalate cases', '{"permissions": ["view", "claim", "complete", "escalate"], "level": "L1", "queue": "level1-queue"}', true, NOW(), NOW()),
((SELECT id FROM business_applications WHERE business_app_name = 'Sanctions-Management'), 'level2-operator', 'Level 2 Operator', 'Can process Level 2 sanctions cases', '{"permissions": ["view", "claim", "complete"], "level": "L2", "queue": "level2-queue"}', true, NOW(), NOW()),
((SELECT id FROM business_applications WHERE business_app_name = 'Sanctions-Management'), 'level2-supervisor', 'Level 2 Supervisor', 'Can supervise Level 2 operations and make final decisions', '{"permissions": ["view", "claim", "complete", "finalize"], "level": "L2", "queue": "level2-queue"}', true, NOW(), NOW()),

-- Expense Reimbursement - Default Roles
((SELECT id FROM business_applications WHERE business_app_name = 'Expense-Reimbursement'), 'deployer', 'Deployer', 'Can deploy and register workflows', '{"permissions": ["deploy", "register"], "type": "default"}', true, NOW(), NOW()),
((SELECT id FROM business_applications WHERE business_app_name = 'Expense-Reimbursement'), 'business-user', 'Business User', 'Basic business user access', '{"permissions": ["view"], "type": "default"}', true, NOW(), NOW()),
((SELECT id FROM business_applications WHERE business_app_name = 'Expense-Reimbursement'), 'workflow-admin', 'Workflow Administrator', 'Full administrative access', '{"permissions": ["deploy", "manage", "admin", "view", "claim", "complete"], "type": "default"}', true, NOW(), NOW()),

-- Vacation Request - Default Roles
((SELECT id FROM business_applications WHERE business_app_name = 'Vacation-Request'), 'deployer', 'Deployer', 'Can deploy and register workflows', '{"permissions": ["deploy", "register"], "type": "default"}', true, NOW(), NOW()),
((SELECT id FROM business_applications WHERE business_app_name = 'Vacation-Request'), 'business-user', 'Business User', 'Basic business user access', '{"permissions": ["view"], "type": "default"}', true, NOW(), NOW()),
((SELECT id FROM business_applications WHERE business_app_name = 'Vacation-Request'), 'workflow-admin', 'Workflow Administrator', 'Full administrative access', '{"permissions": ["deploy", "manage", "admin", "view", "claim", "complete"], "type": "default"}', true, NOW(), NOW())
ON CONFLICT (business_app_id, role_name) DO NOTHING;

-- USERS
INSERT INTO users (id, username, email, first_name, last_name, is_active, attributes, created_at, updated_at) VALUES
-- Operation Users
('operation-user-1', 'operation-user-1', 'operation-user-1@company.com', 'Operation', 'User1', true,
 '{"department": "compliance", "region": "GLOBAL", "queues": ["level1-queue", "level2-queue", "level1-supervisor-queue", "level2-supervisor-queue"], "role_type": "operational", "businessApps": ["Sanctions-Management", "Expense-Reimbursement", "Vacation-Request"]}', NOW(), NOW()),
('automation-user-2', 'automation-user-2', 'automation-user-2@company.com', 'Automation', 'User2', true,
 '{"department": "compliance", "region": "GLOBAL", "queues": ["level1-queue", "level2-queue"], "role_type": "automation", "businessApps": ["Sanctions-Management", "Expense-Reimbursement", "Vacation-Request"]}', NOW(), NOW()),

-- US Region Users (Level 1)
('us-l1-operator-1', 'us-l1-operator-1', 'us-l1-operator-1@company.com', 'John', 'Smith', true,
 '{"department": "compliance", "region": "US", "queues": ["level1-queue"], "level": "L1"}', NOW(), NOW()),
('us-l1-operator-2', 'us-l1-operator-2', 'us-l1-operator-2@company.com', 'Sarah', 'Johnson', true,
 '{"department": "compliance", "region": "US", "queues": ["level1-queue"], "level": "L1"}', NOW(), NOW()),
('us-l1-supervisor-1', 'us-l1-supervisor-1', 'us-l1-supervisor-1@company.com', 'Michael', 'Brown', true,
 '{"department": "compliance", "region": "US", "queues": ["level1-queue", "level1-supervisor-queue" ], "level": "L1"}', NOW(), NOW()),

-- US Region Users (Level 2)
('us-l2-operator-1', 'us-l2-operator-1', 'us-l2-operator-1@company.com', 'Emily', 'Davis', true,
 '{"department": "compliance", "region": "US", "queues": ["level2-queue"], "level": "L2"}', NOW(), NOW()),
('us-l2-operator-2', 'us-l2-operator-2', 'us-l2-operator-2@company.com', 'David', 'Wilson', true,
 '{"department": "compliance", "region": "US", "queues": ["level2-queue"], "level": "L2"}', NOW(), NOW()),
('us-l2-supervisor-1', 'us-l2-supervisor-1', 'us-l2-supervisor-1@company.com', 'Lisa', 'Martinez', true,
 '{"department": "compliance", "region": "US", "queues": ["level2-queue", "level2-supervisor-queue"], "level": "L2"}', NOW(), NOW()),

-- EU Region Users (Level 1)
('eu-l1-operator-1', 'eu-l1-operator-1', 'eu-l1-operator-1@company.com', 'Hans', 'Mueller', true,
 '{"department": "compliance", "region": "EU", "queues": ["level1-queue"], "level": "L1"}', NOW(), NOW()),
('eu-l1-operator-2', 'eu-l1-operator-2', 'eu-l1-operator-2@company.com', 'Marie', 'Dubois', true,
 '{"department": "compliance", "region": "EU", "queues": ["level1-queue"], "level": "L1"}', NOW(), NOW()),
('eu-l1-supervisor-1', 'eu-l1-supervisor-1', 'eu-l1-supervisor-1@company.com', 'Giovanni', 'Rossi', true,
 '{"department": "compliance", "region": "EU", "queues": ["level1-queue", "level1-supervisor-queue"], "level": "L1"}', NOW(), NOW()),

-- EU Region Users (Level 2)
('eu-l2-operator-1', 'eu-l2-operator-1', 'eu-l2-operator-1@company.com', 'Anna', 'Schmidt', true,
 '{"department": "compliance", "region": "EU", "queues": ["level2-queue"], "level": "L2"}', NOW(), NOW()),
('eu-l2-operator-2', 'eu-l2-operator-2', 'eu-l2-operator-2@company.com', 'Pierre', 'Martin', true,
 '{"department": "compliance", "region": "EU", "queues": ["level2-queue"], "level": "L2"}', NOW(), NOW()),
('eu-l2-supervisor-1', 'eu-l2-supervisor-1', 'eu-l2-supervisor-1@company.com', 'Sofia', 'Andersson', true,
 '{"department": "compliance", "region": "EU", "queues": ["level2-queue", "level2-supervisor-queue"], "level": "L2"}', NOW(), NOW()),

-- APAC Region Users (Level 1)
('apac-l1-operator-1', 'apac-l1-operator-1', 'apac-l1-operator-1@company.com', 'Hiroshi', 'Tanaka', true,
 '{"department": "compliance", "region": "APAC", "queues": ["level1-queue"], "level": "L1"}', NOW(), NOW()),
('apac-l1-operator-2', 'apac-l1-operator-2', 'apac-l1-operator-2@company.com', 'Priya', 'Sharma', true,
 '{"department": "compliance", "region": "APAC", "queues": ["level1-queue"], "level": "L1"}', NOW(), NOW()),
('apac-l1-supervisor-1', 'apac-l1-supervisor-1', 'apac-l1-supervisor-1@company.com', 'Wei', 'Chen', true,
 '{"department": "compliance", "region": "APAC", "queues": ["level1-queue", "level1-supervisor-queue"], "level": "L1"}', NOW(), NOW()),

-- APAC Region Users (Level 2)
('apac-l2-operator-1', 'apac-l2-operator-1', 'apac-l2-operator-1@company.com', 'Kenji', 'Nakamura', true,
 '{"department": "compliance", "region": "APAC", "queues": ["level2-queue"], "level": "L2"}', NOW(), NOW()),
('apac-l2-operator-2', 'apac-l2-operator-2', 'apac-l2-operator-2@company.com', 'Mei', 'Wang', true,
 '{"department": "compliance", "region": "APAC", "queues": ["level2-queue"], "level": "L2"}', NOW(), NOW()),
('apac-l2-supervisor-1', 'apac-l2-supervisor-1', 'apac-l2-supervisor-1@company.com', 'Raj', 'Patel', true,
 '{"department": "compliance", "region": "APAC", "queues": ["level2-queue", "level2-supervisor-queue"], "level": "L2"}', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- USER ROLE ASSIGNMENTS
INSERT INTO user_business_app_roles (user_id, business_app_role_id, is_active) VALUES
-- Operation Users
('operation-user-1', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'workflow-admin'), true),
('automation-user-2', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'deployer'), true),
('automation-user-2', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'workflow-initiator'), true),

-- US Region Role Assignments
('us-l1-operator-1', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level1-operator'), true),
('us-l1-operator-2', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level1-operator'), true),
('us-l1-supervisor-1', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level1-supervisor'), true),
('us-l2-operator-1', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level2-operator'), true),
('us-l2-operator-2', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level2-operator'), true),
('us-l2-supervisor-1', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level2-supervisor'), true),

-- EU Region Role Assignments
('eu-l1-operator-1', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level1-operator'), true),
('eu-l1-operator-2', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level1-operator'), true),
('eu-l1-supervisor-1', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level1-supervisor'), true),
('eu-l2-operator-1', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level2-operator'), true),
('eu-l2-operator-2', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level2-operator'), true),
('eu-l2-supervisor-1', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level2-supervisor'), true),

-- APAC Region Role Assignments
('apac-l1-operator-1', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level1-operator'), true),
('apac-l1-operator-2', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level1-operator'), true),
('apac-l1-supervisor-1', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level1-supervisor'), true),
('apac-l2-operator-1', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level2-operator'), true),
('apac-l2-operator-2', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level2-operator'), true),
('apac-l2-supervisor-1', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level2-supervisor'), true)
ON CONFLICT (user_id, business_app_role_id) DO NOTHING;