package com.example.demo.configuration;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

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
    private final BCryptPasswordEncoder passwordEncoder;
    private final TeacherProfileRepository teacherProfileRepository;

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

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
    }

    private byte[] loadImage(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return StreamUtils.copyToByteArray(resource.getInputStream());
        } catch (IOException e) {
            logger.warn("Failed to load image: " + path, e);
            // Zwróć przykładowe dane jeśli nie można wczytać obrazu
            return new byte[]{-119, 80, 78, 71, 13, 10, 26, 10}; // Minimalne nagłówek PNG
        }
    }

    private List<UserEntity> createUsers() {
        List<UserEntity> users = new ArrayList<>();
        String password = passwordEncoder.encode("Test123!@#");

        String[][] userData = {
                {"admin@example.com", "5000"},
                {"teacher1@example.com", "2500"},
                {"teacher2@example.com", "1800"},
                {"teacher3@example.com", "3200"},
                {"verified1@example.com", "500"},
                {"verified2@example.com", "750"},
                {"verified3@example.com", "300"},
                {"verified4@example.com", "1200"},
                {"verified5@example.com", "900"},
                {"user@example.com", "100"}
        };

        for (String[] data : userData) {
            UserEntity user = new UserEntity();
            user.setEmail(data[0]);
            user.setPassword(password);
            user.setPoints(Integer.parseInt(data[1]));
            user.setBlocked(false);
            users.add(userRepository.save(user));
        }

        return users;
    }

    private void createUserProfiles(List<UserEntity> users) {
        String[] names = {
                "John Admin", "Alice Teacher", "Bob Instructor", "Carol Educator",
                "David Student", "Eva Learner", "Frank Scholar", "Grace Reader",
                "Henry Student", "Isabel Newbie"
        };

        for (int i = 0; i < users.size(); i++) {
            UserProfileEntity profile = new UserProfileEntity();
            profile.setUserId(users.get(i).getId());
            profile.setFullName(names[i]);
            profile.setDescription(getDescriptionForUser(i));
            profile.setCreatedAt(new Date());
            profile.setPicture(loadProfileImage(i + 1)); // Używamy odpowiedniego obrazu profilowego
            profile.setPictureMimeType("image/png");
            profile.setBadgesVisible(true);
            userProfileRepository.save(profile);
        }
    }

    private String getDescriptionForUser(int index) {
        if (index == 0) return "Experienced administrator and course creator";
        if (index < 4) return "Professional educator with extensive teaching experience";
        return "Enthusiastic learner and knowledge seeker";
    }

    private void assignRoles(List<UserEntity> users) {
        // Admin
        addRoles(users.get(0), Set.of(RoleEntity.Role.ADMIN, RoleEntity.Role.TEACHER,
                RoleEntity.Role.VERIFIED, RoleEntity.Role.USER));

        // Teachers
        for (int i = 1; i < 4; i++) {
            addRoles(users.get(i), Set.of(RoleEntity.Role.TEACHER,
                    RoleEntity.Role.VERIFIED, RoleEntity.Role.USER));
            createTeacherProfile(users.get(i));

        }

        // Verified users
        for (int i = 4; i < 9; i++) {
            addRoles(users.get(i), Set.of(RoleEntity.Role.VERIFIED, RoleEntity.Role.USER));
        }

        // Regular user
        addRoles(users.get(9), Set.of(RoleEntity.Role.USER));
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
                .review(BigDecimal.valueOf(3.5 + Math.random() * 1.5))
                .reviewNumber((int)(5 + Math.random() * 45))
                .build();
        teacherProfileRepository.save(teacherProfile);
    }

    private List<CourseEntity> createCourses(List<UserEntity> users) {
        List<CourseEntity> courses = new ArrayList<>();

        // Kursy dla teacher1 (user index 1)
        createCourse(courses, users.get(1), "Advanced Mathematics", 1,
                Arrays.asList("mathematics", "science", "education"));
        createCourse(courses, users.get(1), "Physics for Beginners", 2,
                Arrays.asList("science", "physics", "beginners"));
        createCourse(courses, users.get(1), "Calculus Fundamentals", 3,
                Arrays.asList("mathematics", "calculus", "science"));

        // Kursy dla teacher2 (user index 2)
        createCourse(courses, users.get(2), "Web Development Bootcamp", 4,
                Arrays.asList("programming", "web", "frontend"));
        createCourse(courses, users.get(2), "Python Programming", 5,
                Arrays.asList("programming", "python", "backend"));
        createCourse(courses, users.get(2), "Java Masterclass", 6,
                Arrays.asList("programming", "java", "backend"));

        // Kursy dla teacher3 (user index 3)
        createCourse(courses, users.get(3), "English for Business", 7,
                Arrays.asList("language", "business", "english"));
        createCourse(courses, users.get(3), "Spanish Basics", 8,
                Arrays.asList("language", "spanish", "beginners"));
        createCourse(courses, users.get(3), "French Culture & Language", 9,
                Arrays.asList("language", "french", "culture"));

        return courses;
    }

    private CourseEntity createCourse(List<CourseEntity> courses, UserEntity teacher,
                                      String name, int bannerIndex, List<String> tags) {
        CourseEntity course = new CourseEntity();
        course.setName(name);
        course.setPrice(BigDecimal.valueOf((int)(Math.random() * 50)*50));
        course.setDuration(BigDecimal.valueOf(10 + Math.random() * 40));
        course.setUser(teacher);
        course.setTags(tags);
        course.setReview(BigDecimal.valueOf(3.5 + Math.random() * 1.5));
        course.setReviewNumber((int) (10 + Math.random() * 90));
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
                    "question": "Sample question?",
                    "answers": [
                        {"id": 1, "text": "Answer 1", "correct": true},
                        {"id": 2, "text": "Answer 2", "correct": false}
                    ]
                }
            ]
        }
    """);
        contentItemRepository.save(quizContent);
    }

    private void createPurchasedCourses(List<UserEntity> users, List<CourseEntity> courses) {
        Random random = new Random();
        // Dla każdego verified usera (indeksy 4-8)
        for (int i = 4; i < 9; i++) {
            UserEntity user = users.get(i);
            // Losowo wybierz 1-3 kursy do zakupu
            int coursesToBuy = 1 + random.nextInt(3);
            List<CourseEntity> shuffledCourses = new ArrayList<>(courses);
            Collections.shuffle(shuffledCourses);

            for (int j = 0; j < coursesToBuy && j < shuffledCourses.size(); j++) {
                CourseEntity course = shuffledCourses.get(j);
                PurchasedCourseEntity purchase = new PurchasedCourseEntity();
                purchase.setUser(user);
                purchase.setCourse(course);
                purchase.setPurchaseDate(Instant.now().minusSeconds(random.nextInt(2592000))); // w ciągu ostatniego miesiąca
                purchase.setPointsSpent(course.getPrice().intValue());
                purchasedCourseRepository.save(purchase);

                // Aktualizuj punkty użytkownika
                user.setPoints(user.getPoints() - course.getPrice().intValue());
                userRepository.save(user);
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
}
