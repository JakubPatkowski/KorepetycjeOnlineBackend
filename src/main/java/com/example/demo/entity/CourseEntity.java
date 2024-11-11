package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Lob
    @JdbcTypeCode(Types.BINARY)
    private byte[] banner;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(precision = 2, scale = 1)
    private BigDecimal review;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;



    @Column(precision = 10, scale = 2)
    private BigDecimal duration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(
            name = "tags",
            columnDefinition = "varchar(255)[]"
    )
    private List<String> tags;

    @Column(name = "review_number")
    private Integer reviewNumber;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "course",fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChapterEntity> chapters;
}
