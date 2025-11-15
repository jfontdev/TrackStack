package com.jfontdev.trackstack.repository;

import com.jfontdev.trackstack.model.Track;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<Track, Long> {
}
