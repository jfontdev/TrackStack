CREATE TABLE tracks
(
    id     SERIAL PRIMARY KEY,
    title  VARCHAR(255) NOT NULL,
    artist VARCHAR(255),
    album  VARCHAR(255),
    bpm    float8,
    key    VARCHAR(10),
    duration VARCHAR(10)
);

CREATE TABLE tags
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE playlists
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT
);

CREATE TABLE track_tags
(
    track_id INT NOT NULL,
    tag_id   INT NOT NULL,
    PRIMARY KEY (track_id, tag_id),
    FOREIGN KEY (track_id) REFERENCES tracks (id),
    FOREIGN KEY (tag_id) REFERENCES tags (id)
);

CREATE TABLE playlist_tracks
(
    playlist_id INT NOT NULL,
    track_id    INT NOT NULL,
    PRIMARY KEY (playlist_id, track_id),
    FOREIGN KEY (playlist_id) REFERENCES playlists (id),
    FOREIGN KEY (track_id) REFERENCES tracks (id)
);
