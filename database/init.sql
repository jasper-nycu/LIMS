-- ==========================================
-- LIMS Database Initialization Script
-- Dialect: PostgreSQL
-- Note: All comments and schema definitions are in English.
-- ==========================================

-- 1. Table Definitions (DDL)
-- ------------------------------------------

CREATE TABLE IF NOT EXISTS roles (
    role_enum VARCHAR(50) PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL,
    permissions JSONB
);

CREATE TABLE IF NOT EXISTS users (
    employee_id VARCHAR(20) PRIMARY KEY,
    role_enum VARCHAR(50) REFERENCES roles(role_enum),
    is_active BOOLEAN DEFAULT true,
    title VARCHAR(10),
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    department VARCHAR(50),
    email VARCHAR(100) UNIQUE NOT NULL,
    telephone VARCHAR(20),
    extension VARCHAR(10),
    mobile_phone VARCHAR(20),
    avatar_url TEXT,
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(255) NOT NULL,
    two_factor_enabled BOOLEAN DEFAULT false,
    totp_secret VARCHAR(255),
    encrypted_private_key TEXT,
    public_key TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    password_modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS laboratories (
    lab_id VARCHAR(20) PRIMARY KEY,
    lab_name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS experiments (
    exp_key VARCHAR(50) PRIMARY KEY,
    lab_id VARCHAR(20) REFERENCES laboratories(lab_id),
    exp_name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS machines (
    machine_id VARCHAR(50) PRIMARY KEY,
    lab_id VARCHAR(20) REFERENCES laboratories(lab_id),
    name VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    state VARCHAR(20) DEFAULT 'IDLE',
    current_utilization INT DEFAULT 0,
    error_code VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS recipes (
    recipe_id VARCHAR(100) PRIMARY KEY,
    machine_id VARCHAR(50) REFERENCES machines(machine_id),
    recipe_name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS requests (
    request_id VARCHAR(20) PRIMARY KEY,
    requester_id VARCHAR(20) REFERENCES users(employee_id),
    approver_id VARCHAR(20) REFERENCES users(employee_id),
    lab_id VARCHAR(20) REFERENCES laboratories(lab_id),
    priority VARCHAR(20) DEFAULT 'NORMAL',
    status VARCHAR(20) DEFAULT 'PENDING',
    remarks TEXT,
    reject_reason TEXT,
    signature_payload_hash VARCHAR(255),
    requester_signature TEXT,
    approver_signature TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wafers (
    wafer_internal_id SERIAL PRIMARY KEY,
    request_id VARCHAR(20) REFERENCES requests(request_id),
    wafer_code VARCHAR(50) NOT NULL,
    UNIQUE(request_id, wafer_code)
);

CREATE TABLE IF NOT EXISTS wip_tasks (
    task_id SERIAL PRIMARY KEY,
    request_id VARCHAR(20) REFERENCES requests(request_id),
    wafer_code VARCHAR(50) NOT NULL,
    exp_key VARCHAR(50) REFERENCES experiments(exp_key),
    machine_id VARCHAR(50) REFERENCES machines(machine_id),
    status VARCHAR(20) DEFAULT 'QUEUE',
    dispatched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Data Insertion (DML)
-- ------------------------------------------

-- Insert all 6 standard roles
INSERT INTO roles (role_enum, role_name, permissions) VALUES
    ('ROLE_SYSADMIN', 'System Admin', '["ALL"]'),
    ('ROLE_LAB_MANAGER', 'Lab Supervisor', '["APPROVE_REQ", "VIEW_DASHBOARD", "MANAGE_RECIPE"]'),
    ('ROLE_LAB_OPERATOR', 'Lab Operator', '["DISPATCH_MACHINE", "MANAGE_WIP", "UNLOAD_WAFER"]'),
    ('ROLE_MACHINE_OWNER', 'Machine Owner', '["MANAGE_MAINTENANCE", "CLEAR_ALARM"]'),
    ('ROLE_FAB_USER', 'Fab User', '["CREATE_REQ", "TRACK_REQ"]'),
    ('ROLE_PUBLIC', 'Public', '["VIEW_PROFILE"]')
ON CONFLICT (role_enum) DO NOTHING;

-- Insert Laboratories
INSERT INTO laboratories (lab_id, lab_name) VALUES
    ('LAB_RA', 'Reliability Lab (RA)'),
    ('LAB_MA', 'Material Analysis Lab (MA)'),
    ('LAB_FA', 'Failure Analysis Lab (FA)')
ON CONFLICT (lab_id) DO NOTHING;

-- Insert Experiments mapped to Labs
INSERT INTO experiments (exp_key, lab_id, exp_name) VALUES
    ('exp_bake', 'LAB_RA', 'High-Temp Bake'),
    ('exp_etest', 'LAB_RA', 'Electrical Test'),
    ('exp_sem', 'LAB_MA', 'Surface Scan (SEM)'),
    ('exp_deep', 'LAB_MA', 'Deep Layer Analysis'),
    ('exp_xrd', 'LAB_MA', 'X-Ray Diffraction'),
    ('exp_fib', 'LAB_FA', 'Focused Ion Beam (FIB)')
ON CONFLICT (exp_key) DO NOTHING;

-- Insert Machine Status and Capacities
INSERT INTO machines (machine_id, lab_id, name, capacity, state, current_utilization) VALUES
    ('SEM-01', 'LAB_MA', 'Surface Scan (SEM)', 25, 'PROCESSING', 72),
    ('BAKE-OVEN-01', 'LAB_RA', 'High-Temp Bake', 50, 'IDLE', 0),
    ('TEM-01', 'LAB_MA', 'Deep Layer Analysis', 10, 'IDLE', 0),
    ('FIB-01', 'LAB_FA', 'Focused Ion Beam (FIB)', 1, 'IDLE', 0),
    ('E-TEST-02', 'LAB_RA', 'Electrical Test', 50, 'PROCESSING', 84),
    ('XRD-01', 'LAB_MA', 'X-Ray Diffraction', 25, 'IDLE', 0)
ON CONFLICT (machine_id) DO NOTHING;

-- Insert Machine Recipes
INSERT INTO recipes (recipe_id, machine_id, recipe_name) VALUES
    ('SEM-Surface-Std', 'SEM-01', 'SEM-Surface-Std'),
    ('SEM-High-Res', 'SEM-01', 'SEM-High-Res'),
    ('Bake-150C-4H', 'BAKE-OVEN-01', 'Bake-150C-4H'),
    ('Bake-250C-2H', 'BAKE-OVEN-01', 'Bake-250C-2H'),
    ('TEM-Lattice-View', 'TEM-01', 'TEM-Lattice-View'),
    ('FIB-Cross-Section', 'FIB-01', 'FIB-Cross-Section'),
    ('FIB-Circuit-Edit', 'FIB-01', 'FIB-Circuit-Edit'),
    ('E-TEST-Parametric', 'E-TEST-02', 'E-TEST-Parametric'),
    ('E-TEST-Yield', 'E-TEST-02', 'E-TEST-Yield'),
    ('XRD-Crystal-Scan', 'XRD-01', 'XRD-Crystal-Scan')
ON CONFLICT (recipe_id) DO NOTHING;

-- 3. Dynamic WIP Task Initialization for Processing Machines
-- ------------------------------------------

-- Create a dummy system request to anchor the active WIP wafers (Requester is NULL)
INSERT INTO requests (request_id, lab_id, priority, status, remarks) VALUES 
    ('REQ-SYS-INIT', 'LAB_MA', 'NORMAL', 'APPROVED', 'System initialization batch for active processing machines.')
ON CONFLICT (request_id) DO NOTHING;

-- Batch insert 18 Wafers for SEM-01 (W-1001 to W-1018) using generate_series
INSERT INTO wafers (request_id, wafer_code)
SELECT 'REQ-SYS-INIT', 'W-' || to_char(g.i, 'FM0000')
FROM generate_series(1001, 1018) AS g(i)
ON CONFLICT DO NOTHING;

-- Batch insert WIP Tasks for SEM-01
INSERT INTO wip_tasks (request_id, wafer_code, exp_key, machine_id, status)
SELECT 'REQ-SYS-INIT', 'W-' || to_char(g.i, 'FM0000'), 'exp_sem', 'SEM-01', 'PROCESSING'
FROM generate_series(1001, 1018) AS g(i)
ON CONFLICT DO NOTHING;

-- Batch insert 42 Wafers for E-TEST-02 (W-2001 to W-2042) using generate_series
INSERT INTO wafers (request_id, wafer_code)
SELECT 'REQ-SYS-INIT', 'W-' || to_char(g.i, 'FM0000')
FROM generate_series(2001, 2042) AS g(i)
ON CONFLICT DO NOTHING;

-- Batch insert WIP Tasks for E-TEST-02
INSERT INTO wip_tasks (request_id, wafer_code, exp_key, machine_id, status)
SELECT 'REQ-SYS-INIT', 'W-' || to_char(g.i, 'FM0000'), 'exp_etest', 'E-TEST-02', 'PROCESSING'
FROM generate_series(2001, 2042) AS g(i)
ON CONFLICT DO NOTHING;