//package com.example.ekorki.entity;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.hibernate.annotations.JdbcTypeCode;
//
//import java.sql.Types;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(schema = "e_korki", name = "tasks")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class TaskEntity {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false)
//    private String title;
//
//    @Column(nullable = false, columnDefinition = "text")
//    private String content;
//
//    @Column(name = "created_at", nullable = false)
//    private LocalDateTime createdAt;
//
//    @Column(name = "end_date", nullable = false)
//    private LocalDateTime endDate;
//
//    @Lob
//    @JdbcTypeCode(Types.BINARY)
//    private byte[] file;
//
//    @Column(name = "mime_type")
//    private String mimeType;
//
//    @Column(nullable = false)
//    private Integer price;
//
//    @Column(name = "solution_time_minutes", nullable = false)
//    private Integer solutionTimeMinutes;
//
//    @Column(name = "is_public")
//    private Boolean isPublic;
//
//    @Column(name = "is_active")
//    private Boolean isActive;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "student_id", nullable = false)
//    private UserEntity student;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "assigned_teacher_id")
//    private UserEntity assignedTeacher;
//
//    @Column(name = "assigned_at")
//    private LocalDateTime assignedAt;
//
//    @Column(name = "solution_deadline")
//    private LocalDateTime solutionDeadline;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private TaskStatus status;
//
//    public enum TaskStatus {
//        OPEN, ASSIGNED, COMPLETED, EXPIRED
//    }
//}