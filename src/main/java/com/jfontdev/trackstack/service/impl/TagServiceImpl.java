package com.jfontdev.trackstack.service.impl;

import com.jfontdev.trackstack.dto.tag.TagPatchRequestDTO;
import com.jfontdev.trackstack.dto.tag.TagRequestDTO;
import com.jfontdev.trackstack.dto.tag.TagResponseDTO;
import com.jfontdev.trackstack.dto.tag.TagUpdateRequestDTO;
import com.jfontdev.trackstack.exception.NotFoundException;
import com.jfontdev.trackstack.model.Tag;
import com.jfontdev.trackstack.repository.TagRepository;
import com.jfontdev.trackstack.service.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of the {@link TagService} interface.
 * <p>
 * This service handles the business logic for managing {@link Tag} entities.
 * It acts as a bridge between the controller layer (which handles HTTP
 * requests)
 * and the repository layer (which handles database operations).
 * <p>
 * <b>Caching Strategy:</b>
 * We use Spring's caching abstraction to improve read performance.
 * - Read operations ({@code getTagById}, {@code getAllTags}) are cached under
 * the "tags" cache.
 * - Write operations (create, update, patch, delete) evict the entire "tags"
 * cache to ensure that subsequent reads do not return stale data.
 * <p>
 * <b>Transaction Strategy:</b>
 * All write operations are annotated with {@code @Transactional} to ensure
 * proper rollback on failure.
 */
@Service
public class TagServiceImpl implements TagService {

    private static final Logger log = LoggerFactory.getLogger(TagServiceImpl.class);

    private final TagRepository tagRepository;

    /**
     * Constructs a new {@code TagServiceImpl} with the required repository.
     * We use constructor injection to ensure the repository is provided and
     * immutable.
     *
     * @param tagRepository the repository used for database operations on tags
     */
    public TagServiceImpl(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Cache Eviction:</b> We evict all entries in the "tags" cache because
     * adding a new tag invalidates the result of {@code getAllTags()}.
     */
    @Override
    @CacheEvict(value = "tags", allEntries = true)
    @Transactional
    public TagResponseDTO createTag(TagRequestDTO dto) {
        log.info("Evicting 'tags' cache. Creating new tag: {}", dto.name());
        Tag tag = Tag.create(dto.name());
        Tag saved = tagRepository.saveAndFlush(tag);

        return mapToResponseDTO(saved);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Caching:</b> The result of this method is cached. If the tag is requested
     * again with the same ID, the cached value is returned instead of querying the
     * database.
     */
    @Override
    @Cacheable(value = "tags", key = "#id")
    @Transactional(readOnly = true)
    public TagResponseDTO getTagById(Long id) {
        log.info("Cache miss for 'tags' with id: {}. Fetching from database.", id);
        Tag tag = findTagOrThrow(id);

        return mapToResponseDTO(tag);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Caching:</b> The entire list of tags is cached. This is highly efficient
     * for read-heavy workloads, but requires careful eviction whenever a tag
     * is added, updated, or deleted to prevent stale data.
     */
    @Override
    @Cacheable(value = "tags")
    @Transactional(readOnly = true)
    public List<TagResponseDTO> getAllTags() {
        log.info("Cache miss for 'tags' list. Fetching all tags from database.");
        return tagRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Cache Eviction:</b> Evicts all entries in the "tags" cache because
     * updating a tag invalidates both the individual entry and the list. It also
     * evicts "tracks" and "playlists" caches because they both include tag names.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "tags", allEntries = true),
            @CacheEvict(value = "tracks", allEntries = true),
            @CacheEvict(value = "playlists", allEntries = true)
    })
    @Transactional
    public TagResponseDTO updateTag(Long id, TagUpdateRequestDTO dto) {
        log.info("Evicting 'tags', 'tracks', and 'playlists' caches. Updating tag with id: {}", id);
        Tag tag = findTagOrThrow(id);

        tag.update(dto.name());
        Tag saved = tagRepository.saveAndFlush(tag);

        return mapToResponseDTO(saved);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Merges non-null fields from the patch DTO with the existing entity's values,
     * then delegates to the entity's {@code update} method.
     * <p>
     * <b>Cache Eviction:</b> Evicts all entries in the "tags" cache. It also
     * evicts "tracks" and "playlists" caches because they both include tag names.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "tags", allEntries = true),
            @CacheEvict(value = "tracks", allEntries = true),
            @CacheEvict(value = "playlists", allEntries = true)
    })
    @Transactional
    public TagResponseDTO patchTag(Long id, TagPatchRequestDTO dto) {
        log.info("Evicting 'tags', 'tracks', and 'playlists' caches. Patching tag with id: {}", id);
        Tag tag = findTagOrThrow(id);

        String name = dto.name() != null ? dto.name() : tag.getName();

        tag.update(name);
        Tag saved = tagRepository.saveAndFlush(tag);

        return mapToResponseDTO(saved);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Cache Eviction:</b> Evicts all entries in the "tags" cache because
     * deleting a tag invalidates the list cache. It also evicts "tracks"
     * and "playlists" caches because they both include tag names.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "tags", allEntries = true),
            @CacheEvict(value = "tracks", allEntries = true),
            @CacheEvict(value = "playlists", allEntries = true)
    })
    @Transactional
    public void deleteTag(Long id) {
        log.info("Evicting 'tags', 'tracks', and 'playlists' caches. Deleting tag with id: {}", id);
        Tag tag = findTagOrThrow(id);

        tagRepository.delete(tag);
    }

    /**
     * Finds a tag by ID or throws a {@link NotFoundException}.
     * <p>
     * This is an internal helper that centralizes the Optional handling
     * pattern used across all methods that require an existing tag.
     *
     * @param id the tag ID to look up
     * @return the found Tag entity
     * @throws NotFoundException if no tag exists with the given ID
     */
    private Tag findTagOrThrow(Long id) {
        Optional<Tag> tag = tagRepository.findById(id);

        if (tag.isEmpty()) {
            throw new NotFoundException("Tag not found.");
        }

        return tag.get();
    }

    /**
     * Maps a {@link Tag} entity to a {@link TagResponseDTO}.
     * <p>
     * This centralizes the entity-to-DTO mapping logic to avoid repetition
     * across service methods.
     *
     * @param tag the entity to map
     * @return the corresponding response DTO
     */
    private TagResponseDTO mapToResponseDTO(Tag tag) {
        return new TagResponseDTO(
                tag.getId(),
                tag.getName());
    }
}
