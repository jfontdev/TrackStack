package com.jfontdev.trackstack.controller;


import com.jfontdev.trackstack.dto.tag.TagRequestDTO;
import com.jfontdev.trackstack.dto.tag.TagResponseDTO;
import com.jfontdev.trackstack.service.TagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping
    public ResponseEntity<TagResponseDTO> create(@Valid @RequestBody TagRequestDTO dto) {
        TagResponseDTO response = tagService.createTag(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public TagResponseDTO getById(@PathVariable Long id) {
        return tagService.getTagById(id);
    }

    @GetMapping
    public List<TagResponseDTO> getAll() {
        return tagService.getAllTags();
    }
}
