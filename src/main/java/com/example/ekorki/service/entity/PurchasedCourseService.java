package com.example.ekorki.service.entity;

import com.example.ekorki.dto.courseShop.CourseShopResponseDTO;
import com.example.ekorki.entity.CourseEntity;
import com.example.ekorki.entity.PaymentHistoryEntity;
import com.example.ekorki.entity.PurchasedCourseEntity;
import com.example.ekorki.entity.UserEntity;
import com.example.ekorki.exception.ApiException;
import com.example.ekorki.repository.CourseRepository;
import com.example.ekorki.repository.PurchasedCourseRepository;
import com.example.ekorki.repository.UserRepository;
import com.example.ekorki.service.CourseShopService;
import com.example.ekorki.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final CourseShopService courseShopService;
    private final PaymentHistoryService paymentHistoryService;
    private final EmailService emailService;

    @Transactional
    @CacheEvict(value = "courses", allEntries = true)
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

            paymentHistoryService.addTransaction(
                    buyerId,
                    PaymentHistoryEntity.TransactionType.COURSE_PURCHASE,
                    -coursePrice,
                    "Purchase of course" + course.getName(),
                    courseId,
                    PaymentHistoryEntity.RelatedEntityType.COURSE
            );

            paymentHistoryService.addTransaction(
                    courseOwner.getId(),
                    PaymentHistoryEntity.TransactionType.COURSE_SOLD,
                    calculateOwnerShare(coursePrice),
                    "purchased access to the course: " + course.getName(),
                    courseId,
                    PaymentHistoryEntity.RelatedEntityType.COURSE
            );

            emailService.sendCoursePurchaseConfirmation(
                    buyer.getEmail(),
                    course.getName(),
                    coursePrice,
                    buyer.getPoints()
            );

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

    @Transactional(readOnly = true)
    public Page<CourseShopResponseDTO> getPurchasedCourses(Long userId, int page, int size) {
        validatePaginationParams(page, size);

        List<PurchasedCourseEntity> allPurchases = purchasedCourseRepository.findByUserId(userId);

        int start = page * size;
        int end = Math.min(start + size, allPurchases.size());

        List<CourseShopResponseDTO> purchasedCourseDTOs = allPurchases.subList(start, end).stream()
                .map(purchase -> courseShopService.mapToCourseShopResponseDTO(purchase.getCourse()))
                .collect(Collectors.toList());

        return new PageImpl<>(
                purchasedCourseDTOs,
                PageRequest.of(page, size),
                allPurchases.size()
        );
    }

    @Transactional(readOnly = true)
    public boolean canReviewTeacher(Long userId, Long teacherId) {
        try {
            // Sprawdzenie czy użytkownik kupił jakikolwiek kurs tego nauczyciela
            return purchasedCourseRepository.existsByUserIdAndCourseUserId(userId, teacherId);
        } catch (Exception e) {
            throw new ApiException("Error checking review permission: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public boolean hasUserPurchasedCourse(Long userId, Long courseId) {
        return purchasedCourseRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    private void validatePaginationParams(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
    }
}
