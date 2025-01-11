package com.example.ekorki.service.entity;

import com.example.ekorki.dto.task.TaskCreateDTO;
import com.example.ekorki.dto.task.TaskResponseDTO;
import com.example.ekorki.entity.TaskEntity;
import com.example.ekorki.entity.UserEntity;
import com.example.ekorki.exception.ApiException;
import com.example.ekorki.repository.TaskRepository;
import com.example.ekorki.repository.UserRepository;
import com.example.ekorki.service.CourseShopService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.ekorki.entity.PaymentHistoryEntity;


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
    private final PaymentHistoryService paymentHistoryService;

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

        paymentHistoryService.addTransaction(
                studentId,
                PaymentHistoryEntity.TransactionType.TASK_PUBLISHED,
                task.getPrice(),
                "posting a task: " + task.getTitle(),
                task.getId(),
                PaymentHistoryEntity.RelatedEntityType.TASK
        );

        return mapToTaskResponseDTO(savedTask);
    }

    @Transactional(readOnly = true)
    public TaskResponseDTO getTask(Long taskId) {
        TaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
        return mapToTaskResponseDTO(task);
    }

//    @Transactional(readOnly = true)
//    public Page<TaskResponseDTO> getPublicTasks(int page, int size) {
//        validatePaginationParams(page, size);
//
//        Page<TaskEntity> tasks = taskRepository.findPublicActiveTasks(
//                PageRequest.of(page, size)
//        );
//
//        return tasks.map(this::mapToTaskListItemDTO);
//    }

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

//    private TaskResponseDTO mapToTaskResponseDTO(TaskEntity task) {
//        Map<String, Object> fileData = null;
//        if (task.getFile() != null) {
//            fileData = new HashMap<>();
//            fileData.put("data", task.getFile());
//            fileData.put("mimeType", task.getMimeType());
//        }
//
//        return TaskResponseDTO.builder()
//                .id(task.getId())
//                .title(task.getTitle())
//                .content(task.getContent())
//                .createdAt(task.getCreatedAt())
//                .endDate(task.getEndDate())
//                .file(fileData)
//                .price(task.getPrice())
//                .solutionTimeMinutes(task.getSolutionTimeMinutes())
//                .isPublic(task.getIsPublic())
//                .isActive(task.getIsActive())
//                .ownerData()
//                .assignedTeacher()
//                .assignedAt()
//
//                .remainingMinutes(ChronoUnit.MINUTES.between(LocalDateTime.now(), task.getEndDate()))
//                .status(task.getStatus().name())
//                .ownerData(courseShopService.mapToOwnerDataDTO(
//                        userProfileService.getUserProfileEntity(task.getStudent().getId())))
//                .assignedTeacher(task.getAssignedTeacher() != null ?
//                        courseShopService.mapToOwnerDataDTO(
//                                userProfileService.getUserProfileEntity(task.getAssignedTeacher().getId()))
//                        : null)
//                .build();
//    }

    private void validatePaginationParams(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
    }
}