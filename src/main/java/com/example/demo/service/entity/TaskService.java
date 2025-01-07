package com.example.demo.service.entity;

import com.example.demo.dto.task.TaskCreateDTO;
import com.example.demo.dto.task.TaskResponseDTO;
import com.example.demo.entity.TaskEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CourseShopService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final PointsService pointsService;
    private final CourseShopService courseShopService;
    private final UserProfileService userProfileService;

    @Transactional
    public TaskResponseDTO createTask(TaskCreateDTO createDTO, MultipartFile file, Long studentId) {
        UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        // Sprawdź czy student ma wystarczająco punktów
        if (!pointsService.hasEnoughPoints(studentId, createDTO.getPrice())) {
            throw new ApiException("Insufficient points balance");
        }

        TaskEntity task = TaskEntity.builder()
                .title(createDTO.getTitle())
                .content(createDTO.getContent())
                .createdAt(LocalDateTime.now())
                .endDate(LocalDateTime.parse(createDTO.getEndDate(), DateTimeFormatter.ISO_DATE_TIME))
                .price(createDTO.getPrice())
                .solutionTimeMinutes(createDTO.getSolutionTimeMinutes())
                .isPublic(createDTO.getIsPublic())
                .isActive(true)
                .student(student)
                .status(TaskEntity.TaskStatus.OPEN)
                .build();

        if (file != null && !file.isEmpty()) {
            try {
                validateFile(file);
                task.setFile(file.getBytes());
                task.setMimeType(file.getContentType());
            } catch (IOException e) {
                throw new ApiException("Error processing file", e);
            }
        }

        // Pobierz punkty od studenta
        pointsService.deductPoints(studentId, createDTO.getPrice());

        TaskEntity savedTask = taskRepository.save(task);
        return mapToTaskResponseDTO(savedTask);
    }

    @Transactional(readOnly = true)
    public TaskResponseDTO getTask(Long taskId) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
        return mapToTaskResponseDTO(task);
    }

    private TaskResponseDTO mapToTaskResponseDTO(TaskEntity task) {
        Map<String, Object> fileData = null;
        if (task.getFile() != null) {
            fileData = new HashMap<>();
            fileData.put("data", task.getFile());
            fileData.put("mimeType", task.getMimeType());
        }

        return TaskResponseDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .content(task.getContent())
                .createdAt(task.getCreatedAt())
                .endDate(task.getEndDate())
                .file(fileData)
                .price(task.getPrice())
                .solutionTimeMinutes(task.getSolutionTimeMinutes())
                .isPublic(task.getIsPublic())
                .isActive(task.getIsActive())
                .ownerData(courseShopService.mapToOwnerDataDTO(
                        userProfileService.getUserProfileEntity(task.getStudent().getId())))
                .assignedTeacher(task.getAssignedTeacher() != null ?
                        courseShopService.mapToOwnerDataDTO(
                                userProfileService.getUserProfileEntity(task.getAssignedTeacher().getId())) : null)
                .assignedAt(task.getAssignedAt())
                .solutionDeadline(task.getSolutionDeadline())
                .status(task.getStatus().name())
                .build();
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
            throw new ApiException("File size exceeds maximum limit of 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null ||
                !(contentType.startsWith("image/") || contentType.startsWith("application/pdf"))) {
            throw new ApiException("Only image and PDF files are allowed");
        }
    }
}