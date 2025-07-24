-- User Management System Database Schema
-- Designed for Cerbos integration with workflow-specific roles

-- Core Users Table
CREATE TABLE users (
    id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    attributes JSONB DEFAULT '{}' -- Flexible user attributes (department, region, level, hire_date, etc.)
);

-- Business Applications Table (renamed from workflow_types)
CREATE TABLE business_applications (
    id BIGSERIAL PRIMARY KEY,
    business_app_name VARCHAR(100) NOT NULL UNIQUE, -- e.g., 'Sanctions-Management'
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB DEFAULT '{}' -- Flexible business app metadata (processDefinitionKey, version, owner, etc.)
);

-- Business Application Roles Table (application-specific roles)
CREATE TABLE business_app_roles (
    id BIGSERIAL PRIMARY KEY,
    business_app_id BIGINT NOT NULL REFERENCES business_applications(id),
    role_name VARCHAR(100) NOT NULL,           -- e.g., 'level1-maker'
    role_display_name VARCHAR(255) NOT NULL,   -- e.g., 'Level 1 Maker'
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB DEFAULT '{}', -- Flexible role metadata (permissions, etc.)
    UNIQUE(business_app_id, role_name)
);

-- User Business Application Role Assignments
CREATE TABLE user_business_app_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL REFERENCES users(id),
    business_app_role_id BIGINT NOT NULL REFERENCES business_app_roles(id),
    assigned_by VARCHAR(50) REFERENCES users(id),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    UNIQUE(user_id, business_app_role_id)
);

-- Indexes for performance
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_attributes ON users USING GIN (attributes); -- GIN index for JSONB queries
CREATE INDEX idx_business_apps_name ON business_applications(business_app_name);
CREATE INDEX idx_business_apps_metadata ON business_applications USING GIN (metadata);
CREATE INDEX idx_business_app_roles_app ON business_app_roles(business_app_id);
CREATE INDEX idx_business_app_roles_metadata ON business_app_roles USING GIN (metadata);
CREATE INDEX idx_user_business_app_roles_user ON user_business_app_roles(user_id);
CREATE INDEX idx_user_business_app_roles_active ON user_business_app_roles(is_active);

-- Sample Data
INSERT INTO business_applications (business_app_name, description, metadata) VALUES
('Sanctions-Management', 'Level 1 and Level 2 sanctions case management workflow', 
 '{"processDefinitionKey": "sanctionsCaseManagement", "version": "1.0", "owner": "compliance-team"}'),
('Expense-Reimbursement', 'Employee expense reimbursement approval workflow', 
 '{"processDefinitionKey": "expenseReimbursement", "version": "2.0", "owner": "finance-team"}'),
('Vacation-Request', 'Employee vacation request approval workflow', 
 '{"processDefinitionKey": "vacationRequest", "version": "1.5", "owner": "hr-team"}');

-- Sanctions business app roles
INSERT INTO business_app_roles (business_app_id, role_name, role_display_name, description, metadata) VALUES
((SELECT id FROM business_applications WHERE business_app_name = 'Sanctions-Management'), 'level1-maker', 'Level 1 Maker', 'Can review and make decisions on Level 1 sanctions cases', '{"permissions": ["view", "claim", "complete"], "level": "L1"}'),
((SELECT id FROM business_applications WHERE business_app_name = 'Sanctions-Management'), 'level1-checker', 'Level 1 Checker', 'Can review and validate Level 1 maker decisions', '{"permissions": ["view", "claim", "complete"], "level": "L1"}'),
((SELECT id FROM business_applications WHERE business_app_name = 'Sanctions-Management'), 'level1-supervisor', 'Level 1 Supervisor', 'Can review disagreements and escalate to Level 2', '{"permissions": ["view", "claim", "complete", "escalate"], "level": "L1"}'),
((SELECT id FROM business_applications WHERE business_app_name = 'Sanctions-Management'), 'level2-maker', 'Level 2 Maker', 'Can review and make decisions on Level 2 sanctions cases', '{"permissions": ["view", "claim", "complete"], "level": "L2"}'),
((SELECT id FROM business_applications WHERE business_app_name = 'Sanctions-Management'), 'level2-checker', 'Level 2 Checker', 'Can review and validate Level 2 maker decisions', '{"permissions": ["view", "claim", "complete"], "level": "L2"}'),
((SELECT id FROM business_applications WHERE business_app_name = 'Sanctions-Management'), 'level2-supervisor', 'Level 2 Supervisor', 'Can make final decisions on sanctions cases', '{"permissions": ["view", "claim", "complete", "finalize"], "level": "L2"}'),
((SELECT id FROM business_applications WHERE business_app_name = 'Sanctions-Management'), 'workflow-admin', 'Workflow Administrator', 'Can manage workflow deployments and configurations', '{"permissions": ["deploy", "manage", "admin"], "level": "ADMIN"}');

-- Sample Users
INSERT INTO users (id, username, email, first_name, last_name, attributes) VALUES
('alice.johnson', 'alice.johnson', 'alice.johnson@company.com', 'Alice', 'Johnson', 
 '{"department": "compliance", "region": "US", "level": "L1", "hireDate": "2022-01-15", "costCenter": "CC001"}'),
('bob.smith', 'bob.smith', 'bob.smith@company.com', 'Bob', 'Smith', 
 '{"department": "compliance", "region": "US", "level": "L1", "hireDate": "2021-03-20", "costCenter": "CC001", "manager": "eve.martinez"}'),
('carol.davis', 'carol.davis', 'carol.davis@company.com', 'Carol', 'Davis', 
 '{"department": "risk", "region": "US", "level": "L2", "hireDate": "2020-06-10", "costCenter": "CC002", "clearanceLevel": "HIGH"}'),
('david.wilson', 'david.wilson', 'david.wilson@company.com', 'David', 'Wilson', 
 '{"department": "risk", "region": "EU", "level": "L2", "hireDate": "2019-11-05", "costCenter": "CC003", "timezone": "CET"}'),
('eve.martinez', 'eve.martinez', 'eve.martinez@company.com', 'Eve', 'Martinez', 
 '{"department": "it", "region": "US", "level": "ADMIN", "hireDate": "2018-08-12", "costCenter": "CC004", "isAdmin": true}');

-- Sample Role Assignments
INSERT INTO user_business_app_roles (user_id, business_app_role_id, assigned_by) VALUES
-- Alice: L1 Maker and Checker
('alice.johnson', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level1-maker'), 'eve.martinez'),
('alice.johnson', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level1-checker'), 'eve.martinez'),

-- Bob: L1 Supervisor
('bob.smith', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level1-supervisor'), 'eve.martinez'),

-- Carol: L2 Maker and Checker
('carol.davis', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level2-maker'), 'eve.martinez'),
('carol.davis', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level2-checker'), 'eve.martinez'),

-- David: L2 Supervisor
('david.wilson', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'level2-supervisor'), 'eve.martinez'),

-- Eve: Workflow Admin
('eve.martinez', (SELECT r.id FROM business_app_roles r JOIN business_applications a ON r.business_app_id = a.id WHERE a.business_app_name = 'Sanctions-Management' AND r.role_name = 'workflow-admin'), 'eve.martinez');