package com.jfontdev.trackstack.model;

import jakarta.persistence.*;

import java.util.Set;

@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany(mappedBy = "tags")
    private Set<Track> tracks;

    public Tag(String name) {
        this.name = name;
    }

    public Tag() {

    }

    public static Tag create(String name) {
        return new Tag(name);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
