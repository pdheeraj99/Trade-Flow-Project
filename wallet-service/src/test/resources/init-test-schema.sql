-- Test schema initialization for Testcontainers
CREATE SCHEMA IF NOT EXISTS wallet;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA wallet TO test;
