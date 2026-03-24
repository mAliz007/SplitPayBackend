package com.SplitPay.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public void sendVerificationEmail(String toEmail, String token) {
        // Construct the link: http://localhost:5000/verify?token=...
        String verificationLink = frontendUrl + "/verify?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("SPLIT.PAY | Verify Your Identity");
        message.setText("Welcome to the platform.\n\n" +
                "This security link expires in 10 minutes:\n" +
                verificationLink + "\n\n" +
                "If you did not request this, please ignore this email.");

        mailSender.send(message);
    }
}