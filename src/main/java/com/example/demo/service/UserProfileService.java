package com.example.demo.service;

import com.example.demo.entity.UserProfileEntity;
import com.example.demo.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
}
