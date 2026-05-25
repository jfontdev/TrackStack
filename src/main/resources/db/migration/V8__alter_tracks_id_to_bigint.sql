-- ============================================================
-- V8: Alter tracks.id from INTEGER to BIGINT
--
-- Why: V7 used SERIAL which creates an INTEGER column, but the
-- Track entity defines id as Long which Hibernate maps to BIGINT.
-- This migration aligns the database schema with the JPA entity.
-- ============================================================

ALTER TABLE tracks ALTER COLUMN id TYPE BIGINT;
