package com.example.ekorki.configuration;

import com.example.ekorki.dto.review.ReviewTargetType;
import com.example.ekorki.entity.*;
import com.example.ekorki.repository.*;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Profile("!prod") // Nie uruchamiaj na produkcji
public class DataInitializer {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoleRepository roleRepository;
    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final SubchapterRepository subchapterRepository;
    private final ContentItemRepository contentItemRepository;
    private final PurchasedCourseRepository purchasedCourseRepository;
    private final ReviewRepository reviewRepository;
    private final TeacherProfileRepository teacherProfileRepository;

    private final BCryptPasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private Random random = new Random();

    @PostConstruct
    public void init() {
        try {
            initializeData();
            logger.info("Successfully initialized demo data");
        } catch (Exception e) {
            logger.error("Failed to initialize demo data", e);
        }
    }

    @Transactional
    public void initializeData() throws IOException {
        if (userRepository.count() > 0) {
            logger.info("Database is not empty, skipping initialization");
            return;
        }

        // Stwórz użytkowników
        List<UserEntity> users = createUsers();

        // Dodaj profile użytkowników - usuń parametr defaultProfileImage
        createUserProfiles(users);

        // Dodaj role
        assignRoles(users);

        // Stwórz kursy dla nauczycieli - usuń parametr defaultCourseImage
        List<CourseEntity> courses = createCourses(users);

        // Dodaj rozdziały i podrozdziały
        createChaptersAndSubchapters(courses);

        // Dodaj zakupione kursy
        createPurchasedCourses(users, courses);

        // Dodaj recenzje do kursów
        createCourseReviews(users, courses);

        // Dodaj recenzje do nauczycieli
        createTeacherReviews(users);

    }

    private List<UserEntity> createUsers() {
        List<UserEntity> users = new ArrayList<>();
        String password = passwordEncoder.encode("Test123!@#");

        // Admin
        users.add(createSingleUser("admin@example.com", 5000, password));

        // 5 Teachers
        for (int i = 1; i <= 5; i++) {
            users.add(createSingleUser("teacher" + i + "@example.com", 2000 + random.nextInt(2000), password));
        }

        // 50 Verified users
        for (int i = 1; i <= 50; i++) {
            users.add(createSingleUser("verified" + i + "@example.com", 300 + random.nextInt(1700), password));
        }

        // Regular user
        users.add(createSingleUser("user@example.com", 100, password));

        return users;
    }

