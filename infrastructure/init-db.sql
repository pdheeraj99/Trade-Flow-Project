-- ============================================
-- TradeFlow Database Initialization Script
-- Creates all required schemas and extensions
-- ============================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- Schema: auth (Authentication Service)
-- ============================================
CREATE SCHEMA IF NOT EXISTS auth;

-- ============================================
-- Schema: wallet (Wallet & Ledger Service)
-- ============================================
CREATE SCHEMA IF NOT EXISTS wallet;

-- ============================================
-- Schema: orders (Order Management System)
-- ============================================
CREATE SCHEMA IF NOT EXISTS orders;

-- Grant permissions to tradeflow user
GRANT ALL PRIVILEGES ON SCHEMA auth TO tradeflow;
GRANT ALL PRIVILEGES ON SCHEMA wallet TO tradeflow;
GRANT ALL PRIVILEGES ON SCHEMA orders TO tradeflow;

-- Set default search path
ALTER DATABASE tradeflow SET search_path TO public, auth, wallet, orders;

-- Log initialization completion
DO $$
BEGIN
    RAISE NOTICE 'TradeFlow database initialized successfully';
    RAISE NOTICE 'Schemas created: auth, wallet, orders';
    RAISE NOTICE 'Extensions enabled: uuid-ossp, pgcrypto';
END $$;
