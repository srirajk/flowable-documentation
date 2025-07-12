-- Create schemas
CREATE SCHEMA IF NOT EXISTS keycloak;
CREATE SCHEMA IF NOT EXISTS flowable;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA keycloak TO flowable;
GRANT ALL PRIVILEGES ON SCHEMA flowable TO flowable;

-- Set default schema
ALTER DATABASE flowable_wrapper SET search_path TO flowable, public;

-- Create wrapper-specific tables will be handled by Liquibase