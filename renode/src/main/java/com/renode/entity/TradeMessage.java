package com.renode.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_messages")
@Data
public class TradeMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "trade_request_id")
    private TradeRequest tradeRequest;

    @ManyToOne
    @JoinColumn(name = "sender_company_id")
    private Company senderCompany;

    @Column(columnDefinition = "TEXT")
    private String messageContent;

    private LocalDateTime sentAt = LocalDateTime.now();
}