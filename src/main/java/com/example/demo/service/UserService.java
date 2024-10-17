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
import org.springframework.security.core.parameters.P;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;


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
                userDTO.setPicture(userProfileEntity.getPicture());
                userDTO.setDescription(userProfileEntity.getDescription());
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
            userEntity.setBlocked(false);

            userRepository.save(userEntity);
            userProfileService.addUserProfileToUser(userRegisterDTO.fullName(), userEntity.getId());

            String verificationToken = verificationTokenService.generateEmailVerificationToken(userEntity);
            String verificationLink = "http://localhost:8080/user/verify-email?token="+verificationToken;
            emailService.sendEmailVerificationLink(userEntity.getEmail(), verificationLink);

            return true;
        } catch (Exception exception) {
            System.err.println(exception.getMessage());
            throw new ApiException("Error occurred", exception);
        }
    }

    public boolean verifyEmail(String token){
        Optional<UserEntity> optionalUser = verificationTokenService.getUserByToken(token, VerificationTokenEntity.TokenType.EMAIL_VERIFICATION);
        if(optionalUser.isPresent()){
            UserEntity user = optionalUser.get();
            user.setRole(UserEntity.Role.VERIFIED);
            userRepository.save(user);
            verificationTokenService.deleteToken(token);
            return true;
        }
        return false;
    }

    public boolean initiateEmailChange(Long userId){
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));
        String code = verificationTokenService.generateEmailChangeCode(user);
        emailService.sendEmailChangeCode(user.getEmail(), code);
        return true;
    }

    public boolean completeEmailChange(String code, String newEmail){
        Optional<UserEntity> optionalUser = verificationTokenService.getUserByToken(code, VerificationTokenEntity.TokenType.EMAIL_CHANGE);
        if (optionalUser.isPresent()){
            UserEntity userEntity = optionalUser.get();
            if (userRepository.existsByEmail(newEmail)){
                throw new ApiException("Email alredy in use");
            }
            userEntity.setEmail(newEmail);
            userEntity.setRole(UserEntity.Role.USER);
            userRepository.save(userEntity);
            verificationTokenService.deleteToken(code);

            String verificationToken = verificationTokenService.generateEmailVerificationToken(userEntity);
            String verificationLink = "http://localhost:8080/user/verify-email?token="+verificationToken;
            emailService.sendEmailVerificationLink(userEntity.getEmail(), verificationLink);

            return true;
        }
        return false;
    }


    public boolean initiatePasswordChange(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));
        String code = verificationTokenService.generatePasswordChangeCode(user);
        emailService.sendPasswordChangeCode(user.getEmail(), code);
        return true;
    }

    public boolean completePasswordChange(String code, String newPassword) {
        Optional<UserEntity> optionalUser = verificationTokenService.getUserByToken(code, VerificationTokenEntity.TokenType.PASSWORD_CHANGE);
        if (optionalUser.isPresent()) {
            UserEntity userEntity = optionalUser.get();
            userEntity.setPassword(encoder.encode(newPassword));
            userRepository.save(userEntity);
            verificationTokenService.deleteToken(code);
            return true;
        }
        return false;
    }

}
