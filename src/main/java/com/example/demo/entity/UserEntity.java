package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;


import java.util.HashSet;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema = "demo", name = "users")
@SuperBuilder
@JsonInclude(NON_DEFAULT)
public class UserEntity {
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
    private boolean blocked = false;

//    public enum Role implements GrantedAuthority{
//        USER, VERIFIED, TEACHER, ADMIN;
//
//        @Override
//        public String getAuthority() {
//            return name();
//        }
//    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<RoleEntity> roles = new HashSet<>();
}
