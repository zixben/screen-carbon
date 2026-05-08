package com.lks.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.internet.MimeMessage;


@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String htmlContent) throws Exception {
        to = to != null ? to.trim() : "";
        String from = "screencarbontest@glasgow.ac.uk";

        logger.info("Attempting to send email...");
        logger.info("From: {}", from);
        logger.info("To: {}", to);

        if (!isValidEmail(from)) {
            logger.error("Invalid sender email address: {}", from);
            throw new IllegalArgumentException("Invalid sender email address.");
        }

        if (!isValidEmail(to)) {
            logger.error("Invalid recipient email address: {}", to);
            throw new IllegalArgumentException("Invalid recipient email address.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            logger.error("Error sending email: {}", e.getMessage(), e);
            throw e;
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        return email != null && email.matches(emailRegex);
    }
}
