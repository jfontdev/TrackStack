package com.jfontdev.trackstack.controller;

import com.jfontdev.trackstack.dto.tag.TagPatchRequestDTO;
import com.jfontdev.trackstack.dto.tag.TagRequestDTO;
import com.jfontdev.trackstack.dto.tag.TagResponseDTO;
import com.jfontdev.trackstack.dto.tag.TagUpdateRequestDTO;
import com.jfontdev.trackstack.service.TagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing tags.
 * <p>
 * Provides endpoints for full CRUD operations on tags. This controller
 * delegates all business logic to the {@link TagService} and only handles
 * HTTP concerns (request binding, status codes, response formatting).
 */
@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    /**
     * Constructs a new {@code TagController} with the required service.
     *
     * @param tagService the service handling tag business logic
     */
    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    /**
     * Creates a new tag.
     *
     * @param dto the validated request body containing tag details
     * @return 201 Created with the newly created tag
     */
    @PostMapping
    public ResponseEntity<TagResponseDTO> create(@Valid @RequestBody TagRequestDTO dto) {
        TagResponseDTO response = tagService.createTag(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a tag by its ID.
     *
     * @param id the tag's unique identifier
     * @return 200 OK with the tag details, or 404 if not found
     */
    @GetMapping("/{id}")
    public TagResponseDTO getById(@PathVariable Long id) {
        return tagService.getTagById(id);
    }

    /**
     * Retrieves all tags.
     *
     * @return 200 OK with a list of all tags
     */
    @GetMapping
    public List<TagResponseDTO> getAll() {
        return tagService.getAllTags();
    }

    /**
     * Fully updates an existing tag (PUT semantics).
     * <p>
     * The tag name in the request body replaces the existing value.
     * The new name must remain unique.
     *
     * @param id  the tag's unique identifier
     * @param dto the validated request body containing the new tag details
     * @return 200 OK with the updated tag, or 404 if not found
     */
    @PutMapping("/{id}")
    public TagResponseDTO update(@PathVariable Long id, @Valid @RequestBody TagUpdateRequestDTO dto) {
        return tagService.updateTag(id, dto);
    }

    /**
     * Partially updates an existing tag (PATCH semantics).
     * <p>
     * Only non-null fields in the request body are applied to the existing tag.
     *
     * @param id  the tag's unique identifier
     * @param dto the request body containing the fields to update
     * @return 200 OK with the updated tag, or 404 if not found
     */
    @PatchMapping("/{id}")
    public TagResponseDTO patch(@PathVariable Long id, @RequestBody TagPatchRequestDTO dto) {
        return tagService.patchTag(id, dto);
    }

    /**
     * Deletes a tag by its ID.
     * <p>
     * Also removes all track associations for this tag (handled by
     * ON DELETE CASCADE at the database level).
     *
     * @param id the tag's unique identifier
     * @return 204 No Content on success, or 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
}
