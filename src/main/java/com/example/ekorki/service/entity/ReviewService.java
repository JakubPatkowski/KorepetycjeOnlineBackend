package com.example.ekorki.service.entity;

import com.example.ekorki.dto.review.ReviewResponseDTO;
import com.example.ekorki.dto.review.ReviewTargetType;
import com.example.ekorki.dto.userProfile.UserProfileResponseDTO;
import com.example.ekorki.entity.*;
import com.example.ekorki.exception.ApiException;
import com.example.ekorki.repository.*;
import com.example.ekorki.service.CourseShopService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.ekorki.dto.review.ReviewCreateDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;
    private final PurchasedCourseRepository purchasedCourseRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoleService roleService;
    private final TeacherProfileRepository teacherProfileRepository;
    private final CourseShopService courseShopService;

    @Transactional
    public void addCourseReview(Long courseId, Long userId, ReviewCreateDTO reviewDTO){
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));

        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException("Course not found"));

        if(!purchasedCourseRepository.existsByUserIdAndCourseId(userId,courseId)){
            throw new ApiException("You must purchase the course to review it");
        }

        if(reviewRepository.existsByUserIdAndTargetIdAndTargetType(userId, courseId, ReviewTargetType.COURSE)){
            throw new ApiException("You already reviewed this course");
        }

        ReviewEntity review = ReviewEntity.builder()
                .user(user)
                .targetId(courseId)
                .targetType(ReviewTargetType.COURSE)
                .rating(reviewDTO.getRating())
                .content(reviewDTO.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        reviewRepository.save(review);

        int currentReviewCount = course.getReviewNumber();
        BigDecimal currentAverage = course.getReview() != null ? course.getReview() : BigDecimal.ZERO;

        BigDecimal newTotal = currentAverage.multiply(BigDecimal.valueOf(currentReviewCount))
                .add(BigDecimal.valueOf(reviewDTO.getRating()));
        BigDecimal newAverage = newTotal.divide(BigDecimal.valueOf(currentReviewCount + 1), 2, RoundingMode.HALF_UP);

        course.setReview(newAverage);
        course.setReviewNumber(currentReviewCount + 1);
        courseRepository.save(course);
        courseShopService.evictBestCoursesCache(userId);

    }

    @Transactional
    public void addChapterReview(Long chapterId, Long userId, ReviewCreateDTO reviewDTO) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));

        ChapterEntity chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ApiException("Chapter not found"));

        if (!purchasedCourseRepository.existsByUserIdAndCourseId(userId, chapter.getCourse().getId())) {
            throw new ApiException("You must purchase the course to review its chapters");
        }

        if (reviewRepository.existsByUserIdAndTargetIdAndTargetType(userId, chapterId, ReviewTargetType.CHAPTER)) {
            throw new ApiException("You have already reviewed this chapter");
        }

        ReviewEntity review = ReviewEntity.builder()
                .user(user)
                .targetId(chapterId)
                .targetType(ReviewTargetType.CHAPTER)
                .rating(reviewDTO.getRating())
                .content(reviewDTO.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        reviewRepository.save(review);

        int currentReviewCount = chapter.getReviewNumber();
        BigDecimal currentAverage = chapter.getReview() != null ? chapter.getReview() : BigDecimal.ZERO;

        BigDecimal newTotal = currentAverage.multiply(BigDecimal.valueOf(currentReviewCount))
                .add(BigDecimal.valueOf(reviewDTO.getRating()));
        BigDecimal newAverage = newTotal.divide(BigDecimal.valueOf(currentReviewCount + 1), 2, RoundingMode.HALF_UP);

        chapter.setReview(newAverage);
        chapter.setReviewNumber(currentReviewCount + 1);
        chapterRepository.save(chapter);

        courseShopService.evictBestCoursesCache(userId);

    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getReviews(
            Long targetId,
            ReviewTargetType targetType,
            int page,
            int size,
            String sortBy,
            String sortDir,
            Long loggedInUserId) {  // Dodany parametr loggedInUserId

        try {
            validateSearchParams(page, size, sortBy, sortDir);
            long offset = calculateOffset(page, size);

            // Pobierz reviews z bazy z uwzględnieniem sortowania
            List<ReviewEntity> reviews = reviewRepository.findReviewsWithSorting(
                    targetId,
                    targetType.name(),
                    sortBy,
                    sortDir,
                    size,
                    offset
            );

            // Jeśli użytkownik jest zalogowany, odfiltruj jego recenzję
            if (loggedInUserId != null) {
                reviews = reviews.stream()
                        .filter(review -> !review.getUser().getId().equals(loggedInUserId))
                        .collect(Collectors.toList());
            }

            // Pobierz całkowitą liczbę recenzji (z wyłączeniem recenzji zalogowanego użytkownika jeśli istnieje)
            long total;
            if (loggedInUserId != null) {
                total = reviewRepository.countReviewsExcludingUser(targetId, targetType.name(), loggedInUserId);
            } else {
                total = reviewRepository.countReviews(targetId, targetType.name());
            }

            List<ReviewResponseDTO> dtos = reviews.stream()
                    .map(this::mapToReviewDTO)
                    .collect(Collectors.toList());

            return new PageImpl<>(dtos, PageRequest.of(page, size), total);
        } catch (Exception e) {
            throw new ApiException("Error while fetching reviews: " + e.getMessage());
        }
    }

    private ReviewResponseDTO mapToReviewDTO(ReviewEntity review) {
        UserProfileEntity userProfile = userProfileRepository.findByUserId(review.getUser().getId())
                .orElseThrow(() -> new ApiException("User profile not found"));

        // Pobierz role użytkownika
        Set<String> roles = roleService.getUserRoles(review.getUser().getId())
                .stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        // Zbuduj pełny profil użytkownika
        UserProfileResponseDTO userProfileDTO = UserProfileResponseDTO.builder()
                .id(userProfile.getId())
                .fullName(userProfile.getFullName())
                .description(userProfile.getDescription())
                .createdAt(userProfile.getCreatedAt())
                .picture(createPictureData(userProfile))
                .badgesVisible(userProfile.getBadgesVisible())
                .roles(roles)
                .build();

        // Jeśli użytkownik jest nauczycielem, dodaj dane z profilu nauczyciela
        if (roles.contains("TEACHER")) {
            TeacherProfileEntity teacherProfile = teacherProfileRepository.findByUserId(review.getUser().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Teacher profile not found"));

            userProfileDTO = userProfileDTO.toBuilder()
                    .review(teacherProfile.getReview())
                    .reviewNumber(teacherProfile.getReviewNumber())
                    .teacherProfileCreatedAt(teacherProfile.getCreatedAt())
                    .build();
        }

        return ReviewResponseDTO.builder()
                .id(review.getId())
                .rating(review.getRating())
                .content(review.getContent())
                .lastModified(review.getUpdatedAt() != null ? review.getUpdatedAt() : review.getCreatedAt())
                .userProfile(userProfileDTO)
                .build();
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

    private void validateSearchParams(int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        List<String> validSortOptions = Arrays.asList("rating", "date");
        if (!validSortOptions.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort option. Valid options are: rating, date");
        }

        List<String> validSortDirections = Arrays.asList("asc", "desc");
        if (!validSortDirections.contains(sortDir)) {
            throw new IllegalArgumentException("Invalid sort direction. Valid options are: asc, desc");
        }
    }

    private long calculateOffset(int page, int size) {
        if (page > Integer.MAX_VALUE / size) {
            throw new IllegalArgumentException("Page number too large");
        }
        return (long) page * size;
    }

    @Transactional
    public void updateReview(Long reviewId, Long userId, ReviewCreateDTO updateDTO) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException("Review not found"));

        // Sprawdź czy użytkownik jest właścicielem recenzji
        if (!review.getUser().getId().equals(userId)) {
            throw new ApiException("You can only edit your own reviews");
        }

        // Sprawdź czy ocena się zmieniła
        boolean ratingChanged = !review.getRating().equals(updateDTO.getRating());
        int oldRating = review.getRating();

        // Aktualizuj recenzję
        review.setRating(updateDTO.getRating());
        review.setContent(updateDTO.getContent());
        review.setUpdatedAt(LocalDateTime.now());
        reviewRepository.save(review);

        // Jeśli ocena się zmieniła, zaktualizuj średnią ocenę dla kursu/rozdziału
        if (ratingChanged) {
            updateTargetRatingAfterEdit(review.getTargetId(), review.getTargetType(), oldRating, updateDTO.getRating());
        }

        courseShopService.evictBestCoursesCache(userId);

    }

    private void updateTargetRatingAfterEdit(Long targetId, ReviewTargetType targetType, int oldRating, int newRating) {
        switch (targetType) {
            case COURSE -> {
                CourseEntity course = courseRepository.findById(targetId)
                        .orElseThrow(() -> new ApiException("Course not found"));

                int reviewCount = course.getReviewNumber();
                BigDecimal currentTotal = course.getReview().multiply(BigDecimal.valueOf(reviewCount));

                // Odejmij starą ocenę i dodaj nową
                currentTotal = currentTotal.subtract(BigDecimal.valueOf(oldRating))
                        .add(BigDecimal.valueOf(newRating));

                BigDecimal newAverage = currentTotal.divide(BigDecimal.valueOf(reviewCount), 2, RoundingMode.HALF_UP);
                course.setReview(newAverage);
                courseRepository.save(course);

            }
            case CHAPTER -> {
                ChapterEntity chapter = chapterRepository.findById(targetId)
                        .orElseThrow(() -> new ApiException("Chapter not found"));

                int reviewCount = chapter.getReviewNumber();
                BigDecimal currentTotal = chapter.getReview().multiply(BigDecimal.valueOf(reviewCount));

                // Odejmij starą ocenę i dodaj nową
                currentTotal = currentTotal.subtract(BigDecimal.valueOf(oldRating))
                        .add(BigDecimal.valueOf(newRating));

                BigDecimal newAverage = currentTotal.divide(BigDecimal.valueOf(reviewCount), 2, RoundingMode.HALF_UP);
                chapter.setReview(newAverage);
                chapterRepository.save(chapter);
            }
        }
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException("Review not found"));

        // Sprawdź czy użytkownik jest właścicielem recenzji
        if (!review.getUser().getId().equals(userId)) {
            throw new ApiException("You can only delete your own reviews");
        }

        // Zaktualizuj średnią ocenę przed usunięciem recenzji
        updateTargetRatingAfterDelete(review);

        // Usuń recenzję
        reviewRepository.delete(review);

        courseShopService.evictBestCoursesCache(userId);

    }

    private void updateTargetRatingAfterDelete(ReviewEntity review) {
        switch (review.getTargetType()) {
            case COURSE -> {
                CourseEntity course = courseRepository.findById(review.getTargetId())
                        .orElseThrow(() -> new ApiException("Course not found"));

                int newReviewCount = course.getReviewNumber() - 1;

                if (newReviewCount > 0) {
                    BigDecimal currentTotal = course.getReview()
                            .multiply(BigDecimal.valueOf(course.getReviewNumber()))
                            .subtract(BigDecimal.valueOf(review.getRating()));
                    BigDecimal newAverage = currentTotal.divide(BigDecimal.valueOf(newReviewCount), 2, RoundingMode.HALF_UP);
                    course.setReview(newAverage);
                } else {
                    course.setReview(BigDecimal.ZERO);
                }

                course.setReviewNumber(newReviewCount);
                courseRepository.save(course);
            }
            case CHAPTER -> {
                ChapterEntity chapter = chapterRepository.findById(review.getTargetId())
                        .orElseThrow(() -> new ApiException("Chapter not found"));

                int newReviewCount = chapter.getReviewNumber() - 1;

                if (newReviewCount > 0) {
                    BigDecimal currentTotal = chapter.getReview()
                            .multiply(BigDecimal.valueOf(chapter.getReviewNumber()))
                            .subtract(BigDecimal.valueOf(review.getRating()));
                    BigDecimal newAverage = currentTotal.divide(BigDecimal.valueOf(newReviewCount), 2, RoundingMode.HALF_UP);
                    chapter.setReview(newAverage);
                } else {
                    chapter.setReview(BigDecimal.ZERO);
                }

                chapter.setReviewNumber(newReviewCount);
                chapterRepository.save(chapter);
            }
        }
    }

    @Transactional(readOnly = true)
    public ReviewResponseDTO getUserReview(Long targetId, ReviewTargetType targetType, Long userId) {
        ReviewEntity review = reviewRepository.findByUserIdAndTargetIdAndTargetType(userId, targetId, targetType)
                .orElseThrow(() -> new ApiException("Review not found"));

        return mapToReviewDTO(review);
    }


    @Transactional
    public void addTeacherReview(Long teacherId, Long userId, ReviewCreateDTO reviewDTO) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));

        TeacherProfileEntity teacherProfile = teacherProfileRepository.findByUserId(teacherId)
                .orElseThrow(() -> new ApiException("Teacher not found"));

        // Sprawdź czy użytkownik nie próbuje ocenić samego siebie
        if (userId.equals(teacherId)) {
            throw new ApiException("You cannot review yourself");
        }

        // Sprawdź czy już nie dodał recenzji
        if (reviewRepository.existsByUserIdAndTargetIdAndTargetType(userId, teacherId, ReviewTargetType.TEACHER)) {
            throw new ApiException("You have already reviewed this teacher");
        }

        // Sprawdź czy użytkownik kupił jakikolwiek kurs tego nauczyciela
        boolean hasPurchasedCourse = purchasedCourseRepository.existsByUserIdAndCourseUserId(userId, teacherId);
        if (!hasPurchasedCourse) {
            throw new ApiException("You must purchase a course from this teacher to review them");
        }

        // TODO: W przyszłości dodać tutaj sprawdzenie czy użytkownik ma wykonane zadanie od nauczyciela
        // boolean hasCompletedTask = taskRepository.existsByStudentIdAndTeacherIdAndStatus(userId, teacherId, TaskStatus.COMPLETED);
        // if (!hasPurchasedCourse && !hasCompletedTask) {
        //     throw new ApiException("You must either purchase a course or have a completed task from this teacher to review them");
        // }

        ReviewEntity review = ReviewEntity.builder()
                .user(user)
                .targetId(teacherId)
                .targetType(ReviewTargetType.TEACHER)
                .rating(reviewDTO.getRating())
                .content(reviewDTO.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        reviewRepository.save(review);

        // Aktualizuj średnią ocenę nauczyciela
        int currentReviewCount = teacherProfile.getReviewNumber();
        BigDecimal currentAverage = teacherProfile.getReview() != null ? teacherProfile.getReview() : BigDecimal.ZERO;

        BigDecimal newTotal = currentAverage.multiply(BigDecimal.valueOf(currentReviewCount))
                .add(BigDecimal.valueOf(reviewDTO.getRating()));
        BigDecimal newAverage = newTotal.divide(BigDecimal.valueOf(currentReviewCount + 1), 2, RoundingMode.HALF_UP);

        teacherProfile.setReview(newAverage);
        teacherProfile.setReviewNumber(currentReviewCount + 1);
        teacherProfile.setUpdatedAt(LocalDateTime.now());
        teacherProfileRepository.save(teacherProfile);
    }
}
