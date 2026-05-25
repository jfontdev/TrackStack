-- ============================================================
-- V9: Create transitions table
--
-- Why: Phase 02 introduces the Transition Graph — a directed
-- relationship between tracks that captures DJ mixing knowledge.
-- Each row represents a user's experience transitioning from one
-- track to another, with ratings, notes, and auto-calculated
-- compatibility metrics.
-- ============================================================

CREATE TABLE transitions
(
    id              BIGSERIAL PRIMARY KEY,
    source_track_id BIGINT NOT NULL,
    target_track_id BIGINT NOT NULL,
    rating          INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    notes           TEXT,
    style           VARCHAR(50),
    compatible_keys BOOLEAN,
    bpm_difference  DOUBLE PRECISION,
    times_played    INTEGER DEFAULT 0,
    last_played_date TIMESTAMP,
    created_date    TIMESTAMP DEFAULT NOW(),

    -- Ensure we don't duplicate the same directed transition
    CONSTRAINT unique_transition UNIQUE (source_track_id, target_track_id),

    -- Foreign keys to tracks table
    CONSTRAINT fk_source_track FOREIGN KEY (source_track_id) REFERENCES tracks (id) ON DELETE CASCADE,
    CONSTRAINT fk_target_track FOREIGN KEY (target_track_id) REFERENCES tracks (id) ON DELETE CASCADE
);

-- Indexes for common queries
CREATE INDEX idx_transitions_source ON transitions(source_track_id);
CREATE INDEX idx_transitions_target ON transitions(target_track_id);
CREATE INDEX idx_transitions_rating ON transitions(rating DESC);
CREATE INDEX idx_transitions_created ON transitions(created_date);
