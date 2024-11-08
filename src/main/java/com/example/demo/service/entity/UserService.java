package com.example.demo.service.entity;

import com.example.demo.dto.user.UserResponseDTO;
import com.example.demo.dto.user.UserLoginDTO;
import com.example.demo.dto.user.UserRegisterDTO;
import com.example.demo.entity.VerificationTokenEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.entity.UserEntity;
import com.example.demo.entity.UserProfileEntity;
import com.example.demo.repository.UserProfileRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.EmailService;
import com.example.demo.service.JWTService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
                UserProfileEntity userProfileEntity = userProfileRepository.findByUserId(userEntity.getId())
                        .orElseThrow(() -> new EntityNotFoundException("User Profile not found"));

                userDTO.setBadgesVisible(false);

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

    @Transactional
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

    @Transactional
    public boolean initiateEmailChange(Long loggedInUserId){
        UserEntity user = userRepository.findById(loggedInUserId)
                .orElseThrow(() -> new ApiException("User not found"));
        String code = verificationTokenService.generateEmailChangeCode(user);
        emailService.sendEmailChangeCode(user.getEmail(), code);
        return true;
    }
    @Transactional
    public boolean completeEmailChange(String code, String newEmail, Long loggedInUserId){
        Optional<UserEntity> optionalUser = verificationTokenService.getUserByToken(code, VerificationTokenEntity.TokenType.EMAIL_CHANGE);
        if (optionalUser.isPresent()){
            UserEntity userEntity = optionalUser.get();
            if(!userEntity.getId().equals(loggedInUserId)){
                throw new AccessDeniedException("You do not have permission to change this email");

            }
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

    @Transactional
    public boolean initiatePasswordChange(Long loggedInUserId) {
        UserEntity user = userRepository.findById(loggedInUserId)
                .orElseThrow(() -> new ApiException("User not found"));
        String code = verificationTokenService.generatePasswordChangeCode(user);
        emailService.sendPasswordChangeCode(user.getEmail(), code);
        return true;
    }
    @Transactional
    public boolean completePasswordChange(String code, String newPassword, Long loggedInUserId) {
        Optional<UserEntity> optionalUser = verificationTokenService.getUserByToken(code, VerificationTokenEntity.TokenType.PASSWORD_CHANGE);
        if (optionalUser.isPresent()) {
            UserEntity userEntity = optionalUser.get();
            if (!userEntity.getId().equals(loggedInUserId)) {
                throw new AccessDeniedException("You do not have permission to change this password");
            }
            userEntity.setPassword(encoder.encode(newPassword));
            userRepository.save(userEntity);
            verificationTokenService.deleteToken(code);
            return true;
        }
        return false;
    }

    public UserResponseDTO getUserEntity(Long userId){
        Optional<UserEntity> optionalUser = userRepository.findById(userId);
        if(optionalUser.isPresent()){
            UserEntity userEntity = optionalUser.get();
            UserResponseDTO userDTO = new UserResponseDTO();

            userDTO.setId(userEntity.getId());
            UserProfileEntity userProfileEntity = userProfileRepository.findByUserId(userEntity.getId())
                    .orElseThrow(() -> new EntityNotFoundException("User Profile not found"));


            //TODO dodać badge
            userDTO.setBadgesVisible(false);

            userDTO.setFullName(userProfileEntity.getFullName());
            userDTO.setEmail(userEntity.getEmail());
            userDTO.setPoints(userEntity.getPoints());
            userDTO.setRole(userEntity.getRole().toString());
            userDTO.setPicture(userProfileEntity.getPicture());
            userDTO.setDescription(userProfileEntity.getDescription());
            return userDTO;
        } else {
            throw new EntityNotFoundException("User not found");
        }
    }

//    public boolean buyPoints(int value, Long loggedInUserId){
//
//    }



}
