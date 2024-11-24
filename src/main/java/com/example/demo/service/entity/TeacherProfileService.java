package com.example.demo.service.entity;

import com.example.demo.entity.TeacherProfileEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.TeacherProfileRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TeacherProfileService {
    private final TeacherProfileRepository teacherProfileRepository;
    private final UserRepository userRepository;

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
}
