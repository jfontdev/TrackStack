-- TrackStack baseline
CREATE TABLE IF NOT EXISTS flyway_baseline (
                                               id SERIAL PRIMARY KEY,
                                               initialized_at TIMESTAMP DEFAULT NOW()
    );
