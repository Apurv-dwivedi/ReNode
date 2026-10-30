package com.renode.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_requests")
@Data
public class TradeRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private Company sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private Company receiver;

    @ManyToOne
    @JoinColumn(name = "resource_id")
    private ResourceListing resource;

    private Double requestedQuantity;

    private String status; // PENDING, ACCEPTED, REJECTED, COMPLETED

    private LocalDateTime createdAt = LocalDateTime.now();
}