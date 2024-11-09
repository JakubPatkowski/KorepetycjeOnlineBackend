package com.example.demo.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.sql.Types;

@Entity
@Table(name = "content_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ContentItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subchapter_id")
    private SubchapterEntity subchapter;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "\"order\"", nullable = false)
    private Integer order;

    @Column(name = "text", columnDefinition = "text")
    private String text;

    @Column(name = "font_size", length = 20)
    private String fontSize;

    @Column(name = "bolder", length = 20)
    private Boolean bolder;

    @Column(name = "text_color")
    private String textColor;

    @Column
    private Boolean italics;

    @Column
    private Boolean underline;

    @Lob
    @JdbcTypeCode(Types.BINARY)
    private byte[] file;

    @Column(name = "mime_type")
    private String mimeType;

    @Type(JsonBinaryType.class)
    @Column(name = "quiz_data", columnDefinition = "jsonb")
    private String quizContent;
}
