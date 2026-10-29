package com.renode.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "verification_tokens")
@Data
public class VerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer companyId;
    private String token;
    private String tokenType;
    private LocalDateTime expiryDate;
    private Boolean isUsed = false;
}