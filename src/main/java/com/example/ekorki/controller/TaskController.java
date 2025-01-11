package com.example.ekorki.controller;

import com.example.ekorki.dto.http.HttpResponseDTO;
import com.example.ekorki.dto.task.TaskCreateDTO;
import com.example.ekorki.dto.task.TaskResponseDTO;
import com.example.ekorki.model.UserPrincipals;
import com.example.ekorki.service.entity.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static java.time.LocalDateTime.now;

@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HttpResponseDTO> createTask(
            @RequestPart("taskData") @Valid String taskDataJson,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Authentication authentication) {
        try {
            TaskCreateDTO taskCreateDTO = objectMapper.readValue(taskDataJson, TaskCreateDTO.class);
            Long studentId = ((UserPrincipals) authentication.getPrincipal()).getId();
            TaskResponseDTO createdTask = taskService.createTask(taskCreateDTO, file, studentId);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .data(Map.of("task", createdTask))
                    .message("Task created successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message(e.getMessage())
                    .status(HttpStatus.BAD_REQUEST)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }

    @GetMapping("/get/{taskId}")
    public ResponseEntity<HttpResponseDTO> getTask(@PathVariable Long taskId) {
        try {
            TaskResponseDTO task = taskService.getTask(taskId);

            return ResponseEntity.ok(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .data(Map.of("task", task))
                    .message("Task retrieved successfully")
                    .status(HttpStatus.OK)
                    .statusCode(HttpStatus.OK.value())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message(e.getMessage())
                    .status(HttpStatus.BAD_REQUEST)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
}