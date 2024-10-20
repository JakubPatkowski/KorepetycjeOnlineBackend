package com.example.demo.service;

import com.example.demo.dto.UserProfileUpdateDTO;
import com.example.demo.entity.UserProfileEntity;
import com.example.demo.repository.UserProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    @Autowired
    private final UserProfileRepository userProfileRepository;

    void addUserProfileToUser(String nameAndSurname, Long userId){
        UserProfileEntity userProfileEntity = new UserProfileEntity();
        userProfileEntity.setFullName(nameAndSurname);
        userProfileEntity.setUserId(userId);
        userProfileEntity.setCreatedAt(new Date());
        userProfileRepository.save(userProfileEntity);
    }

    @Transactional
    public boolean updateUserProfile(UserProfileUpdateDTO updateDTO, Long loggedInUserId) {
        if (!updateDTO.getUserId().equals(loggedInUserId)) {
            throw new AccessDeniedException("You don not have permissions to modify this profile");
        }

        UserProfileEntity userProfile = userProfileRepository.findByUserId(updateDTO.getUserId());

        if (userProfile == null) {
            throw new EntityNotFoundException("User Profile not found");
        }

        if (updateDTO.getFullName() != null) {
            updateDTO.getFullName().ifPresent(userProfile::setFullName);
        }
        if (updateDTO.getDescription() != null) {
            updateDTO.getDescription().ifPresent(userProfile::setDescription);
        }
        if (updateDTO.getPicture() != null) {
            updateDTO.getPicture().ifPresent(userProfile::setPicture);
        }

        userProfileRepository.save(userProfile);
        return true;
    }
}
