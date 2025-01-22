package com.example.ekorki.service;

import com.example.ekorki.entity.RoleEntity;
import com.example.ekorki.entity.UserEntity;
import com.example.ekorki.entity.VerificationTokenEntity;
import com.example.ekorki.exception.ApiException;
import com.example.ekorki.repository.UserRepository;
import com.example.ekorki.service.entity.RoleService;
import com.example.ekorki.service.entity.VerificationTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

	private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);

	@Autowired
	private final UserRepository userRepository;

	@Autowired
	private final VerificationTokenService verificationTokenService;

	@Autowired
	private final EmailService emailService;

	@Autowired
	private final RoleService roleService;

	private final BCryptPasswordEncoder encoder;


	@Transactional
	public boolean initiateEmailChange(Long loggedInUserId) {
		UserEntity user = userRepository.findById(loggedInUserId)
				.orElseThrow(() -> new ApiException("User not found"));
		String code = verificationTokenService.generateEmailChangeCode(user);
		emailService.sendEmailChangeCode(user.getEmail(), code);
		return true;
	}

	@Transactional
	public boolean completeEmailChange(String code, String newEmail, Long loggedInUserId) {
		Optional<UserEntity> optionalUser = verificationTokenService.getUserByToken(code, VerificationTokenEntity.TokenType.EMAIL_CHANGE);
		if (optionalUser.isPresent()) {
			UserEntity userEntity = optionalUser.get();
			if (!userEntity.getId().equals(loggedInUserId)) {
				throw new AccessDeniedException("You do not have permission to change this email");
			}
			if (userRepository.existsByEmail(newEmail)) {
				throw new ApiException("Email already in use");
			}
			roleService.removeRoleFromUser(userEntity, RoleEntity.Role.VERIFIED);
			userEntity.getRoles().removeIf(role -> role.getRole() == RoleEntity.Role.VERIFIED);

			userEntity.setEmail(newEmail);
			userRepository.save(userEntity);
			verificationTokenService.deleteToken(code);

			String verificationToken = verificationTokenService.generateEmailVerificationToken(userEntity);
			String verificationLink = "http://localhost:8080/user/verify-email?token=" + verificationToken;
			emailService.sendEmailVerificationLink(userEntity.getEmail(), verificationLink);

			return true;
		}
		return false;
	}

	@Transactional
	public boolean resendVerificationEmail(Long userId) {
		UserEntity user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException("User not found"));

		if (roleService.getUserRoles(userId).contains(RoleEntity.Role.VERIFIED)) {
			throw new ApiException("User is already verified");
		}

		String verificationToken = verificationTokenService.generateEmailVerificationToken(user);
		String verificationLink = "http://localhost:8080/user/verify-email?token=" + verificationToken;
		emailService.sendEmailVerificationLink(user.getEmail(), verificationLink);

		return true;
	}

	@Transactional
	public boolean verifyEmail(String token) {
		Optional<UserEntity> optionalUser = verificationTokenService.getUserByToken(token, VerificationTokenEntity.TokenType.EMAIL_VERIFICATION);

		if (optionalUser.isEmpty()) {
			throw new ApiException("Invalid or expired verification token");
		}

		UserEntity user = optionalUser.get();
		roleService.addRoleToUser(user, RoleEntity.Role.VERIFIED);

		userRepository.save(user);
		verificationTokenService.deleteToken(token);
		return true;
	}

	public void sendInitialVerificationEmail(UserEntity user) {
		String verificationToken = verificationTokenService.generateEmailVerificationToken(user);
		String verificationLink = "http://localhost:8080/user/verify-email?token=" + verificationToken;
		emailService.sendEmailVerificationLink(user.getEmail(), verificationLink);
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
		Optional<UserEntity> optionalUser = verificationTokenService.getUserByToken(
				code,
				VerificationTokenEntity.TokenType.PASSWORD_CHANGE
		);

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
}