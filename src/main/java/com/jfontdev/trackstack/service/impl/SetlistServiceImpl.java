package com.jfontdev.trackstack.service.impl;

import com.jfontdev.trackstack.dto.setlist.SetlistEnergyArcDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistExportDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistRequestDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistResponseDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistSlotRequestDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistSlotResponseDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistTransitionValidationDTO;
import com.jfontdev.trackstack.dto.setlist.SetlistUpdateRequestDTO;
import com.jfontdev.trackstack.exception.NotFoundException;
import com.jfontdev.trackstack.model.Setlist;
import com.jfontdev.trackstack.model.SetlistSlot;
import com.jfontdev.trackstack.model.Track;
import com.jfontdev.trackstack.model.Transition;
import com.jfontdev.trackstack.repository.SetlistRepository;
import com.jfontdev.trackstack.repository.SetlistSlotRepository;
import com.jfontdev.trackstack.repository.TrackRepository;
import com.jfontdev.trackstack.repository.TransitionRepository;
import com.jfontdev.trackstack.service.SetlistService;
import com.jfontdev.trackstack.service.TransitionCompatibilityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the {@link SetlistService} interface.
 * <p>
 * Manages the lifecycle of {@link Setlist} entities and their ordered
 * {@link SetlistSlot} children. Provides operations for building,
 * modifying, and progressing setlists through their lifecycle.
 * <p>
 * <b>Transaction Strategy:</b>
 * All write operations are {@code @Transactional} to ensure atomicity
 * of setlist + slot mutations. Read operations are {@code @Transactional(readOnly = true)}.
 */
@Service
public class SetlistServiceImpl implements SetlistService {

    private static final Logger log = LoggerFactory.getLogger(SetlistServiceImpl.class);

    private final SetlistRepository setlistRepository;
    private final SetlistSlotRepository setlistSlotRepository;
    private final TrackRepository trackRepository;
    private final TransitionRepository transitionRepository;
    private final TransitionCompatibilityEngine compatibilityEngine;

    public SetlistServiceImpl(SetlistRepository setlistRepository,
                              SetlistSlotRepository setlistSlotRepository,
                              TrackRepository trackRepository,
                              TransitionRepository transitionRepository,
                              TransitionCompatibilityEngine compatibilityEngine) {
        this.setlistRepository = setlistRepository;
        this.setlistSlotRepository = setlistSlotRepository;
        this.trackRepository = trackRepository;
        this.transitionRepository = transitionRepository;
        this.compatibilityEngine = compatibilityEngine;
    }

    /**
     * Creates a new setlist with optional initial slots.
     * <p>
     * Steps:
     * <ol>
     *   <li>Create the setlist entity via static factory (DRAFT status).</li>
     *   <li>Save the setlist to obtain its generated ID.</li>
     *   <li>If slots are provided, validate each track exists and create slot entities.</li>
     *   <li>Save all slots and recalculate total duration.</li>
     * </ol>
     *
     * @param dto the request containing setlist metadata and optional slots
     * @return the created setlist with populated slots
     */
    @Override
    @Transactional
    public SetlistResponseDTO createSetlist(SetlistRequestDTO dto) {
        log.info("Creating setlist: {}", dto.name());

        Setlist setlist = Setlist.create(dto.name(), dto.description());
        Setlist savedSetlist = setlistRepository.saveAndFlush(setlist);

        if (dto.slots() != null && !dto.slots().isEmpty()) {
            for (SetlistSlotRequestDTO slotDto : dto.slots()) {
                validateTrackExists(slotDto.trackId());
                SetlistSlot slot = SetlistSlot.create(
                        savedSetlist.getId(),
                        slotDto.trackId(),
                        slotDto.slotOrder(),
                        slotDto.energy(),
                        slotDto.notes());
                setlistSlotRepository.save(slot);
            }
            setlistSlotRepository.flush();
            recalculateTotalDuration(savedSetlist);
        }

        return mapToResponseDTO(savedSetlist);
    }

    /**
     * Retrieves a setlist by ID with all of its ordered slots.
     *
     * @param id the setlist's unique identifier
     * @return the setlist with its ordered slots
     * @throws NotFoundException if the setlist does not exist
     */
    @Override
    @Transactional(readOnly = true)
    public SetlistResponseDTO getSetlistById(Long id) {
        log.debug("Fetching setlist with id: {}", id);
        Setlist setlist = findSetlistOrThrow(id);
        return mapToResponseDTO(setlist);
    }

