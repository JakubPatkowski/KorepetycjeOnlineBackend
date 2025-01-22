package com.example.ekorki.service.entity;

import com.example.ekorki.dto.user.UserResponseDTO;
import com.example.ekorki.dto.user.UserLoginDTO;
import com.example.ekorki.dto.user.UserRegisterDTO;
import com.example.ekorki.entity.VerificationTokenEntity;
import com.example.ekorki.exception.ApiException;
import com.example.ekorki.entity.UserEntity;
import com.example.ekorki.entity.UserProfileEntity;
import com.example.ekorki.entity.RoleEntity;
import com.example.ekorki.repository.UserProfileRepository;
import com.example.ekorki.repository.UserRepository;
import com.example.ekorki.service.EmailService;
import com.example.ekorki.service.EmailVerificationService;
import com.example.ekorki.service.JWTService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Validated
@RequiredArgsConstructor
 public class UserService{
    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final JWTService jwtService;

    @Autowired
    private final UserProfileService userProfileService;

    @Autowired
    private final UserProfileRepository userProfileRepository;

    @Autowired
    private final RefreshTokenService refreshTokenService;

    @Autowired
    private final VerificationTokenService verificationTokenService;

    @Autowired
    private final EmailService emailServicfuil7e;

    @Autowired
    private final TeacherProfileService teacherProfileService;

    @Autowired
    private final RoleService roleService;

    @Autowired
    private final LoginAttemptService loginAttemptService;

    @Autowired
    private final EmailVerificationService emailVerificationService;

    private final BCryptPasswordEncoder encoder;

    private final AuthenticationManager authenticationManager;



    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Transactional
    public UserResponseDTO login(UserLoginDTO userLoginDTO, String clientIp) {
        logger.debug("Starting login process for email: {}", userLoginDTO.email());

        try {

            if (loginAttemptService.isAccountBlocked(userLoginDTO.email(), clientIp)) {
                loginAttemptService.recordLoginAttempt(userLoginDTO.email(), clientIp, false);
                throw new ApiException("Account is temporarily blocked due to too many failed attempts");
            }

            Authentication authentication;

            try {
                authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(userLoginDTO.email(), userLoginDTO.password())
                );
            } catch (AuthenticationException e) {
                // Record failed attempt in separate transaction
                loginAttemptService.recordLoginAttempt(userLoginDTO.email(), clientIp, false);
                throw new ApiException("Authentication failed: " + e.getMessage(), e);
            }

            UserEntity userEntity = userRepository.findByEmail(userLoginDTO.email().trim().toLowerCase());
            String accessToken = jwtService.generateAccessToken(userLoginDTO.email());
            String refreshToken = refreshTokenService.generateRefreshToken(userEntity.getId(), clientIp);

            loginAttemptService.recordLoginAttempt(userLoginDTO.email(), clientIp, true);

            return createUserResponse(userEntity, accessToken, refreshToken);
        } catch (Exception e) {
            logger.error("Error during login process", e);
            throw e;
        }
    }

    @Transactional
    public boolean registerUser(UserRegisterDTO userRegisterDTO) {
        if (userRepository.existsByEmail(userRegisterDTO.email()))
            throw new ApiException("Email already in use.");
        try {
            UserEntity userEntity = new UserEntity();
            userEntity.setEmail(userRegisterDTO.email());
            userEntity.setPassword(encoder.encode(userRegisterDTO.password()));
            userEntity.setPoints(0);
            userEntity.setBlocked(false);
            userRepository.save(userEntity);

            roleService.addRoleToUser(userEntity, RoleEntity.Role.USER);

            userProfileService.addUserProfileToUser(userRegisterDTO.fullName(), userEntity.getId());

            emailVerificationService.sendInitialVerificationEmail(userEntity);

            return true;
        } catch (Exception exception) {
            System.err.println(exception.getMessage());
            throw new ApiException("Error occurred", exception);
        }
    }

    @Transactional
    public UserResponseDTO getUserEntity(Long userId){
        Optional<UserEntity> optionalUser = userRepository.findById(userId);
        if(optionalUser.isPresent()){
            UserEntity userEntity = optionalUser.get();
            UserResponseDTO userDTO = new UserResponseDTO();

            userDTO.setId(userEntity.getId());
            UserProfileEntity userProfileEntity = userProfileRepository.findByUserId(userEntity.getId())
                    .orElseThrow(() -> new EntityNotFoundException("User Profile not found"));


            userDTO.setBadgesVisible(userProfileEntity.getBadgesVisible());

            userDTO.setFullName(userProfileEntity.getFullName());
            userDTO.setEmail(userEntity.getEmail());
            userDTO.setPoints(userEntity.getPoints());
            userDTO.setCreatedAt(userProfileEntity.getCreatedAt());
            Set<String> roles = userEntity.getRoles().stream()
                    .map(roleEntity -> roleEntity.getRole().toString())
                    .collect(Collectors.toSet());
            userDTO.setRoles(roles);
            userDTO.setPicture(createPictureData(userProfileEntity.getPicture(), userProfileEntity.getPictureMimeType()));
            userDTO.setDescription(userProfileEntity.getDescription());
            return userDTO;
        } else {
            throw new EntityNotFoundException("User not found");
        }
    }

    @Transactional
    public boolean upgradeToTeacher(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));

        // Sprawdź czy użytkownik już nie ma roli Teacher
        if(roleService.getUserRoles(userId).contains(RoleEntity.Role.TEACHER)){
            throw new ApiException("User already has Teacher role");
        }

        // Sprawdź czy użytkownik ma wystarczająco punktów
        if (user.getPoints() < 1000) {
            throw new ApiException("Insufficient points. Required: 1000");
        }

        // Odejmij punkty i dodaj rolę
        try {
            user.setPoints(user.getPoints() - 1000);
            roleService.addRoleToUser(user, RoleEntity.Role.TEACHER);
            userRepository.save(user);

            teacherProfileService.createTeacherProfile(userId);

            return true;
        } catch (Exception e) {
            throw new ApiException("Error while upgrading to teacher: " + e.getMessage());
        }
    }

    private UserResponseDTO createUserResponse(UserEntity userEntity, String accessToken, String refreshToken) {
        UserProfileEntity userProfileEntity = userProfileRepository.findByUserId(userEntity.getId())
                .orElseThrow(() -> new EntityNotFoundException("User Profile not found"));

        UserResponseDTO userDTO = new UserResponseDTO();
        userDTO.setAccessToken(accessToken);
        userDTO.setRefreshToken(refreshToken);
        userDTO.setId(userEntity.getId());
        userDTO.setFullName(userProfileEntity.getFullName());
        userDTO.setEmail(userEntity.getEmail());
        userDTO.setPoints(userEntity.getPoints());
        userDTO.setCreatedAt(userProfileEntity.getCreatedAt());
        userDTO.setRoles(userEntity.getRoles().stream()
                .map(role -> role.getRole().toString())
                .collect(Collectors.toSet()));
        userDTO.setPicture(createPictureData(userProfileEntity.getPicture(), userProfileEntity.getPictureMimeType()));
        userDTO.setDescription(userProfileEntity.getDescription());
        userDTO.setBadgesVisible(userProfileEntity.getBadgesVisible());

        return userDTO;
    }

    private Map<String, Object> createPictureData(byte[] picture, String mimeType) {
        Map<String, Object> pictureData = new HashMap<>();
        pictureData.put("data", picture);
        pictureData.put("mimeType", mimeType);
        return pictureData;
    }



}
