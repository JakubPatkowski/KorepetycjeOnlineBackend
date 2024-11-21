package com.example.demo.service.entity;

import com.example.demo.dto.mapper.UserProfileMapper;
import com.example.demo.dto.userProfile.LoggedInUserProfileDTO;
import com.example.demo.dto.userProfile.UserProfileUpdateDTO;
import com.example.demo.entity.UserProfileEntity;
import com.example.demo.repository.UserProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    @Autowired
    private final UserProfileRepository userProfileRepository;

    @Autowired
    private final UserProfileMapper userProfileMapper;


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

    @Transactional
    public LoggedInUserProfileDTO getUserProfile(Long loggedInUserId) {
        UserProfileEntity userProfile = userProfileRepository.findByUserId(loggedInUserId)
                .orElseThrow(() -> new EntityNotFoundException("User Profile not found"));


        return userProfileMapper.mapToDTO(userProfile);
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
