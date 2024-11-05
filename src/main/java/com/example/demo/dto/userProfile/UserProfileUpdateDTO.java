package com.example.demo.dto.userProfile;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateDTO {
    private Optional<String> fullName;
    private Optional<String> description;
    private Optional<Boolean> badgesVisible;
}
