package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(schema = "demo", name = "purchased_courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PurchasedCourseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id")
    private CourseEntity course;

    @Column(name = "purchase_date")
    private Instant purchaseDate;

    @Column(name = "points_spent")
    private Integer pointsSpent;
}
