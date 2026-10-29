package com.renode.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_outbox")
@Data
public class NotificationOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String recipientEmail;
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String htmlBody;

    @Column(columnDefinition = "TEXT")
    private String plainTextBody;

    private String status = "QUEUED"; // QUEUED, SENT, FAILED
    private int retryCount = 0;
    private int maxRetries = 3;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}