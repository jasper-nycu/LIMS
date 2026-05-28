-- ==========================================
-- LIMS Database Initialization Script
-- Dialect: PostgreSQL
-- Note: All comments and schema definitions are in English.
-- ==========================================

-- 0. Sequence Definitions
-- ------------------------------------------
-- Create a centralized sequence for generating sequential Employee IDs (e.g., TS-0001)
CREATE SEQUENCE IF NOT EXISTS user_emp_id_seq START WITH 1 INCREMENT BY 1;

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
    exp_key VARCHAR(50) REFERENCES experiments(exp_key),
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

CREATE TABLE IF NOT EXISTS machine_owners (
    machine_id VARCHAR(50) REFERENCES machines(machine_id),
    owner_initials VARCHAR(50) NOT NULL,
    PRIMARY KEY (machine_id, owner_initials)
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

-- Lab-ops backend: incremental additions (idempotent, safe for existing databases)
ALTER TABLE wip_tasks ADD COLUMN IF NOT EXISTS recipe_id  VARCHAR(100) REFERENCES recipes(recipe_id);
ALTER TABLE wip_tasks ADD COLUMN IF NOT EXISTS priority   VARCHAR(20) DEFAULT 'NORMAL';
ALTER TABLE wip_tasks ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS machine_logs (
    id BIGSERIAL PRIMARY KEY,
    machine_id VARCHAR(50) NOT NULL REFERENCES machines(machine_id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    level VARCHAR(10) NOT NULL,
    message TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_machine_logs_machine_id ON machine_logs(machine_id);
CREATE INDEX IF NOT EXISTS idx_machine_logs_created_at ON machine_logs(created_at);

CREATE TABLE IF NOT EXISTS notifications (
    notif_id SERIAL PRIMARY KEY,
    user_id VARCHAR(20) REFERENCES users(employee_id),
    title VARCHAR(20) NOT NULL,
    message TEXT,
    type VARCHAR(20) DEFAULT 'info',
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
INSERT INTO machines (machine_id, lab_id, name, exp_key, capacity, state, current_utilization) VALUES
    ('SEM-01',       'LAB_MA', 'Surface Scan (SEM)',     'exp_sem',   25, 'IDLE', 0),
    ('BAKE-OVEN-01', 'LAB_RA', 'High-Temp Bake',         'exp_bake',  50, 'IDLE', 0),
    ('TEM-01',       'LAB_MA', 'Deep Layer Analysis',    'exp_deep',  10, 'IDLE', 0),
    ('FIB-01',       'LAB_FA', 'Focused Ion Beam (FIB)', 'exp_fib',   1,  'IDLE', 0),
    ('E-TEST-02',    'LAB_RA', 'Electrical Test',        'exp_etest', 50, 'IDLE', 0),
    ('XRD-01',       'LAB_MA', 'X-Ray Diffraction',      'exp_xrd',   25, 'IDLE', 0)
ON CONFLICT (machine_id) DO NOTHING;

-- Insert Machine Owners
INSERT INTO machine_owners (machine_id, owner_initials) VALUES
    ('SEM-01',       'MW'), ('SEM-01',       'JS'),
    ('BAKE-OVEN-01', 'SC'),
    ('TEM-01',       'RK'),
    ('FIB-01',       'CH'),
    ('E-TEST-02',    'AS'),
    ('XRD-01',       'TH')
ON CONFLICT DO NOTHING;

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

-- 3. Test WIP Queue Data
-- ------------------------------------------

-- Six approved requests, one per experiment type, no requester (nullable)
INSERT INTO requests (request_id, lab_id, priority, status, remarks) VALUES
    ('REQ-TEST-001', 'LAB_MA', 'NORMAL',   'APPROVED', 'SEM surface scan — oxide layer QC'),
    ('REQ-TEST-002', 'LAB_RA', 'URGENT',   'APPROVED', 'High-temp bake — DRAM thermal stress'),
    ('REQ-TEST-003', 'LAB_MA', 'NORMAL',   'APPROVED', 'TEM deep analysis — defect investigation'),
    ('REQ-TEST-004', 'LAB_FA', 'CRITICAL', 'APPROVED', 'FIB circuit edit — failure analysis'),
    ('REQ-TEST-005', 'LAB_RA', 'NORMAL',   'APPROVED', 'E-Test parametric — yield verification'),
    ('REQ-TEST-006', 'LAB_MA', 'URGENT',   'APPROVED', 'XRD crystal structure — process check')
ON CONFLICT (request_id) DO NOTHING;

-- Wafers: 3 for SEM, 2 for Bake, 2 for TEM, 1 for FIB (cap=1), 3 for E-Test, 2 for XRD
INSERT INTO wafers (request_id, wafer_code) VALUES
    ('REQ-TEST-001', 'W-0001'), ('REQ-TEST-001', 'W-0002'), ('REQ-TEST-001', 'W-0003'),
    ('REQ-TEST-002', 'W-0004'), ('REQ-TEST-002', 'W-0005'),
    ('REQ-TEST-003', 'W-0006'), ('REQ-TEST-003', 'W-0007'),
    ('REQ-TEST-004', 'W-0008'),
    ('REQ-TEST-005', 'W-0009'), ('REQ-TEST-005', 'W-0010'), ('REQ-TEST-005', 'W-0011'),
    ('REQ-TEST-006', 'W-0012'), ('REQ-TEST-006', 'W-0013')
ON CONFLICT DO NOTHING;

-- WIP QUEUE tasks — all waiting to be dispatched, with mixed priorities
INSERT INTO wip_tasks (request_id, wafer_code, exp_key, status, priority) VALUES
    ('REQ-TEST-001', 'W-0001', 'exp_sem',   'QUEUE', 'URGENT'),
    ('REQ-TEST-001', 'W-0002', 'exp_sem',   'QUEUE', 'NORMAL'),
    ('REQ-TEST-001', 'W-0003', 'exp_sem',   'QUEUE', 'NORMAL'),
    ('REQ-TEST-002', 'W-0004', 'exp_bake',  'QUEUE', 'URGENT'),
    ('REQ-TEST-002', 'W-0005', 'exp_bake',  'QUEUE', 'NORMAL'),
    ('REQ-TEST-003', 'W-0006', 'exp_deep',  'QUEUE', 'NORMAL'),
    ('REQ-TEST-003', 'W-0007', 'exp_deep',  'QUEUE', 'URGENT'),
    ('REQ-TEST-004', 'W-0008', 'exp_fib',   'QUEUE', 'CRITICAL'),
    ('REQ-TEST-005', 'W-0009', 'exp_etest', 'QUEUE', 'NORMAL'),
    ('REQ-TEST-005', 'W-0010', 'exp_etest', 'QUEUE', 'NORMAL'),
    ('REQ-TEST-005', 'W-0011', 'exp_etest', 'QUEUE', 'URGENT'),
    ('REQ-TEST-006', 'W-0012', 'exp_xrd',   'QUEUE', 'URGENT'),
    ('REQ-TEST-006', 'W-0013', 'exp_xrd',   'QUEUE', 'NORMAL')
ON CONFLICT DO NOTHING;