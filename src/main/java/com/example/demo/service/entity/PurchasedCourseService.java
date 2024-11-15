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
    public boolean purchaseCourse(Long courseId, Long buyerId) {
        // Sprawdzenie czy kurs istnieje
        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException("Course not found"));

        // Sprawdzenie czy użytkownik istnieje
        UserEntity buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ApiException("Buyer not found"));

        // Sprawdzenie czy użytkownik nie jest właścicielem kursu
        if (course.getUser().getId().equals(buyerId)) {
            throw new ApiException("You cannot purchase your own course");
        }

        // Sprawdzenie czy użytkownik już nie kupił tego kursu
        if (purchasedCourseRepository.existsByUserIdAndCourseId(buyerId, courseId)) {
            throw new ApiException("You already own this course");
        }

        // Pobierz właściciela kursu
        UserEntity courseOwner = course.getUser();
        int coursePrice = course.getPrice().intValue();

        // Sprawdź czy kupujący ma wystarczająco punktów
        if (!pointsService.hasEnoughPoints(buyerId, coursePrice)) {
            throw new ApiException("Insufficient points to purchase this course");
        }

        try {
            // Rozpoczęcie transakcji punktowej
            pointsService.deductPoints(buyerId, coursePrice);
            pointsService.addPoints(courseOwner.getId(), calculateOwnerShare(coursePrice));

            // Zapisz informację o zakupie
            PurchasedCourseEntity purchase = PurchasedCourseEntity.builder()
                    .user(buyer)
                    .course(course)
                    .purchaseDate(Instant.now())
                    .pointsSpent(coursePrice)
                    .build();

            purchasedCourseRepository.save(purchase);
            return true;

        } catch (Exception e) {
            pointsService.addPoints(buyerId, coursePrice);
            if (courseOwner != null) {
                pointsService.deductPoints(courseOwner.getId(), calculateOwnerShare(coursePrice));
            }
            throw new ApiException("Failed to process purchase: " + e.getMessage());
        }
    }

    private int calculateOwnerShare(int coursePrice) {
        final double PLATFORM_FEE_PERCENTAGE = 0.10;
        return (int) (coursePrice * (1 - PLATFORM_FEE_PERCENTAGE));
    }

    @Transactional
    public List<CourseInfoDTO> getPurchasedCourses(Long userId) {
        return purchasedCourseRepository.findByUserId(userId).stream()
                .map(purchase -> courseService.mapToCourseInfo(purchase.getCourse()))
                .collect(Collectors.toList());
    }

//    @Transactional
//    public boolean hasUserPurchasedCourse(Long userId, Long courseId) {
//        return purchasedCourseRepository.existsByUserIdAndCourseId(userId, courseId);
//    }
}
