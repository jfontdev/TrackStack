- == == == == == == == == == == == == == == == == == == == == == == == == == == == == == == -- V6: Add genre column to tracks.
--
-- Why: Phase 07 introduces server-side filtering by genre.
-- A dedicated column keeps filtering simple and query-efficient
-- while preserving existing tag-based categorization features.
-- ============================================================
ALTER TABLE tracks
ADD COLUMN genre VARCHAR(100);