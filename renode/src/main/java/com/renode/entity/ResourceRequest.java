package com.renode.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private Company sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private Company receiver;

    // Yahan tumhari exact file ka naam (ResourceListing) laga diya hai
    @ManyToOne
    @JoinColumn(name = "resource_id", nullable = false)
    private ResourceListing resource;

    @Column(name = "requested_quantity", nullable = false)
    private Double requestedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}