    /**
     * Retrieves all setlists ordered by creation date descending.
     *
     * @return list of all setlists, newest first
     */
    @Override
    @Transactional(readOnly = true)
    public List<SetlistResponseDTO> getAllSetlists() {
        log.debug("Fetching all setlists");
        return setlistRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Setlist::getCreatedDate).reversed())
                .map(this::mapToResponseDTO)
                .toList();
    }

    /**
     * Retrieves setlists filtered by status, ordered by creation date descending.
     *
     * @param status the lifecycle status (DRAFT, READY, PERFORMED)
     * @return list of matching setlists, newest first
     * @throws IllegalArgumentException if status is not a valid value
     */
    @Override
    @Transactional(readOnly = true)
    public List<SetlistResponseDTO> getSetlistsByStatus(String status) {
        log.debug("Fetching setlists with status: {}", status);
        validateStatus(status);
        return setlistRepository.findByStatusOrderByCreatedDateDesc(status)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    /**
     * Fully updates a setlist's metadata (name and description).
     * <p>
     * Does not modify slots or status. Updates the {@code updatedDate} timestamp.
     *
     * @param id  the setlist's unique identifier
     * @param dto the new metadata
     * @return the updated setlist
     * @throws NotFoundException if the setlist does not exist
     */
    @Override
    @Transactional
    public SetlistResponseDTO updateSetlist(Long id, SetlistUpdateRequestDTO dto) {
        log.info("Updating setlist with id: {}", id);
        Setlist setlist = findSetlistOrThrow(id);
        setlist.update(dto.name(), dto.description());
        Setlist saved = setlistRepository.saveAndFlush(setlist);
        return mapToResponseDTO(saved);
    }

    /**
     * Deletes a setlist and all of its slots.
     * <p>
     * Slots are automatically deleted via {@code ON DELETE CASCADE},
     * but we also explicitly delete them to ensure flush ordering.
     *
     * @param id the setlist's unique identifier
     * @throws NotFoundException if the setlist does not exist
     */
    @Override
    @Transactional
    public void deleteSetlist(Long id) {
        log.info("Deleting setlist with id: {}", id);
        Setlist setlist = findSetlistOrThrow(id);
        setlistSlotRepository.deleteBySetlistId(id);
        setlistRepository.delete(setlist);
    }

    /**
     * Adds a new slot to an existing setlist.
     * <p>
     * Validates the track exists, creates the slot, and recalculates
     * the setlist's total duration.
     *
     * @param setlistId the parent setlist ID
     * @param dto       the slot details
     * @return the updated setlist with the new slot
     * @throws NotFoundException if setlist or track not found
     */
    @Override
    @Transactional
    public SetlistResponseDTO addSlot(Long setlistId, SetlistSlotRequestDTO dto) {
        log.info("Adding slot to setlist {}: track {}", setlistId, dto.trackId());
        Setlist setlist = findSetlistOrThrow(setlistId);
        validateTrackExists(dto.trackId());

        SetlistSlot slot = SetlistSlot.create(
                setlistId,
                dto.trackId(),
                dto.slotOrder(),
                dto.energy(),
                dto.notes());
        setlistSlotRepository.saveAndFlush(slot);

        recalculateTotalDuration(setlist);
        Setlist saved = setlistRepository.saveAndFlush(setlist);
        return mapToResponseDTO(saved);
    }

    /**
     * Removes a slot from a setlist.
     * <p>
     * Verifies the slot belongs to the specified setlist before deletion.
     * Recalculates total duration after removal.
     *
     * @param setlistId the parent setlist ID
     * @param slotId    the slot to remove
     * @return the updated setlist
     * @throws NotFoundException if setlist or slot not found
     * @throws IllegalArgumentException if slot does not belong to setlist
     */
    @Override
    @Transactional
    public SetlistResponseDTO removeSlot(Long setlistId, Long slotId) {
        log.info("Removing slot {} from setlist {}", slotId, setlistId);
        Setlist setlist = findSetlistOrThrow(setlistId);
        SetlistSlot slot = findSlotOrThrow(slotId);

        if (!slot.getSetlistId().equals(setlistId)) {
            throw new IllegalArgumentException(
                    "Slot " + slotId + " does not belong to setlist " + setlistId);
        }

        setlistSlotRepository.delete(slot);
        recalculateTotalDuration(setlist);
        Setlist saved = setlistRepository.saveAndFlush(setlist);
        return mapToResponseDTO(saved);
    }

    /**
     * Updates an existing slot within a setlist.
     * <p>
     * Validates track existence and ownership, then updates slot fields
     * and recalculates total duration.
     *
     * @param setlistId the parent setlist ID
     * @param slotId    the slot to update
     * @param dto       the new slot details
     * @return the updated setlist
     * @throws NotFoundException if setlist, slot, or track not found
     * @throws IllegalArgumentException if slot does not belong to setlist
     */
    @Override
    @Transactional
    public SetlistResponseDTO updateSlot(Long setlistId, Long slotId, SetlistSlotRequestDTO dto) {
        log.info("Updating slot {} in setlist {}", slotId, setlistId);
        Setlist setlist = findSetlistOrThrow(setlistId);
        SetlistSlot slot = findSlotOrThrow(slotId);

        if (!slot.getSetlistId().equals(setlistId)) {
            throw new IllegalArgumentException(
                    "Slot " + slotId + " does not belong to setlist " + setlistId);
        }

        validateTrackExists(dto.trackId());
        slot.update(dto.trackId(), dto.energy(), dto.notes());
        slot.setSlotOrder(dto.slotOrder());
        setlistSlotRepository.saveAndFlush(slot);

        recalculateTotalDuration(setlist);
        Setlist saved = setlistRepository.saveAndFlush(setlist);
        return mapToResponseDTO(saved);
    }

    /**
     * Reorders the slots of a setlist to match the provided sequence.
     * <p>
     * Validates that all existing slots are included exactly once,
     * then updates each slot's {@code slotOrder} to match the new sequence.
     *
     * @param setlistId   the parent setlist ID
     * @param slotIdOrder ordered list of slot IDs defining the new sequence
     * @return the updated setlist with reordered slots
     * @throws NotFoundException if setlist not found
     * @throws IllegalArgumentException if the order is invalid
     */
    @Override
    @Transactional
    public SetlistResponseDTO reorderSlots(Long setlistId, List<Long> slotIdOrder) {
        log.info("Reordering slots for setlist {}", setlistId);
        Setlist setlist = findSetlistOrThrow(setlistId);

        List<SetlistSlot> existingSlots = setlistSlotRepository
                .findBySetlistIdOrderBySlotOrderAsc(setlistId);

        validateReorder(slotIdOrder, existingSlots);

        for (int i = 0; i < slotIdOrder.size(); i++) {
            Long slotId = slotIdOrder.get(i);
            SetlistSlot slot = existingSlots.stream()
                    .filter(s -> s.getId().equals(slotId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Slot not found: " + slotId));
            slot.setSlotOrder(i + 1);
            setlistSlotRepository.save(slot);
        }
        setlistSlotRepository.flush();

        Setlist saved = setlistRepository.saveAndFlush(setlist);
        return mapToResponseDTO(saved);
    }

    /**
     * Marks a setlist as ready for performance.
     * <p>
     * Transitions status from DRAFT to READY and updates the timestamp.
     *
     * @param id the setlist's unique identifier
     * @return the updated setlist
     * @throws NotFoundException if the setlist does not exist
     */
    @Override
    @Transactional
    public SetlistResponseDTO markReady(Long id) {
        log.info("Marking setlist {} as READY", id);
        Setlist setlist = findSetlistOrThrow(id);
        setlist.markReady();
        Setlist saved = setlistRepository.saveAndFlush(setlist);
        return mapToResponseDTO(saved);
    }

    /**
     * Marks a setlist as performed.
     * <p>
     * Transitions status to PERFORMED and records the performance date.
     *
     * @param id the setlist's unique identifier
     * @return the updated setlist
     * @throws NotFoundException if the setlist does not exist
     */
    @Override
    @Transactional
    public SetlistResponseDTO markPerformed(Long id) {
        log.info("Marking setlist {} as PERFORMED", id);
        Setlist setlist = findSetlistOrThrow(id);
        setlist.markPerformed();
        Setlist saved = setlistRepository.saveAndFlush(setlist);
        return mapToResponseDTO(saved);
    }

    /**
     * Retrieves the energy arc visualization for a setlist.
     * <p>
     * Fetches all slots in order, resolves each track's metadata, and computes
     * aggregate statistics (average, peak, low, trend) from the energy values.
     *
     * @param id the setlist's unique identifier
     * @return the energy arc data with points and statistics
     * @throws NotFoundException if the setlist does not exist
     */
    @Override
    @Transactional(readOnly = true)
    public SetlistEnergyArcDTO getEnergyArc(Long id) {
        log.debug("Fetching energy arc for setlist {}", id);
        Setlist setlist = findSetlistOrThrow(id);

        List<SetlistSlot> slots = setlistSlotRepository
                .findBySetlistIdOrderBySlotOrderAsc(id);

        List<SetlistEnergyArcDTO.EnergyPoint> points = new java.util.ArrayList<>();
        for (SetlistSlot slot : slots) {
            Optional<Track> trackOpt = trackRepository.findById(slot.getTrackId());
            Track track = trackOpt.orElse(null);

            points.add(new SetlistEnergyArcDTO.EnergyPoint(
                    slot.getSlotOrder(),
                    slot.getTrackId(),
                    track != null ? track.getTitle() : null,
                    track != null ? track.getArtist() : null,
                    slot.getEnergy(),
                    track != null ? track.getBpm() : null,
                    track != null ? track.getKey() : null,
                    track != null ? track.getDurationSeconds() : null));
        }

        SetlistEnergyArcDTO.EnergyStats stats = calculateEnergyStats(points);

        return new SetlistEnergyArcDTO(
                setlist.getId(),
                setlist.getName(),
                points,
                stats);
    }

    // --- Helper methods ---

    /**
     * Recalculates the total duration of a setlist by summing the
     * durations of all tracks in its slots.
     * <p>
     * Called whenever slots are added, removed, or updated.
     *
     * @param setlist the setlist to recalculate
     */
    private void recalculateTotalDuration(Setlist setlist) {
        List<SetlistSlot> slots = setlistSlotRepository
                .findBySetlistIdOrderBySlotOrderAsc(setlist.getId());

        int totalSeconds = 0;
        for (SetlistSlot slot : slots) {
            Optional<Track> track = trackRepository.findById(slot.getTrackId());
            if (track.isPresent() && track.get().getDurationSeconds() != null) {
                totalSeconds += track.get().getDurationSeconds();
            }
        }

        setlist.setTotalDurationSeconds(totalSeconds);
        log.debug("Recalculated total duration for setlist {}: {} seconds",
                setlist.getId(), totalSeconds);
    }

    /**
     * Validates that a track exists in the database.
     *
     * @param trackId the track ID to validate
     * @throws NotFoundException if the track does not exist
     */
    private void validateTrackExists(Long trackId) {
        Optional<Track> track = trackRepository.findById(trackId);
        if (track.isEmpty()) {
            throw new NotFoundException("Track not found with id: " + trackId);
        }
    }

    /**
     * Validates that a status string is one of the allowed values.
     *
     * @param status the status to validate
     * @throws IllegalArgumentException if status is invalid
     */
    private void validateStatus(String status) {
        if (!"DRAFT".equals(status) && !"READY".equals(status) && !"PERFORMED".equals(status)) {
            throw new IllegalArgumentException(
                    "Invalid status: " + status + ". Must be DRAFT, READY, or PERFORMED.");
        }
    }

    /**
     * Validates a reorder request against existing slots.
     * <p>
     * Ensures that all existing slots are included exactly once.
     *
     * @param slotIdOrder   the requested new order
     * @param existingSlots the current slots in the setlist
     * @throws IllegalArgumentException if the order is invalid
     */
    private void validateReorder(List<Long> slotIdOrder, List<SetlistSlot> existingSlots) {
        if (slotIdOrder == null || slotIdOrder.isEmpty()) {
            throw new IllegalArgumentException("Slot order list must not be empty");
        }

        if (slotIdOrder.size() != existingSlots.size()) {
            throw new IllegalArgumentException(
                    "Slot order must include exactly " + existingSlots.size() + " slots, got "
                            + slotIdOrder.size());
        }

        List<Long> existingIds = existingSlots.stream()
                .map(SetlistSlot::getId)
                .toList();

        for (Long slotId : slotIdOrder) {
            if (!existingIds.contains(slotId)) {
                throw new IllegalArgumentException(
                        "Slot ID " + slotId + " is not part of this setlist");
            }
        }

        long uniqueCount = slotIdOrder.stream().distinct().count();
        if (uniqueCount != slotIdOrder.size()) {
            throw new IllegalArgumentException("Slot order contains duplicate slot IDs");
        }
    }

    /**
     * Finds a setlist by ID or throws {@link NotFoundException}.
     *
     * @param id the setlist ID to look up
     * @return the found Setlist entity
     * @throws NotFoundException if no setlist exists with the given ID
     */
    private Setlist findSetlistOrThrow(Long id) {
        Optional<Setlist> setlist = setlistRepository.findById(id);
        if (setlist.isEmpty()) {
            throw new NotFoundException("Setlist not found with id: " + id);
        }
        return setlist.get();
    }

    /**
     * Finds a slot by ID or throws {@link NotFoundException}.
     *
     * @param id the slot ID to look up
     * @return the found SetlistSlot entity
     * @throws NotFoundException if no slot exists with the given ID
     */
    private SetlistSlot findSlotOrThrow(Long id) {
        Optional<SetlistSlot> slot = setlistSlotRepository.findById(id);
        if (slot.isEmpty()) {
            throw new NotFoundException("Slot not found with id: " + id);
        }
        return slot.get();
    }

    /**
     * Maps a {@link Setlist} entity to a {@link SetlistResponseDTO}.
     * <p>
     * Fetches and maps all associated slots in order.
     *
     * @param setlist the entity to convert
     * @return the fully populated response DTO
     */
    private SetlistResponseDTO mapToResponseDTO(Setlist setlist) {
        List<SetlistSlotResponseDTO> slotDTOs = setlistSlotRepository
                .findBySetlistIdOrderBySlotOrderAsc(setlist.getId())
                .stream()
                .map(this::mapSlotToResponseDTO)
                .toList();

        return new SetlistResponseDTO(
                setlist.getId(),
                setlist.getName(),
                setlist.getDescription(),
                setlist.getStatus(),
                setlist.getCreatedDate(),
                setlist.getUpdatedDate(),
                setlist.getPerformedDate(),
                setlist.getTotalDurationSeconds(),
                setlist.getPreparationTimeMinutes(),
                slotDTOs);
    }

    /**
     * Validates all track-to-track transitions within a setlist.
     * <p>
     * For each adjacent pair of tracks, calculates key compatibility (Camelot wheel),
     * BPM difference, and checks whether a logged transition exists in the database.
     * Generates a human-readable warning for any problematic pairs.
     *
     * @param id the setlist's unique identifier
     * @return the validation report with per-pair details and summary
     * @throws NotFoundException if the setlist does not exist
     */
    @Override
    @Transactional(readOnly = true)
    public SetlistTransitionValidationDTO validateTransitions(Long id) {
        log.debug("Validating transitions for setlist {}", id);
        Setlist setlist = findSetlistOrThrow(id);

        List<SetlistSlot> slots = setlistSlotRepository
                .findBySetlistIdOrderBySlotOrderAsc(id);

        List<SetlistTransitionValidationDTO.TransitionPairValidation> pairs =
                new java.util.ArrayList<>();

        int compatibleKeyCount = 0;
        int incompatibleKeyCount = 0;
        int unknownKeyCount = 0;
        int knownTransitionCount = 0;
        int missingTransitionCount = 0;
        int warningCount = 0;

        for (int i = 0; i < slots.size() - 1; i++) {
            SetlistSlot fromSlot = slots.get(i);
            SetlistSlot toSlot = slots.get(i + 1);

            Track fromTrack = trackRepository.findById(fromSlot.getTrackId()).orElse(null);
            Track toTrack = trackRepository.findById(toSlot.getTrackId()).orElse(null);

            Boolean keyCompat = null;
            Double bpmDiff = null;
            if (fromTrack != null && toTrack != null) {
                keyCompat = compatibilityEngine.calculateKeyCompatibility(
                        fromTrack.getKey(), toTrack.getKey());
                bpmDiff = compatibilityEngine.calculateBpmDifference(
                        fromTrack.getBpm(), toTrack.getBpm());
            }

            Optional<Transition> transitionOpt = transitionRepository
                    .findBySourceTrackIdAndTargetTrackId(fromSlot.getTrackId(), toSlot.getTrackId());
            boolean knownTransition = transitionOpt.isPresent();
            Integer transitionRating = transitionOpt.map(Transition::getRating).orElse(null);

            String warning = buildTransitionWarning(fromTrack, toTrack, keyCompat, bpmDiff, knownTransition);
            if (warning != null) {
                warningCount++;
            }

            if (Boolean.TRUE.equals(keyCompat)) {
                compatibleKeyCount++;
            } else if (Boolean.FALSE.equals(keyCompat)) {
                incompatibleKeyCount++;
            } else {
                unknownKeyCount++;
            }

            if (knownTransition) {
                knownTransitionCount++;
            } else {
                missingTransitionCount++;
            }

            pairs.add(new SetlistTransitionValidationDTO.TransitionPairValidation(
                    fromSlot.getSlotOrder(),
                    fromSlot.getTrackId(),
                    fromTrack != null ? fromTrack.getTitle() : null,
                    fromTrack != null ? fromTrack.getKey() : null,
                    fromTrack != null ? fromTrack.getBpm() : null,
                    toSlot.getSlotOrder(),
                    toSlot.getTrackId(),
                    toTrack != null ? toTrack.getTitle() : null,
                    toTrack != null ? toTrack.getKey() : null,
                    toTrack != null ? toTrack.getBpm() : null,
                    keyCompat,
                    bpmDiff,
                    knownTransition,
                    transitionRating,
                    warning));
        }

        SetlistTransitionValidationDTO.Summary summary = new SetlistTransitionValidationDTO.Summary(
                pairs.size(),
                compatibleKeyCount,
                incompatibleKeyCount,
                unknownKeyCount,
                knownTransitionCount,
                missingTransitionCount,
                warningCount);

        return new SetlistTransitionValidationDTO(
                setlist.getId(),
                setlist.getName(),
                pairs,
                summary);
    }

    /**
     * Exports a setlist in the requested format.
     * <p>
     * Builds a structured DTO from the setlist's slots and track metadata.
     * The format parameter is validated but does not affect the DTO structure
     * (text format is handled at the controller layer).
     *
     * @param id     the setlist's unique identifier
     * @param format the export format: "json" or "text"
     * @return the exported setlist data
     * @throws NotFoundException        if the setlist does not exist
     * @throws IllegalArgumentException if format is not supported
     */
    @Override
    @Transactional(readOnly = true)
    public SetlistExportDTO exportSetlist(Long id, String format) {
        log.debug("Exporting setlist {} in format {}", id, format);
        Setlist setlist = findSetlistOrThrow(id);

        if (format == null || (!"json".equals(format) && !"text".equals(format))) {
            throw new IllegalArgumentException(
                    "Unsupported format: " + format + ". Must be 'json' or 'text'.");
        }

        List<SetlistSlot> slots = setlistSlotRepository
                .findBySetlistIdOrderBySlotOrderAsc(id);

        List<SetlistExportDTO.ExportSlot> exportSlots = new java.util.ArrayList<>();
        for (SetlistSlot slot : slots) {
            Optional<Track> trackOpt = trackRepository.findById(slot.getTrackId());
            Track track = trackOpt.orElse(null);

            exportSlots.add(new SetlistExportDTO.ExportSlot(
                    slot.getSlotOrder(),
                    slot.getTrackId(),
                    track != null ? track.getTitle() : null,
                    track != null ? track.getArtist() : null,
                    track != null ? track.getKey() : null,
                    track != null ? track.getBpm() : null,
                    slot.getEnergy(),
                    slot.getNotes(),
                    track != null ? track.getDurationSeconds() : null));
        }

        return new SetlistExportDTO(
                setlist.getId(),
                setlist.getName(),
                setlist.getStatus(),
                setlist.getTotalDurationSeconds(),
                exportSlots);
    }

    /**
     * Builds a human-readable warning for a transition pair.
     * <p>
     * Returns null if no issues are detected.
     *
     * @param fromTrack the source track
     * @param toTrack the target track
     * @param keyCompat key compatibility result
     * @param bpmDiff BPM difference
     * @param knownTransition whether a transition is logged
     * @return a warning string, or null if no issues
     */
    private String buildTransitionWarning(Track fromTrack, Track toTrack,
                                           Boolean keyCompat, Double bpmDiff,
                                           boolean knownTransition) {
        StringBuilder warning = new StringBuilder();

        if (Boolean.FALSE.equals(keyCompat)) {
            String fromKey = fromTrack != null ? fromTrack.getKey() : "?";
            String toKey = toTrack != null ? toTrack.getKey() : "?";
            warning.append("Keys not compatible: ").append(fromKey).append(" -> ").append(toKey);
        }

        if (bpmDiff != null && bpmDiff > 15.0) {
            if (warning.length() > 0) {
                warning.append("; ");
            }
            warning.append("Large BPM jump: ").append(String.format("%.1f", bpmDiff));
        }

        if (!knownTransition) {
            if (warning.length() > 0) {
                warning.append("; ");
            }
            warning.append("No logged transition");
        }

        return warning.length() > 0 ? warning.toString() : null;
    }

    /**
     * Calculates aggregate energy statistics from a list of energy points.
     * <p>
     * Computes average, peak, low, and a qualitative trend label.
     * Only non-null energy values are considered for the average.
     *
     * @param points the ordered energy points
     * @return the calculated statistics
     */
    private SetlistEnergyArcDTO.EnergyStats calculateEnergyStats(
            List<SetlistEnergyArcDTO.EnergyPoint> points) {
        int sum = 0;
        int count = 0;
        Integer peak = null;
        Integer low = null;

        for (SetlistEnergyArcDTO.EnergyPoint point : points) {
            Integer energy = point.energy();
            if (energy == null) {
                continue;
            }

            sum += energy;
            count++;

            if (peak == null || energy > peak) {
                peak = energy;
            }
            if (low == null || energy < low) {
                low = energy;
            }
        }

        Double average = count > 0 ? (double) sum / count : null;
        String trend = determineEnergyTrend(points);

        return new SetlistEnergyArcDTO.EnergyStats(average, peak, low, trend);
    }

    /**
     * Determines a qualitative trend label for the energy progression.
     * <p>
     * Compares the average energy of the first half vs. the second half:
     * <ul>
     *   <li>{@code "BUILD"} — second half is higher than first half</li>
     *   <li>{@code "COOLDOWN"} — second half is lower than first half</li>
     *   <li>{@code "PEAK"} — both halves are high (average >= 4)</li>
     *   <li>{@code "FLAT"} — neither build nor cooldown, and not peak</li>
     * </ul>
     *
     * @param points the ordered energy points
     * @return the trend label
     */
    private String determineEnergyTrend(List<SetlistEnergyArcDTO.EnergyPoint> points) {
        if (points.size() < 2) {
            return "FLAT";
        }

        int mid = points.size() / 2;
        double firstHalfAvg = averageEnergy(points.subList(0, mid));
        double secondHalfAvg = averageEnergy(points.subList(mid, points.size()));

        if (firstHalfAvg >= 4.0 && secondHalfAvg >= 4.0) {
            return "PEAK";
        }
        if (secondHalfAvg > firstHalfAvg + 0.5) {
            return "BUILD";
        }
        if (firstHalfAvg > secondHalfAvg + 0.5) {
            return "COOLDOWN";
        }
        return "FLAT";
    }

    /**
     * Calculates the average energy of a sublist of points.
     * <p>
     * Only non-null energy values are considered.
     *
     * @param points the sublist of points
     * @return the average energy, or 0.0 if no energy values are present
     */
    private double averageEnergy(List<SetlistEnergyArcDTO.EnergyPoint> points) {
        int sum = 0;
        int count = 0;
        for (SetlistEnergyArcDTO.EnergyPoint point : points) {
            if (point.energy() != null) {
                sum += point.energy();
                count++;
            }
        }
        return count > 0 ? (double) sum / count : 0.0;
    }

    /**
     * Maps a {@link SetlistSlot} entity to a {@link SetlistSlotResponseDTO}.
     *
     * @param slot the entity to convert
     * @return the populated slot response DTO
     */
    private SetlistSlotResponseDTO mapSlotToResponseDTO(SetlistSlot slot) {
        return new SetlistSlotResponseDTO(
                slot.getId(),
                slot.getSetlistId(),
                slot.getTrackId(),
                slot.getSlotOrder(),
                slot.getEnergy(),
                slot.getNotes());
    }
}
