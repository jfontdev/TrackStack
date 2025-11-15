-- Change track_tags.track_id to BIGINT
ALTER TABLE track_tags
    ALTER COLUMN track_id TYPE BIGINT;

-- Change track_tags.tag_id to BIGINT
ALTER TABLE track_tags
    ALTER COLUMN tag_id TYPE BIGINT;

-- Change playlist_tracks.playlist_id to BIGINT
ALTER TABLE playlist_tracks
    ALTER COLUMN playlist_id TYPE BIGINT;

-- Change playlist_tracks.track_id to BIGINT
ALTER TABLE playlist_tracks
    ALTER COLUMN track_id TYPE BIGINT;
