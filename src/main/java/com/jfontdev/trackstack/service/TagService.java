package com.jfontdev.trackstack.service;

import com.jfontdev.trackstack.dto.tag.TagRequestDTO;
import com.jfontdev.trackstack.dto.tag.TagResponseDTO;

import java.util.List;

public interface TagService {
    TagResponseDTO createTag(TagRequestDTO dto);

    TagResponseDTO getTagById(Long id);

    List<TagResponseDTO> getAllTags();
}