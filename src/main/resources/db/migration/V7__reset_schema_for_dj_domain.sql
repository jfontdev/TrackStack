-- ============================================================
-- V7: Reset schema for DJ Set Planner domain
--
-- Why: Pivot from generic music API to DJ-focused domain.
-- Drops Tracks, Tags, Playlists and creates new Track schema
-- with file metadata support for audio library scanning.
-- ============================================================

-- Drop old tables (cascade handles join tables)
DROP TABLE IF EXISTS playlist_tracks CASCADE;
DROP TABLE IF EXISTS track_tags CASCADE;
DROP TABLE IF EXISTS playlists CASCADE;
DROP TABLE IF EXISTS tags CASCADE;
DROP TABLE IF EXISTS tracks CASCADE;

-- Create new tracks table with DJ metadata
CREATE TABLE tracks
(
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(500) NOT NULL,
    artist          VARCHAR(500),
    album           VARCHAR(500),
    bpm             DOUBLE PRECISION,
    musical_key     VARCHAR(10),
    duration_seconds INTEGER,
    genre           VARCHAR(100),
    file_path       VARCHAR(2000) NOT NULL UNIQUE,
    file_format     VARCHAR(10),
    bitrate         INTEGER,
    energy          INTEGER CHECK (energy >= 1 AND energy <= 5),
    play_count      INTEGER DEFAULT 0,
    last_played_date TIMESTAMP,
    added_date      TIMESTAMP DEFAULT NOW()
);

-- Index for common queries
CREATE INDEX idx_tracks_genre ON tracks(genre);
CREATE INDEX idx_tracks_bpm ON tracks(bpm);
CREATE INDEX idx_tracks_key ON tracks(musical_key);
CREATE INDEX idx_tracks_added_date ON tracks(added_date);
