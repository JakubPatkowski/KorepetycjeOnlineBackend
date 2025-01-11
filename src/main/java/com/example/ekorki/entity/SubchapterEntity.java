package com.example.ekorki.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(schema = "e_korki", name = "subchapters")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SubchapterEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private ChapterEntity chapter;

    @Column(nullable = false)
    private String name;

    @Column(name = "\"order\"")
    private Integer order;

    @OneToMany(mappedBy = "subchapter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContentItemEntity> content;
}
