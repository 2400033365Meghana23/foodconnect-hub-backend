package com.foodwaste.platform.service;

import com.foodwaste.platform.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String email, String otpCode, long expiryMinutes) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ApiException(500, "Mail config missing. Set MAIL_USERNAME and MAIL_PASSWORD in backend/.env");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(username);
        message.setTo(email);
        message.setSubject("Your Food Waste Platform OTP");
        message.setText("Your OTP is " + otpCode + ". It is valid for " + expiryMinutes + " minutes.");
        mailSender.send(message);
    }
}
