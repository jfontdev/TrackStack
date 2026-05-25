package com.jfontdev.trackstack.service;

import com.jfontdev.trackstack.model.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service for scanning the local music directory and converting audio files into
 * {@link Track} entities.
 * <p>
 * This service is the entry point for Phase 01 of the TrackStack domain evolution:
 * populating the track library from the user's actual DJ music collection. It walks
 * the configured directory tree recursively, identifies audio files by extension,
 * and extracts whatever metadata is available from the file system (path, filename,
 * folder structure).
 * <p>
 * <b>Metadata extraction strategy (Phase 01):</b>
 * <ul>
 *   <li><b>Genre:</b> Derived from the immediate parent folder name. The user's
 *       collection is organized by genre (e.g., {@code Techno/}, {@code Trance/}),
 *       so the folder name is treated as the genre tag.</li>
 *   <li><b>Title & Artist:</b> Parsed from the filename using the common DJ naming
 *       convention {@code "Artist - Title [Label].ext"}. If the dash separator is
 *       missing, the entire filename becomes the title and the artist is set to
 *       "Unknown Artist".</li>
 *   <li><b>BPM, Key, Duration, Bitrate, Energy:</b> Not extracted in Phase 01.
 *       These fields are left null and can be populated later via ID3 tag parsing
 *       (JAudioTagger), Rekordbox import, or manual editing.</li>
 * </ul>
 * <p>
 * <b>Design decisions:</b>
 * <ul>
 *   <li>The service is stateless and does not persist tracks itself. It returns a
 *       list of {@link Track} entities that the caller ({@link TrackService})
 *       decides whether to save.</li>
 *   <li>Duplicate detection is handled by the caller using
 *       {@link com.jfontdev.trackstack.repository.TrackRepository#existsByFilePath(String)}.
 *       This keeps the scanner focused on discovery and avoids database coupling.</li>
 *   <li>Any file that fails parsing is skipped with a warning rather than aborting
 *       the entire scan. This makes the scanner resilient to malformed filenames.</li>
 * </ul>
 * <p>
 * <b>Future enhancements:</b>
 * <ul>
 *   <li>ID3 tag extraction (JAudioTagger) for BPM, Key, and Duration from actual
 *       audio metadata.</li>
 *   <li>Rekordbox XML/Database import to sync with existing library analysis.</li>
 *   <li>File system watcher to automatically detect new files added after initial scan.</li>
 * </ul>
 *
 * @see Track
 * @see TrackService#scanLibrary()
 */
@Service
public class AudioMetadataScanner {

    private static final Logger log = LoggerFactory.getLogger(AudioMetadataScanner.class);

    /**
     * Supported audio file extensions that the scanner will process.
     * These cover the most common formats used by DJs.
     * <p>
     * Note: The list is case-insensitive; filenames are normalized to lowercase
     * before matching.
     */
    private static final List<String> AUDIO_EXTENSIONS = List.of(
            ".mp3",   // MPEG Layer 3, most common
            ".flac",  // Free Lossless Audio Codec, preferred for quality
            ".wav",   // Waveform Audio File Format, uncompressed
            ".aiff",  // Audio Interchange File Format, Apple equivalent of WAV
            ".m4a",   // MPEG-4 Audio, often AAC encoded
            ".ogg"    // Ogg Vorbis, open-source alternative
    );

    /**
     * Absolute path to the root music directory that will be scanned.
     * Injected from the {@code trackstack.music-directory} property, defaulting to
     * the user's DJ music folder.
     * <p>
     * The path is stored as a String (not {@link Path}) because it is only used
     * once at scan time via {@link Paths#get(String)}.
     */
    private final String musicDirectory;

    /**
     * Constructs a new {@code AudioMetadataScanner} with the configured music directory.
     *
     * @param musicDirectory the root directory to scan for audio files;
     *                       defaults to {@code C:/Users/jordi/Documents/MEGA/Musica DJ}
     *                       if not configured via properties
     */
    public AudioMetadataScanner(
            @Value("${trackstack.music-directory:C:/Users/jordi/Documents/MEGA/Musica DJ}") String musicDirectory) {
        this.musicDirectory = musicDirectory;
    }

    /**
     * Recursively scans the configured music directory for audio files and converts
     * each file into a {@link Track} entity.
     * <p>
     * The scan follows these steps:
     * <ol>
     *   <li>Resolve the {@code musicDirectory} path.</li>
     *   <li>If the path does not exist, log a warning and return an empty list.</li>
     *   <li>Walk the directory tree recursively using {@link Files#walk(Path)}.</li>
     *   <li>Filter to only regular files with supported audio extensions.</li>
     *   <li>For each audio file, parse the filename and folder structure to build
     *       a Track entity.</li>
     *   <li>Collect all successfully parsed tracks into a list and return it.</li>
     * </ol>
     * <p>
     * <b>Important:</b> The returned tracks are <em>not persisted</em>. The caller
     * is responsible for saving them to the database (usually after deduplication).
     *
     * @return a list of {@link Track} entities representing the discovered audio files;
     *         empty if the directory does not exist or contains no supported files
     */
    public List<Track> scanDirectory() {
        List<Track> tracks = new ArrayList<>();
        Path rootPath = Paths.get(musicDirectory);

        // Guard clause: if the configured directory is missing, log and return empty.
        if (!Files.exists(rootPath)) {
            log.warn("Music directory does not exist: {}", musicDirectory);
            return tracks;
        }

        // Use try-with-resources so the stream is closed automatically.
        // Files.walk opens file handles; failing to close them causes resource leaks.
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(this::isAudioFile)
                    .forEach(path -> {
                        // Attempt to parse each audio file into a Track entity.
                        // If parsing fails (malformed filename, etc.), the file is
                        // skipped and a warning is logged, but the scan continues.
                        Track track = createTrackFromFile(path, rootPath);
                        if (track != null) {
                            tracks.add(track);
                        }
                    });
        } catch (IOException e) {
            // IO errors during directory walking (permissions, broken symlinks, etc.)
            log.error("Error scanning directory: {}", musicDirectory, e);
        }

        log.info("Scanned {} audio files from {}", tracks.size(), musicDirectory);
        return tracks;
    }

    /**
     * Determines whether the given path is a supported audio file.
     * <p>
     * A path is considered an audio file if:
     * <ul>
     *   <li>It is a regular file (not a directory or symbolic link).</li>
     *   <li>Its filename ends with one of the supported extensions in
     *       {@link #AUDIO_EXTENSIONS} (case-insensitive).</li>
     * </ul>
     *
     * @param path the filesystem path to check
     * @return {@code true} if the path represents a supported audio file;
     *         {@code false} otherwise
     */
    private boolean isAudioFile(Path path) {
        // Directories and non-regular files (symlinks, devices) are skipped.
        if (!Files.isRegularFile(path)) {
            return false;
        }

        // Normalize to lowercase for case-insensitive extension matching.
        String fileName = path.getFileName().toString().toLowerCase();

        // Check if the filename ends with any known audio extension.
        return AUDIO_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    /**
     * Parses a single audio file into a {@link Track} entity.
     * <p>
     * This method orchestrates three sub-steps:
     * <ol>
     *   <li><b>Genre extraction:</b> The immediate parent folder name is used as the
     *       genre (e.g., a file inside {@code Techno/} gets genre "Techno").</li>
     *   <li><b>Filename parsing:</b> The filename (without extension) is split into
     *       artist and title using the "Artist - Title" convention. Bracketed suffixes
     *       like {@code [Label]} or {@code (Remix)} are stripped first.</li>
     *   <li><b>Entity creation:</b> A new Track is built via the static factory,
     *       with null values for fields that require audio analysis (BPM, Key, etc.).</li>
     * </ol>
     *
     * @param filePath the absolute path to the audio file
     * @param rootPath the root music directory (used to determine relative depth for genre)
     * @return a fully constructed {@link Track}, or {@code null} if parsing fails
     */
    private Track createTrackFromFile(Path filePath, Path rootPath) {
        try {
            // Get the raw filename including extension (e.g., "Artist - Title.mp3").
            String fileName = filePath.getFileName().toString();

            // Strip the extension for parsing (e.g., "Artist - Title").
            String nameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));

            // Step 1: Derive genre from the parent folder.
            String genre = extractGenre(filePath, rootPath);

            // Step 2: Split filename into artist and title components.
            // Expected format: "Artist - Title [Label].ext" or "Artist - Title.ext"
            ParsedTrackName parsedName = parseTrackName(nameWithoutExtension);

            // Extract the file extension without the dot (e.g., "mp3", "flac").
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

            // Step 3: Build the Track entity using the static factory.
            // BPM, Key, Duration, Bitrate, and Energy are intentionally left null
            // because they require audio analysis or manual entry in later phases.
            return Track.create(
                    parsedName.title,
                    parsedName.artist,
                    null, // album - not available from filename
                    null, // bpm - requires audio analysis (Phase 02+)
                    null, // key - requires audio analysis (Phase 02+)
                    null, // durationSeconds - requires audio analysis (Phase 02+)
                    genre,
                    filePath.toString(),
                    extension,
                    null, // bitrate - requires audio analysis (Phase 02+)
                    null  // energy - user-defined or AI-suggested (Phase 03+)
            );
        } catch (Exception e) {
            // Malformed filenames, missing extensions, or unexpected characters
            // should not crash the entire scan. Log and skip.
            log.warn("Failed to parse track from file: {}", filePath, e);
            return null;
        }
    }

    /**
     * Derives the genre from the file's parent folder name.
     * <p>
     * The user's music library is organized by genre (e.g.,
     * {@code C:/.../Musica DJ/Techno/}, {@code C:/.../Musica DJ/Trance/}).
     * Therefore, the immediate parent folder of any audio file is assumed to
     * represent its genre.
     * <p>
     * If the file sits directly in the root music directory (no subfolder),
     * the genre is set to {@code null} because there is no classification.
     *
     * @param filePath the path to the audio file
     * @param rootPath the root music directory used as the boundary
     * @return the genre string (folder name), or {@code null} if the file is in the root
     */
    private String extractGenre(Path filePath, Path rootPath) {
        Path parent = filePath.getParent();

        // If the file has no parent or sits directly in the root, there's no genre folder.
        if (parent == null || parent.equals(rootPath)) {
            return null;
        }

        // The immediate parent folder name becomes the genre.
        return parent.getFileName().toString();
    }

    /**
     * Parses a raw filename (without extension) into artist and title components.
     * <p>
     * This method applies a heuristic based on common DJ music naming conventions:
     * <pre>
     *   "Artist - Title [Label]"  -> artist="Artist", title="Title"
     *   "Artist - Title"        -> artist="Artist", title="Title"
     *   "Title"                 -> artist="Unknown Artist", title="Title"
     * </pre>
     * <p>
     * <b>Cleaning rules:</b>
     * <ul>
     *   <li>Trailing bracketed suffixes ({@code [Label]}, {@code [Catalog Number]})
     *       are removed.</li>
     *   <li>Trailing parenthetical suffixes ({@code (Remix)}, {@code (Extended Mix)})
     *       are removed.</li>
     *   <li>Anything before the first {@code " - "} is treated as the artist.</li>
     *   <li>Anything after the first {@code " - "} is treated as the title.</li>
     * </ul>
     *
     * @param nameWithoutExtension the filename without its extension (e.g., "Artist - Title")
     * @return a {@link ParsedTrackName} record containing the extracted artist and title
     */
    private ParsedTrackName parseTrackName(String nameWithoutExtension) {
        // Start with the raw filename string.
        String cleaned = nameWithoutExtension;

        // Remove bracketed metadata suffixes like [Label] or [Previous Records].
        cleaned = cleaned.replaceAll("\\s*\\[.*?\\]\\s*$", "");

        // Remove parenthetical suffixes like (Remix) or (Extended Mix).
        cleaned = cleaned.replaceAll("\\s*\\(.*?\\)\\s*$", "");

        // Try to split on the " - " separator, which is the de-facto standard
        // for artist/title separation in DJ music libraries.
        String artist = null;
        String title = cleaned;

        int dashIndex = cleaned.indexOf(" - ");
        if (dashIndex > 0) {
            // Text before the first " - " is the artist.
            artist = cleaned.substring(0, dashIndex).trim();
            // Text after is the title.
            title = cleaned.substring(dashIndex + 3).trim();
        }

        // Fallback: if no " - " separator was found, we cannot determine the artist
        // from the filename alone. Use a placeholder so the track is still importable.
        if (artist == null || artist.isEmpty()) {
            artist = "Unknown Artist";
        }

        // Edge case: if title somehow became empty after splitting, use the cleaned
        // filename as the title so the entity is still valid.
        if (title == null || title.isEmpty()) {
            title = cleaned;
        }

        return new ParsedTrackName(title, artist);
    }

    /**
     * Immutable data holder for the result of parsing a filename into
     * artist and title components.
     * <p>
     * Using a private record keeps the parsing logic encapsulated within the
     * scanner while still providing type-safe, named return values.
     *
     * @param title  the track title extracted from the filename
     * @param artist the track artist extracted from the filename
     */
    private record ParsedTrackName(String title, String artist) {
    }
}
