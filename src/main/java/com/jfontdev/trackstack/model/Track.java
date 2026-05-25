package com.jfontdev.trackstack.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a music track in the DJ library.
 * <p>
 * Tracks are the core domain object in TrackStack. Each track has metadata
 * extracted from audio files including BPM, musical key, genre, and file info.
 * Tracks are associated with Transitions and Setlists in the DJ workflow.
 * <p>
 * This entity follows the static factory method pattern for creation
 * and provides an {@code update} method for encapsulated mutation.
 */
@Entity
@Table(name = "tracks")
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String artist;
    private String album;
    private Double bpm;

    @Column(name = "musical_key")
    private String key; // Camelot or traditional notation

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    private String genre;

    @Column(name = "file_path", nullable = false, unique = true)
    private String filePath;

    @Column(name = "file_format")
    private String fileFormat; // mp3, flac, aiff, wav

    private Integer bitrate; // kbps

    @Column(name = "energy")
    private Integer energy; // 1-5, manual or AI-suggested

    @Column(name = "play_count")
    private Integer playCount;

    @Column(name = "last_played_date")
    private LocalDateTime lastPlayedDate;

    @Column(name = "added_date")
    private LocalDateTime addedDate;

    /**
     * Parameterized constructor used by the static factory method.
     */
    public Track(String title, String artist, String album, Double bpm, String key,
                 Integer durationSeconds, String genre, String filePath,
                 String fileFormat, Integer bitrate, Integer energy) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.bpm = bpm;
        this.key = key;
        this.durationSeconds = durationSeconds;
        this.genre = genre;
        this.filePath = filePath;
        this.fileFormat = fileFormat;
        this.bitrate = bitrate;
        this.energy = energy;
        this.playCount = 0;
        this.addedDate = LocalDateTime.now();
    }

    /**
     * Default no-args constructor required by JPA.
     */
    public Track() {

    }

    /**
     * Static factory method for creating a new Track instance.
     */
    public static Track create(String title, String artist, String album, Double bpm, String key,
                               Integer durationSeconds, String genre, String filePath,
                               String fileFormat, Integer bitrate, Integer energy) {
        return new Track(title, artist, album, bpm, key, durationSeconds, genre,
                filePath, fileFormat, bitrate, energy);
    }

    /**
     * Updates mutable fields of this track.
     */
    public void update(String title, String artist, String album, Double bpm, String key,
                       Integer durationSeconds, String genre, String filePath,
                       String fileFormat, Integer bitrate, Integer energy) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.bpm = bpm;
        this.key = key;
        this.durationSeconds = durationSeconds;
        this.genre = genre;
        this.filePath = filePath;
        this.fileFormat = fileFormat;
        this.bitrate = bitrate;
        this.energy = energy;
    }

    /**
     * Records that this track was played, incrementing play count
     * and updating last played date.
     */
    public void recordPlay() {
        this.playCount++;
        this.lastPlayedDate = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public Double getBpm() {
        return bpm;
    }

    public String getKey() {
        return key;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public String getGenre() {
        return genre;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public Integer getBitrate() {
        return bitrate;
    }

    public Integer getEnergy() {
        return energy;
    }

    public Integer getPlayCount() {
        return playCount;
    }

    public LocalDateTime getLastPlayedDate() {
        return lastPlayedDate;
    }

    public LocalDateTime getAddedDate() {
        return addedDate;
    }
}
