package com.mvc.nms_be.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UnlockEmailService {
    private final ObjectProvider<JavaMailSender> sender;
    private final String from;

    public UnlockEmailService(ObjectProvider<JavaMailSender> sender,
            @Value("${app.mail.from:${spring.mail.username:}}") String from) {
        this.sender = sender;
        this.from = from;
    }

    public void sendOtp(String email, String otp) {
        JavaMailSender mailSender = sender.getIfAvailable();
        if (mailSender == null || from.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email service is not configured. Please contact an administrator.");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("NMS - Account unlock verification code");
        message.setText("Your account unlock code is: " + otp
                + "\nThis code expires in 5 minutes and can only be used once."
                + "\nAfter verification, set a new password to unlock your account."
                + "\nIf you did not request this, ignore this email.");
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to send the verification email. Please try again later.");
        }
    }
}
