package com.jfontdev.trackstack.service.impl;

import com.jfontdev.trackstack.dto.tag.TagRequestDTO;
import com.jfontdev.trackstack.dto.tag.TagResponseDTO;
import com.jfontdev.trackstack.exception.NotFoundException;
import com.jfontdev.trackstack.model.Tag;
import com.jfontdev.trackstack.repository.TagRepository;
import com.jfontdev.trackstack.service.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
 * - Write operations ({@code createTag}) evict the entire "tags" cache to
 * ensure
 * that subsequent reads (especially {@code getAllTags}) do not return stale
 * data.
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
     * Creates a new tag in the system.
     * <p>
     * This method maps the incoming DTO to a domain entity, saves it to the
     * database,
     * and then maps the saved entity back to a response DTO.
     * <p>
     * <b>Cache Eviction:</b> We evict all entries in the "tags" cache because
     * adding
     * a new tag invalidates the result of {@code getAllTags()}.
     *
     * @param dto the data transfer object containing the details of the tag to
     *            create
     * @return a response DTO containing the saved tag's details, including its
     *         generated ID
     */
    @Override
    @CacheEvict(value = "tags", allEntries = true)
    public TagResponseDTO createTag(TagRequestDTO dto) {
        log.info("Evicting 'tags' cache. Creating new tag: {}", dto.name());
        Tag tag = Tag.create(dto.name());
        Tag saved = tagRepository.saveAndFlush(tag);

        return new TagResponseDTO(
                saved.getId(),
                saved.getName());
    }

    /**
     * Retrieves a tag by its unique identifier.
     * <p>
     * <b>Caching:</b> The result of this method is cached. If the tag is requested
     * again
     * with the same ID, the cached value is returned instead of querying the
     * database.
     *
     * @param id the unique identifier of the tag to retrieve
     * @return a response DTO containing the tag's details
     * @throws NotFoundException if no tag is found with the provided ID
     */
    @Override
    @Cacheable(value = "tags", key = "#id")
    public TagResponseDTO getTagById(Long id) {
        log.info("Cache miss for 'tags' with id: {}. Fetching from database.", id);
        Optional<Tag> tag = tagRepository.findById(id);

        if (tag.isEmpty()) {
            throw new NotFoundException("Tag not found.");
        }

        Tag t = tag.get();

        return new TagResponseDTO(
                t.getId(),
                t.getName());
    }

    /**
     * Retrieves all tags currently stored in the system.
     * <p>
     * <b>Caching:</b> The entire list of tags is cached. This is highly efficient
     * for
     * read-heavy workloads, but requires careful eviction (clearing the cache)
     * whenever
     * a tag is added, updated, or deleted to prevent stale data.
     *
     * @return a list of response DTOs representing all tags
     */
    @Override
    @Cacheable(value = "tags")
    public List<TagResponseDTO> getAllTags() {
        log.info("Cache miss for 'tags' list. Fetching all tags from database.");
        return tagRepository.findAll()
                .stream()
                .map(t -> new TagResponseDTO(
                        t.getId(),
                        t.getName()))
                .toList();
    }
}