    private UserEntity createSingleUser(String email, int points, String password) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword(password);
        user.setPoints(points);
        user.setBlocked(false);
        return userRepository.save(user);
    }

    private void createUserProfiles(List<UserEntity> users) {
        // Admin profile
        createProfile(users.get(0), "John Admin", "Experienced administrator", 1);

        // Teacher profiles (index 1-5)
        String[] teacherNames = {
                "Alice Teacher", "Bob Instructor", "Carol Educator",
                "David Mentor", "Eva Professor"
        };
        for (int i = 0; i < 5; i++) {
            createProfile(
                    users.get(i + 1),
                    teacherNames[i],
                    "Professional educator with extensive teaching experience",
                    2 + random.nextInt(3) // losowe zdjęcie 2-4
            );
        }

        // Verified user profiles (index 6-56)
        String[] firstNames = {"Emma", "Noah", "Olivia", "Liam", "Ava", "William", "Sophia", "James",
                "Isabella", "Oliver", "Mia", "Benjamin", "Charlotte", "Elijah", "Amelia"};
        String[] lastNames = {"Smith", "Johnson", "Brown", "Davis", "Wilson", "Moore", "Taylor",
                "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin", "Thompson"};

        for (int i = 6; i <= 55; i++) {
            String randomName = firstNames[random.nextInt(firstNames.length)] + " " +
                    lastNames[random.nextInt(lastNames.length)];
            createProfile(
                    users.get(i),
                    randomName,
                    "Enthusiastic learner and knowledge seeker",
                    5 + random.nextInt(6) // losowe zdjęcie 5-10
            );
        }

        // Regular user profile
        createProfile(users.get(56), "Regular User", "New learner", 5 + random.nextInt(6));
    }

    private void createProfile(UserEntity user, String fullName, String description, int profileImageIndex) {
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUserId(user.getId());
        profile.setFullName(fullName);
        profile.setDescription(description);
        profile.setCreatedAt(new Date());
        profile.setPicture(loadProfileImage(profileImageIndex));
        profile.setPictureMimeType("image/png");
        profile.setBadgesVisible(true);
        userProfileRepository.save(profile);
    }

    private void assignRoles(List<UserEntity> users) {
        // Admin
        addRoles(users.get(0), Set.of(RoleEntity.Role.ADMIN, RoleEntity.Role.TEACHER,
                RoleEntity.Role.VERIFIED, RoleEntity.Role.USER));

        // Teachers (1-5)
        for (int i = 1; i <= 5; i++) {
            addRoles(users.get(i), Set.of(RoleEntity.Role.TEACHER,
                    RoleEntity.Role.VERIFIED, RoleEntity.Role.USER));
            createTeacherProfile(users.get(i));
        }

        // Verified users (6-55)
        for (int i = 6; i <= 55; i++) {
            addRoles(users.get(i), Set.of(RoleEntity.Role.VERIFIED, RoleEntity.Role.USER));
        }

        // Regular user
        addRoles(users.get(56), Set.of(RoleEntity.Role.USER));
    }

    private void addRoles(UserEntity user, Set<RoleEntity.Role> roles) {
        for (RoleEntity.Role role : roles) {
            RoleEntity roleEntity = new RoleEntity();
            roleEntity.setUser(user);
            roleEntity.setRole(role);
            roleRepository.save(roleEntity);
        }
    }

    private void createTeacherProfile(UserEntity user) {
        TeacherProfileEntity teacherProfile = TeacherProfileEntity.builder()
                .user(user)
                .review(BigDecimal.ZERO)
                .reviewNumber(0)
                .build();
        teacherProfileRepository.save(teacherProfile);
    }

    private List<CourseEntity> createCourses(List<UserEntity> users) {
        List<CourseEntity> courses = new ArrayList<>();

        // Lista możliwych kursów z kategoriami
        String[][] courseTemplates = {
                {"Python Programming", "programming,python,backend"},
                {"Web Development", "programming,web,frontend"},
                {"Java Masterclass", "programming,java,backend"},
                {"Data Science Basics", "data,science,programming"},
                {"Machine Learning", "ai,programming,science"},
                {"Advanced Mathematics", "mathematics,science,education"},
                {"Business English", "language,business,english"},
                {"Spanish for Beginners", "language,spanish,beginners"},
                {"Digital Marketing", "business,marketing,social"},
                {"Mobile App Development", "programming,mobile,development"}
        };

        // Dla każdego teachera (users[1] do users[5])
        for (int i = 1; i <= 5; i++) {
            UserEntity teacher = users.get(i);
            int courseCount = 4 + random.nextInt(3); // 4-6 kursów

            for (int j = 0; j < courseCount; j++) {
                // Wybierz losowy template
                String[] template = courseTemplates[random.nextInt(courseTemplates.length)];
                String courseName = template[0] + " " + (j + 1);
                List<String> tags = Arrays.asList(template[1].split(","));

                createCourse(
                        courses,
                        teacher,
                        courseName,
                        1 + random.nextInt(9), // losowy banner 1-9
                        tags
                );
            }
        }

        return courses;
    }

    private CourseEntity createCourse(List<CourseEntity> courses, UserEntity teacher,
                                      String name, int bannerIndex, List<String> tags) {
        CourseEntity course = new CourseEntity();
        course.setName(name);
        course.setPrice(BigDecimal.valueOf((int)(Math.random() * 50)*50));
        course.setDuration(BigDecimal.valueOf(10 + Math.random() * 40));
        course.setUser(teacher);
        course.setTags(tags.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList()));
        course.setReview(BigDecimal.ZERO);
        course.setReviewNumber(0);
        course.setDescription("Comprehensive course covering all aspects of " + name);
        course.setBanner(loadBannerImage(bannerIndex));
        course.setMimeType("image/png");
        course.setCreatedAt(LocalDateTime.now().minusMonths(1));
        course.setUpdatedAt(LocalDateTime.now());
        CourseEntity savedCourse = courseRepository.save(course);
        courses.add(savedCourse);
        return savedCourse;
    }

    private void createChaptersAndSubchapters(List<CourseEntity> courses) {
        for (CourseEntity course : courses) {
            for (int i = 1; i <= 5; i++) { // 5 rozdziałów na kurs
                ChapterEntity chapter = new ChapterEntity();
                chapter.setCourse(course);
                chapter.setName("Chapter " + i);
                chapter.setOrder(i);
                chapter.setReview(BigDecimal.valueOf(3.5 + Math.random() * 1.5));
                chapter.setReviewNumber((int)(5 + Math.random() * 45));
                chapter = chapterRepository.save(chapter);

                for (int j = 1; j <= 3; j++) { // 3 podrozdziały na rozdział
                    SubchapterEntity subchapter = new SubchapterEntity();
                    subchapter.setChapter(chapter);
                    subchapter.setName("Subchapter " + j);
                    subchapter.setOrder(j);
                    subchapter = subchapterRepository.save(subchapter);

                    createContentItems(subchapter);
                }
            }
        }
    }

    private void createContentItems(SubchapterEntity subchapter) {
        // Text content
        ContentItemEntity textContent = new ContentItemEntity();
        textContent.setSubchapter(subchapter);
        textContent.setType("text");
        textContent.setOrder(1);
        textContent.setText("Sample content text for learning");
        textContent.setFontSize("medium");
        textContent.setBolder(true);
        textContent.setTextColor("black");
        contentItemRepository.save(textContent);

        // Quiz content
        ContentItemEntity quizContent = new ContentItemEntity();
        quizContent.setSubchapter(subchapter);
        quizContent.setType("quiz");
        quizContent.setOrder(2);
        quizContent.setQuizContent("""
        {
            "questions": [
                {
                    "id": 1,
                    "question": "Pytanie testowe?",
                    "order": 1,
                    "singleAnswer": true,
                    "answers": [
                        {
                            "id": 1,
                            "order": 1,
                            "answer": "Odpowiedź A",
                            "isCorrect": true
                        },
                        {
                            "id": 2,
                            "order": 2,
                            "answer": "Odpowiedź B",
                            "isCorrect": false
                        }
                    ]
                }
            ]
        }
    """);






        contentItemRepository.save(quizContent);
    }

    private void createPurchasedCourses(List<UserEntity> users, List<CourseEntity> courses) {
        Random random = new Random();
        // Dla każdego verified usera (indeksy 6-56)
        for (int i = 6; i <= 55; i++) {
            UserEntity user = users.get(i);
            // Każdy user kupuje 6-8 kursów
            int coursesToBuy = 6 + random.nextInt(3);
            List<CourseEntity> shuffledCourses = new ArrayList<>(courses);
            Collections.shuffle(shuffledCourses);

            for (int j = 0; j < coursesToBuy && j < shuffledCourses.size(); j++) {
                CourseEntity course = shuffledCourses.get(j);
                if (user.getPoints() >= course.getPrice().intValue()) {
                    PurchasedCourseEntity purchase = new PurchasedCourseEntity();
                    purchase.setUser(user);
                    purchase.setCourse(course);
                    purchase.setPurchaseDate(Instant.now().minusSeconds(random.nextInt(2592000)));
                    purchase.setPointsSpent(course.getPrice().intValue());
                    purchasedCourseRepository.save(purchase);

                    user.setPoints(user.getPoints() - course.getPrice().intValue());
                    userRepository.save(user);
                }
            }
        }
    }

    private byte[] loadProfileImage(int index) {
        try {
            String imagePath = String.format("/demo-data/profile-pictures/profile%d.png", index);
            ClassPathResource resource = new ClassPathResource(imagePath);
            return StreamUtils.copyToByteArray(resource.getInputStream());
        } catch (IOException e) {
            logger.warn("Failed to load profile image: " + index, e);
            return new byte[0];
        }
    }

    private byte[] loadBannerImage(int index) {
        try {
            String imagePath = String.format("/demo-data/courses-banners/banner%d.png", index);
            ClassPathResource resource = new ClassPathResource(imagePath);
            return StreamUtils.copyToByteArray(resource.getInputStream());
        } catch (IOException e) {
            logger.warn("Failed to load banner image: " + index, e);
            return new byte[0];
        }
    }

    private void createCourseReviews(List<UserEntity> users, List<CourseEntity> courses) {
        Random random = new Random();
        List<UserEntity> verifiedUsers = users.subList(6, 56); // Wszyscy verified users (indeksy 6-55)

        String[] reviewTemplates = {
                "Excellent course with detailed explanations. Great practical examples.",
                "Very informative content. The instructor explains concepts clearly.",
                "Good course structure. Helped me understand the topic better.",
                "Solid content and well-presented material. Would recommend.",
                "Nice course with practical assignments. Looking forward to more courses.",
                "Clear explanations and good examples. Worth the investment.",
                "The course met my expectations. Good learning experience.",
                "Comprehensive coverage of the topic. Engaging presentation."
        };

        for (CourseEntity course : courses) {
            CourseEntity managedCourse = courseRepository.findById(course.getId())
                    .orElseThrow(() -> new RuntimeException("Course not found"));

            List<UserEntity> potentialReviewers = verifiedUsers.stream()
                    .filter(user -> purchasedCourseRepository.existsByUserIdAndCourseId(user.getId(), managedCourse.getId()))
                    .collect(Collectors.toList());

            if (potentialReviewers.isEmpty()) {
                continue;
            }

            int reviewCount = (int)(potentialReviewers.size() * (0.8 + random.nextDouble() * 0.2));
            Collections.shuffle(potentialReviewers);
            List<UserEntity> selectedReviewers = potentialReviewers.subList(0, reviewCount);

            BigDecimal totalRating = BigDecimal.ZERO;
            List<ReviewEntity> courseReviews = new ArrayList<>();

            for (UserEntity reviewer : selectedReviewers) {
                // Więcej pozytywnych ocen (3-5 gwiazdek, z większą szansą na 4-5)
                int rating = 3 + (random.nextDouble() < 0.8 ? 1 + random.nextInt(2) : 0);
                totalRating = totalRating.add(BigDecimal.valueOf(rating));

                ReviewEntity review = ReviewEntity.builder()
                        .user(reviewer)
                        .targetId(managedCourse.getId())
                        .targetType(ReviewTargetType.COURSE)
                        .rating(rating)
                        .content(reviewTemplates[random.nextInt(reviewTemplates.length)] +
                                (rating == 5 ? " Highly recommended!" : ""))
                        .createdAt(LocalDateTime.now().minusDays(random.nextInt(30)))
                        .build();

                courseReviews.add(reviewRepository.save(review));
            }

            if (!courseReviews.isEmpty()) {
                managedCourse.setReviewNumber(courseReviews.size());
                managedCourse.setReview(totalRating.divide(BigDecimal.valueOf(courseReviews.size()), 2, RoundingMode.HALF_UP));
                courseRepository.save(managedCourse);
            }
        }
    }

    private void createTeacherReviews(List<UserEntity> users) {
        Random random = new Random();
        List<UserEntity> verifiedUsers = users.subList(6, 56);
        List<UserEntity> teachers = users.subList(1, 6);

        String[] reviewTemplates = {
                "Świetny nauczyciel, bardzo dobrze tłumaczy.",
                "Profesjonalne podejście do nauczania.",
                "Polecam tego nauczyciela, świetny kontakt.",
                "Dobra metodyka nauczania, jasne tłumaczenia.",
                "Bardzo pomocny i cierpliwy nauczyciel.",
                "Wysoki poziom wiedzy i umiejętność jej przekazania.",
                "Dobrze przygotowany do zajęć, polecam.",
                "Świetne materiały i sposób nauczania."
        };

        for (UserEntity teacher : teachers) {
            TeacherProfileEntity teacherProfile = teacherProfileRepository.findByUserId(teacher.getId())
                    .orElseThrow(() -> new RuntimeException("Teacher profile not found"));

            // Pobierz użytkowników, którzy kupili kursy tego nauczyciela
            List<UserEntity> eligibleReviewers = verifiedUsers.stream()
                    .filter(user -> purchasedCourseRepository.existsByUserIdAndCourseUserId(user.getId(), teacher.getId()))
                    .collect(Collectors.toList());

            if (eligibleReviewers.isEmpty()) {
                continue;
            }

            // 80-100% uprawnionych użytkowników dodaje recenzję
            int reviewCount = (int)(eligibleReviewers.size() * (0.8 + random.nextDouble() * 0.2));
            Collections.shuffle(eligibleReviewers);
            List<UserEntity> selectedReviewers = eligibleReviewers.subList(0, Math.min(reviewCount, eligibleReviewers.size()));

            BigDecimal totalRating = BigDecimal.ZERO;
            List<ReviewEntity> teacherReviews = new ArrayList<>();

            for (UserEntity reviewer : selectedReviewers) {
                // Więcej pozytywnych ocen (3-5 gwiazdek)
                int rating = 3 + (random.nextDouble() < 0.8 ? 1 + random.nextInt(2) : 0);
                totalRating = totalRating.add(BigDecimal.valueOf(rating));

                ReviewEntity review = ReviewEntity.builder()
                        .user(reviewer)
                        .targetId(teacher.getId())
                        .targetType(ReviewTargetType.TEACHER)
                        .rating(rating)
                        .content(reviewTemplates[random.nextInt(reviewTemplates.length)] +
                                (rating == 5 ? " Zdecydowanie polecam!" : ""))
                        .createdAt(LocalDateTime.now().minusDays(random.nextInt(30)))
                        .build();

                teacherReviews.add(reviewRepository.save(review));
            }

            if (!teacherReviews.isEmpty()) {
                teacherProfile.setReviewNumber(teacherReviews.size());
                teacherProfile.setReview(totalRating.divide(
                        BigDecimal.valueOf(teacherReviews.size()),
                        2,
                        RoundingMode.HALF_UP
                ));
                teacherProfileRepository.save(teacherProfile);
            }
        }
    }
}
