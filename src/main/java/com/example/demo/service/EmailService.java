package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendEmailVerificationLink(String toEmail, String verificationLink){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Verify your email address");
        message.setText("Please click on the link below to verify your email address:\n" + verificationLink);
        mailSender.send(message);
    }

    public void sendEmailChangeCode(String toEmail, String code){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Your Email Change Verification Code");
        message.setText("Your verification code to change your email is: \n" + code + "\nThis code will expire in 15 minutes.");
        mailSender.send(message);
    }

    public void sendPasswordChangeCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Your Password Change Verification Code");
        message.setText("Your verification code to change your password is: \n" + code + "\nThis code will expire in 15 minutes.");
        mailSender.send(message);
    }
}
