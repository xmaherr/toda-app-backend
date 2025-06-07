package com.todoapp.user_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("mahernabil219@gmail.com");
        message.setTo(toEmail);
        message.setSubject("Your OTP for Todo App Registration");
        message.setText("Dear user,\n\nYour One-Time Password (OTP) for Todo App registration is: " + otpCode + "\n\nThis OTP is valid for 5 minutes.\n\nRegards,\nTodo App Team");
        mailSender.send(message);
    }
}