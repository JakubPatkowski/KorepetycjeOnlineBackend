package com.example.demo.service.entity;

import com.example.demo.dto.course.CourseInfoDTO;
import com.example.demo.entity.CourseEntity;
import com.example.demo.entity.PurchasedCourseEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.PurchasedCourseRepository;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchasedCourseService {
    private final PurchasedCourseRepository purchasedCourseRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final PointsService pointsService;
    private final CourseService courseService;

    @Transactional
    public boolean purchaseCourse(Long courseId, Long userId) {
        if (purchasedCourseRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ApiException("You already own this course");
        }

        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException("Course not found"));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));

        int requiredPoints = course.getPrice().intValue();

        if (!pointsService.hasEnoughPoints(userId, requiredPoints)) {
            throw new ApiException("Insufficient points to purchase this course");
        }

        try {
            pointsService.deductPoints(userId, requiredPoints);

            PurchasedCourseEntity purchase = PurchasedCourseEntity.builder()
                    .user(user)
                    .course(course)
                    .purchaseDate(Instant.now())
                    .pointsSpent(requiredPoints)
                    .build();

            purchasedCourseRepository.save(purchase);
            return true;
        } catch (Exception e) {
            // If something goes wrong, return points to user
            pointsService.addPoints(userId, requiredPoints);
            throw new ApiException("Failed to process purchase: " + e.getMessage());
        }
    }

    @Transactional
    public List<CourseInfoDTO> getPurchasedCourses(Long userId) {
        return purchasedCourseRepository.findByUserId(userId).stream()
                .map(purchase -> courseService.mapToCourseInfo(purchase.getCourse()))
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean hasUserPurchasedCourse(Long userId, Long courseId) {
        return purchasedCourseRepository.existsByUserIdAndCourseId(userId, courseId);
    }
}
