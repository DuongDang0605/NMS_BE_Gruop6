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
    private final String otpSubject;
    private final String otpBody;

    public UnlockEmailService(ObjectProvider<JavaMailSender> sender,
            @Value("${app.mail.from:${spring.mail.username:}}") String from,
            @Value("${app.mail.otp-subject}") String otpSubject,
            @Value("${app.mail.otp-body}") String otpBody) {
        this.sender = sender;
        this.from = from;
        this.otpSubject = otpSubject;
        this.otpBody = otpBody;
    }

    public void sendOtp(String email, String otp) {
        JavaMailSender mailSender = sender.getIfAvailable();
        if (mailSender == null || from.isBlank() || otpSubject.isBlank() || !otpBody.contains("{otp}")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email service is not configured. Please contact an administrator.");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(otpSubject);
        message.setText(otpBody.replace("{otp}", otp));
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to send the verification email. Please try again later.");
        }
    }
}
