package com.jfontdev.trackstack.service.impl;

import com.jfontdev.trackstack.dto.tag.TagRequestDTO;
import com.jfontdev.trackstack.dto.tag.TagResponseDTO;
import com.jfontdev.trackstack.exception.NotFoundException;
import com.jfontdev.trackstack.model.Tag;
import com.jfontdev.trackstack.repository.TagRepository;
import com.jfontdev.trackstack.service.TagService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    public TagServiceImpl(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Override
    public TagResponseDTO createTag(TagRequestDTO dto) {
        Tag tag = Tag.create(dto.name());
        Tag saved = tagRepository.saveAndFlush(tag);

        return new TagResponseDTO(
                saved.getId(),
                saved.getName()
        );
    }

    @Override
    public TagResponseDTO getTagById(Long id) {
        Optional<Tag> tag = tagRepository.findById(id);

        if (tag.isEmpty()) {
            throw new NotFoundException("Tag not found.");
        }

        Tag t = tag.get();

        return new TagResponseDTO(
                t.getId(),
                t.getName()
        );
    }

    @Override
    public List<TagResponseDTO> getAllTags() {
        return tagRepository.findAll()
                .stream()
                .map(t -> new TagResponseDTO(
                        t.getId(),
                        t.getName()
                ))
                .toList();
    }
}
