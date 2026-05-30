-- ============================================================
-- V10: Create setlists and setlist_slots tables
--
-- Why: Phase 03 introduces Set Planning — ordered sequences of
-- tracks that represent a DJ set. A setlist has metadata (name,
-- status, timing) and contains ordered slots, each referencing
-- a track with an energy level for the set's energy arc.
-- ============================================================

CREATE TABLE setlists
(
    id                       BIGSERIAL PRIMARY KEY,
    name                     VARCHAR(255) NOT NULL,
    description              TEXT,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_date             TIMESTAMP DEFAULT NOW(),
    updated_date             TIMESTAMP,
    performed_date           TIMESTAMP,
    total_duration_seconds   INTEGER DEFAULT 0,
    preparation_time_minutes INTEGER DEFAULT 0,

    CONSTRAINT check_status CHECK (status IN ('DRAFT', 'READY', 'PERFORMED'))
);

CREATE INDEX idx_setlists_status ON setlists(status);
CREATE INDEX idx_setlists_created ON setlists(created_date DESC);

CREATE TABLE setlist_slots
(
    id          BIGSERIAL PRIMARY KEY,
    setlist_id  BIGINT  NOT NULL,
    track_id    BIGINT  NOT NULL,
    slot_order  INTEGER NOT NULL,
    energy      INTEGER CHECK (energy >= 1 AND energy <= 5),
    notes       TEXT,

    CONSTRAINT fk_setlist_slot_setlist FOREIGN KEY (setlist_id) REFERENCES setlists (id) ON DELETE CASCADE,
    CONSTRAINT fk_setlist_slot_track FOREIGN KEY (track_id) REFERENCES tracks (id) ON DELETE CASCADE
);

CREATE INDEX idx_setlist_slots_setlist ON setlist_slots(setlist_id);
CREATE INDEX idx_setlist_slots_track ON setlist_slots(track_id);
CREATE INDEX idx_setlist_slots_order ON setlist_slots(setlist_id, slot_order);
