package com.example.ekorki.dto.task;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TaskCreateDTO {
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "End date is required")
    private String endDate;

    @NotNull(message = "Price is required")
    private Integer price;

    @NotNull(message = "Solution time is required")
    @Max(value = 1440, message = "Maximum solution time is 24 hours (1440 minutes)")
    private Integer solutionTimeMinutes;

    @NotNull(message = "Visibility setting is required")
    private Boolean isPublic;
}

