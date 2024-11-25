package com.example.demo.service.entity;

import com.example.demo.dto.userProfile.UserProfileResponseDTO;
import com.example.demo.entity.TeacherProfileEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.TeacherProfileRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
@RequiredArgsConstructor
public class TeacherProfileService {
    @Autowired
    private final UserProfileService userProfileService;

    private final TeacherProfileRepository teacherProfileRepository;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(TeacherProfileService.class);

    @Transactional
    public void createTeacherProfile(Long userId) {
        if (teacherProfileRepository.existsByUserId(userId)) {
            throw new ApiException("Teacher profile already exists");
        }

        TeacherProfileEntity profile = TeacherProfileEntity.builder()
                .user(userRepository.findById(userId)
                        .orElseThrow(() -> new ApiException("User not found")))
                .review(BigDecimal.ZERO)
                .reviewNumber(0)
                .build();

        teacherProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponseDTO> getBestTeachers(Long loggedInUserId) {
        try {
            List<TeacherProfileEntity> bestTeachers = teacherProfileRepository.findBestTeachers(loggedInUserId);
            return bestTeachers.stream()
                    .map(teacherProfile -> userProfileService.getUserProfile(teacherProfile.getUser().getId()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error while fetching best teachers: {}", e.getMessage());
            throw new ApiException("Error retrieving best teachers: " + e.getMessage());
        }
    }
}
