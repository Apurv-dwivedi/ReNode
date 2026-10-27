package com.renode.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "resource_matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "provider_company_id", nullable = false)
    private Company provider;

    @ManyToOne
    @JoinColumn(name = "consumer_company_id", nullable = false)
    private Company consumer;

    @ManyToOne
    @JoinColumn(name = "resource_id", nullable = false)
    private ResourceListing resource;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "match_status")
    private String matchStatus; // "PENDING", "ACCEPTED"
}