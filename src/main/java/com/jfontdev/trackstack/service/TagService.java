package com.jfontdev.trackstack.service;

import com.jfontdev.trackstack.dto.tag.TagRequestDTO;
import com.jfontdev.trackstack.dto.tag.TagResponseDTO;

import java.util.List;

/**
 * Service interface for managing {@link com.jfontdev.trackstack.model.Tag}
 * entities.
 * <p>
 * This interface defines the contract for tag-related business operations.
 * By using an interface, we decouple the controller from the specific
 * implementation,
 * making the code easier to test and maintain.
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
}