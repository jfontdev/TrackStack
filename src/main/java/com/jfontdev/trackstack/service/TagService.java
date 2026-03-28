package com.jfontdev.trackstack.service;

import com.jfontdev.trackstack.dto.tag.TagPatchRequestDTO;
import com.jfontdev.trackstack.dto.tag.TagRequestDTO;
import com.jfontdev.trackstack.dto.tag.TagResponseDTO;
import com.jfontdev.trackstack.dto.tag.TagUpdateRequestDTO;

import java.util.List;

/**
 * Service interface for managing {@link com.jfontdev.trackstack.model.Tag}
 * entities.
 * <p>
 * This interface defines the contract for tag-related business operations,
 * including full CRUD and partial updates. By using an interface, we decouple
 * the controller from the specific implementation, making the code easier
 * to test and maintain.
 */
public interface TagService {

    /**
     * Creates a new tag based on the provided request data.
     *
     * @param dto the data transfer object containing the tag details
     * @return a response DTO containing the newly created tag's details
     */
    TagResponseDTO createTag(TagRequestDTO dto);

    /**
     * Retrieves a tag by its unique identifier.
     *
     * @param id the unique identifier of the tag
     * @return a response DTO containing the tag's details
     * @throws com.jfontdev.trackstack.exception.NotFoundException if the tag is not
     *                                                             found
     */
    TagResponseDTO getTagById(Long id);

    /**
     * Retrieves all tags in the system.
     *
     * @return a list of response DTOs representing all tags
     */
    List<TagResponseDTO> getAllTags();

    /**
     * Fully updates an existing tag (PUT semantics).
     * <p>
     * The tag name is replaced with the value from the request DTO.
     * The new name must remain unique (enforced at the database level).
     *
     * @param id  the unique identifier of the tag to update
     * @param dto the data transfer object containing the new tag details
     * @return a response DTO containing the updated tag's details
     * @throws com.jfontdev.trackstack.exception.NotFoundException if the tag is not
     *                                                             found
     */
    TagResponseDTO updateTag(Long id, TagUpdateRequestDTO dto);

    /**
     * Partially updates an existing tag (PATCH semantics).
     * <p>
     * Only non-null fields from the request DTO are applied. Fields that are
     * null in the DTO retain their current values.
     *
     * @param id  the unique identifier of the tag to patch
     * @param dto the data transfer object containing the fields to update
     * @return a response DTO containing the updated tag's details
     * @throws com.jfontdev.trackstack.exception.NotFoundException if the tag is not
     *                                                             found
     */
    TagResponseDTO patchTag(Long id, TagPatchRequestDTO dto);

    /**
     * Deletes a tag by its unique identifier.
     * <p>
     * The tag is removed from the database along with all its join table
     * associations (track relationships) thanks to ON DELETE CASCADE on
     * the foreign keys.
     *
     * @param id the unique identifier of the tag to delete
     * @throws com.jfontdev.trackstack.exception.NotFoundException if the tag is not
     *                                                             found
     */
    void deleteTag(Long id);
}
