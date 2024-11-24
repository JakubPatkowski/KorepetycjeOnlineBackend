package com.example.demo.service.entity;

import com.example.demo.dto.userProfile.UserProfileResponseDTO;
import com.example.demo.dto.userProfile.UserProfileUpdateDTO;
import com.example.demo.entity.RoleEntity;
import com.example.demo.entity.TeacherProfileEntity;
import com.example.demo.entity.UserProfileEntity;
import com.example.demo.repository.TeacherProfileRepository;
import com.example.demo.repository.UserProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    @Autowired
    private final UserProfileRepository userProfileRepository;



    @Autowired
    private final RoleService roleService;

    @Autowired
    private final TeacherProfileRepository teacherProfileRepository;


    void addUserProfileToUser(String nameAndSurname, Long userId){
        UserProfileEntity userProfileEntity = new UserProfileEntity();
        userProfileEntity.setFullName(nameAndSurname);
        userProfileEntity.setUserId(userId);
        userProfileEntity.setCreatedAt(new Date());
        userProfileRepository.save(userProfileEntity);
    }

    @Transactional
    public void updateUserProfile(UserProfileUpdateDTO updateDTO, MultipartFile picture, Long loggedInUserId) {
        UserProfileEntity userProfile = userProfileRepository.findByUserId(loggedInUserId)
                .orElseThrow(() -> new EntityNotFoundException("User Profile not found"));

        if (updateDTO.getFullName() != null) {
            updateDTO.getFullName().ifPresent(userProfile::setFullName);
        }
        if (updateDTO.getDescription() != null) {
            updateDTO.getDescription().ifPresent(userProfile::setDescription);
        }
        if (picture != null && !picture.isEmpty()) {
            try {
                validatePictureFile(picture);
                userProfile.setPicture(picture.getBytes());
                userProfile.setPictureMimeType(picture.getContentType());
            } catch (IOException e) {
                throw new RuntimeException("Failed to process picture file", e);
            }
        }
        if (updateDTO.getBadgesVisible() != null) {
            updateDTO.getBadgesVisible().ifPresent(userProfile::setBadgesVisible);
        }

        userProfileRepository.save(userProfile);
    }

    @Transactional(readOnly = true)
    public UserProfileResponseDTO getUserProfile(Long userId) {
        UserProfileEntity userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));

        Set<RoleEntity.Role> roles = roleService.getUserRoles(userId);
        UserProfileResponseDTO.UserProfileResponseDTOBuilder builder = UserProfileResponseDTO.builder()
                .id(userProfile.getId())
                .fullName(userProfile.getFullName())
                .description(userProfile.getDescription())
                .createdAt(userProfile.getCreatedAt())
                .picture(createPictureData(userProfile))
                .badgesVisible(userProfile.getBadgesVisible())
                .roles(roles.stream().map(Enum::name).collect(Collectors.toSet()));

        // Jeśli użytkownik jest nauczycielem, dodaj dane z profilu nauczyciela
        if (roles.contains(RoleEntity.Role.TEACHER)) {
            TeacherProfileEntity teacherProfile = teacherProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new EntityNotFoundException("Teacher profile not found"));

            builder.review(teacherProfile.getReview())
                    .reviewNumber(teacherProfile.getReviewNumber())
                    .teacherProfileCreatedAt(teacherProfile.getCreatedAt());
        }

        return builder.build();
    }

    private Map<String, Object> createPictureData(UserProfileEntity profile) {
        if (profile.getPicture() == null) {
            return null;
        }

        Map<String, Object> pictureData = new HashMap<>();
        pictureData.put("data", profile.getPicture());
        pictureData.put("mimeType", profile.getPictureMimeType());
        return pictureData;
    }

    private void validatePictureFile(MultipartFile file) {
        // Sprawdź rozmiar pliku (np. max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
        }

        // Sprawdź typ pliku
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
    }




}
