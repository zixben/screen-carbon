package com.lks.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.internet.MimeMessage;


@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public void sendEmail(String to, String subject, String htmlContent) throws Exception {
        // Trim leading and trailing spaces
        to = to != null ? to.trim() : "";
        String from = "screencarbontest@glasgow.ac.uk"; // Ensure this is a valid email address and matches your SMTP account

        // Log the 'to' and 'from' addresses for debugging
        logger.info("Attempting to send email...");
        logger.info("From: {}", from);
        logger.info("To: {}", to);

        // Validate email addresses
        if (!isValidEmail(from)) {
            logger.error("Invalid sender email address: {}", from);
            throw new IllegalArgumentException("Invalid sender email address.");
        }

        if (!isValidEmail(to)) {
            logger.error("Invalid recipient email address: {}", to);
            throw new IllegalArgumentException("Invalid recipient email address.");
        }

        try {
            // Create a MimeMessage
            MimeMessage message = mailSender.createMimeMessage();

            // Use MimeMessageHelper to set email properties
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // Set the second parameter to 'true' for HTML content

            // Send the email
            mailSender.send(message);
            logger.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            logger.error("Error sending email: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Basic email validation method
    private boolean isValidEmail(String email) {
        // Simple regex for email validation
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        return email != null && email.matches(emailRegex);
    }
}
