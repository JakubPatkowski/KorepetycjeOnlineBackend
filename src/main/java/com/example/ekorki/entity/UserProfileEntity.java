package com.example.ekorki.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@Data
@Entity
@Table(schema = "e_korki", name = "user_profiles")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_DEFAULT)
public class UserProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "Name and Surname cannot be empty")
    @Column(nullable = false, length = 50, name = "full_name")
    private String fullName;

    @Column(nullable = false, name = "user_id")
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private Long userId;

    @Column(length = 5000)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    @Lob
    @JdbcTypeCode(Types.BINARY)
    private byte[] picture;

    @Column(name = "mime_type")
    private String pictureMimeType;

    @Column(name = "badges_visible")
    private Boolean badgesVisible;
}
