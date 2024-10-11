package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;


import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@Data
@Entity
@Table(schema = "demo", name = "users")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_DEFAULT)
public class User{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotNull(message = "Email cannot be empty")
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Enter correct email")
    private String email;

    @Column( nullable = false, unique = true, length = 60, name = "password_hash")
    @NotEmpty(message = "password cannot be empty")
    private String password; //password_hash

    @Column(nullable = false)
    private int points = 0;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private boolean blocked = false;

    @Column(nullable = false)
    private boolean mfa = false;

    public enum Role implements GrantedAuthority{
        USER, TEACHER, ADMIN;

        @Override
        public String getAuthority() {
            return name();
        }
    }

    @Column(nullable = false) //columnDefinition = "role_enum"
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;
}
