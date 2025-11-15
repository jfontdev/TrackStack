package com.jfontdev.trackstack.repository;

import com.jfontdev.trackstack.model.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
}
