package com.example.demo.service;

import com.example.demo.dto.UserResponseDTO;
import com.example.demo.dto.UserLoginDTO;
import com.example.demo.dto.UserRegisterDTO;
import com.example.demo.entity.VerificationTokenEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.entity.UserEntity;
import com.example.demo.entity.UserProfileEntity;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;


@Service
@Validated
@RequiredArgsConstructor
 public class UserService{
    @Autowired
    private final UserRepository userRepository;

    @Autowired
    public final JWTService jwtService;

    @Autowired
    public final UserProfileService userProfileService;

    @Autowired
    public final UserProfileRepository userProfileRepository;

    @Autowired
    public final RefreshTokenService refreshTokenService;

    private final VerificationTokenService verificationTokenService;

    private final EmailService emailService;

    public final BCryptPasswordEncoder encoder;

    public final AuthenticationManager authenticationManager;

    public UserResponseDTO login(UserLoginDTO userLoginDTO, String clientIp) {
        try {

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userLoginDTO.email(), userLoginDTO.password())
            );

            if (authentication.isAuthenticated()) {
                UserEntity userEntity = userRepository.findByEmail(userLoginDTO.email().trim().toLowerCase());
                String accessToken = jwtService.generateAccessToken(userLoginDTO.email());
                String refreshToken = refreshTokenService.generateRefreshToken(userEntity.getId(), clientIp);

                UserResponseDTO userDTO = new UserResponseDTO();

                userDTO.setAccessToken(accessToken);
                userDTO.setRefreshToken(refreshToken);

                userDTO.setId(userEntity.getId());
                UserProfileEntity userProfileEntity = userProfileRepository.findByUserId(userEntity.getId());

                userDTO.setFullName(userProfileEntity.getFullName());
                userDTO.setEmail(userEntity.getEmail());
                userDTO.setPoints(userEntity.getPoints());
                userDTO.setRole(userEntity.getRole().toString());
                userDTO.setVerified(userEntity.isVerified());
                userDTO.setBlocked(userEntity.isBlocked());
                userDTO.setMfa(userEntity.isMfa());
                userDTO.setPicture(userProfileEntity.getPicture());
                return userDTO;

            }
            else {


                throw new ApiException("Wrong password");
            }
        } catch (AuthenticationException e) {
            System.err.println(e.getMessage());
            throw new ApiException(STR."Authentication failed: \{e.getMessage()}", e);
        }
    }


    public boolean registerUser(UserRegisterDTO userRegisterDTO) {
        if (userRepository.existsByEmail(userRegisterDTO.email()))
            throw new ApiException("Email already in use.");
        try {
            UserEntity userEntity = new UserEntity();
            userEntity.setEmail(userRegisterDTO.email());
            userEntity.setPassword(encoder.encode(userRegisterDTO.password()));
            userEntity.setPoints(0);
            userEntity.setRole(UserEntity.Role.USER);
            userEntity.setVerified(false);
            userEntity.setBlocked(false);
            userEntity.setMfa(false);

            userRepository.save(userEntity);
            userProfileService.addUserProfileToUser(userRegisterDTO.fullName(), userEntity.getId());

            String token = verificationTokenService.generateToken(userEntity, VerificationTokenEntity.TokenType.EMAIL_VERIFICATION);
            String verificationLink = "http://localhost:8080/user/verify-email?token="+token;
            emailService.sendVerificationEmail(userEntity.getEmail(), verificationLink);

            return true;
        } catch (Exception exception) {
            System.err.println(exception.getMessage());
            throw new ApiException("Error occurred", exception);
        }
    }

    public boolean changeEmail(Long userId, String newEmail){
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));
        if (userRepository.existsByEmail(newEmail)) {
            throw new ApiException("Email already in use");
        }
        userEntity.setEmail(newEmail);
        userRepository.save(userEntity);
        return true;
    }
}
