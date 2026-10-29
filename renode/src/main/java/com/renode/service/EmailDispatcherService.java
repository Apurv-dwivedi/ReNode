package com.renode.service;

import com.renode.entity.NotificationOutbox;
import com.renode.repository.NotificationOutboxRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmailDispatcherService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @Value("${renode.mail.dev-mode:true}")
    private boolean devMode;

    @Value("${renode.mail.from:no-reply@renode.com}")
    private String fromEmail;

    @Transactional
    public void queueEmail(String to, String subject, String htmlBody, String plainText) {
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setRecipientEmail(to);
        outbox.setSubject(subject);
        outbox.setHtmlBody(htmlBody);
        outbox.setPlainTextBody(plainText);
        outbox.setStatus("QUEUED");
        outboxRepository.save(outbox);
    }

    @Scheduled(fixedDelay = 30000) // Poll outbox every 30 seconds
    @Transactional
    public void processOutbox() {
        List<NotificationOutbox> pendingEmails = outboxRepository.findByStatusAndRetryCountLessThan("QUEUED", 3);

        for (NotificationOutbox email : pendingEmails) {
            try {
                if (devMode) {
                    System.out.println("=== [DEV EMAIL MODE] ===");
                    System.out.println("To: " + email.getRecipientEmail());
                    System.out.println("Subject: " + email.getSubject());
                    System.out.println("Body: " + email.getHtmlBody());
                    System.out.println("========================");
                    email.setStatus("SENT");
                } else {
                    if (mailSender == null) throw new IllegalStateException("JavaMailSender not configured.");
                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                    helper.setFrom(fromEmail);
                    helper.setTo(email.getRecipientEmail());
                    helper.setSubject(email.getSubject());
                    helper.setText(email.getPlainTextBody(), email.getHtmlBody());
                    mailSender.send(message);
                    email.setStatus("SENT");
                }
            } catch (Exception e) {
                email.setRetryCount(email.getRetryCount() + 1);
                email.setErrorMessage(e.getMessage());
                if (email.getRetryCount() >= email.getMaxRetries()) {
                    email.setStatus("FAILED");
                }
            }
            outboxRepository.save(email);
        }
    }
}