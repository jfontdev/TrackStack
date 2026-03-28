-- ============================================================
-- V5: Add ON DELETE CASCADE to join table foreign keys.
--
-- Why: Phase 6 introduces DELETE operations for Track, Tag,
-- and Playlist. Without cascade behavior, deleting an entity
-- that has associations in a join table would fail with a
-- foreign key constraint violation. With ON DELETE CASCADE,
-- the join table rows are automatically removed when the
-- referenced entity is deleted.
-- ============================================================

-- ============================
-- track_tags
-- ============================

ALTER TABLE track_tags DROP CONSTRAINT track_tags_track_id_fkey;
ALTER TABLE track_tags ADD CONSTRAINT track_tags_track_id_fkey
    FOREIGN KEY (track_id) REFERENCES tracks (id) ON DELETE CASCADE;

ALTER TABLE track_tags DROP CONSTRAINT track_tags_tag_id_fkey;
ALTER TABLE track_tags ADD CONSTRAINT track_tags_tag_id_fkey
    FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE;

-- ============================
-- playlist_tracks
-- ============================

ALTER TABLE playlist_tracks DROP CONSTRAINT playlist_tracks_playlist_id_fkey;
ALTER TABLE playlist_tracks ADD CONSTRAINT playlist_tracks_playlist_id_fkey
    FOREIGN KEY (playlist_id) REFERENCES playlists (id) ON DELETE CASCADE;

ALTER TABLE playlist_tracks DROP CONSTRAINT playlist_tracks_track_id_fkey;
ALTER TABLE playlist_tracks ADD CONSTRAINT playlist_tracks_track_id_fkey
    FOREIGN KEY (track_id) REFERENCES tracks (id) ON DELETE CASCADE;
