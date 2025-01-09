package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendEmailVerificationLink(String toEmail, String verificationLink){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Verify your email address");
        message.setText("Please click on the link below to verify your email address:\n" + verificationLink);
        mailSender.send(message);
    }
    @Async
    public void sendEmailChangeCode(String toEmail, String code){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Your Email Change Verification Code");
        message.setText("Your verification code to change your email is: \n" + code + "\nThis code will expire in 15 minutes.");
        mailSender.send(message);
    }
    @Async
    public void sendPasswordChangeCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Your Password Change Verification Code");
        message.setText("Your verification code to change your password is: \n" + code + "\nThis code will expire in 15 minutes.");
        mailSender.send(message);
    }

    @Async
    public void sendPointsPurchaseConfirmation(String toEmail, int points, int price, int newBalance) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Points Purchase Confirmation");
        message.setText(String.format("""
            Thank you for your purchase!
            
            Transaction details:
            - Points purchased: %d
            - Price: %d PLN
            - Current balance: %d points
            
            Thank you for using our service!
            """, points, price, newBalance));

        mailSender.send(message);
    }

    @Async
    public void sendPointsWithdrawalConfirmation(String toEmail, int points, int price, int newBalance) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Points Withdrawal Confirmation");
        message.setText(String.format("""
            Points withdrawal confirmation
            
            Transaction details:
            - Points withdrawn: %d
            - Value: %d PLN
            - Remaining balance: %d points
            
            The funds will be transferred to your account within 24 hours.
            Thank you for using our service!
            """, points, price, newBalance));

        mailSender.send(message);
    }

    @Async
    public void sendCoursePurchaseConfirmation(String toEmail, String courseName, int pointsSpent, int newBalance) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Course Purchase Confirmation");
        message.setText(String.format("""
            Thank you for purchasing the course!
            
            Transaction details:
            - Course: %s
            - Points spent: %d
            - Remaining balance: %d points
            
            You can now access your course in your account.
            Happy learning!
            """, courseName, pointsSpent, newBalance));

        mailSender.send(message);
    }
}
