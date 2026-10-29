package com.renode.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "resource_listings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "listing_type", nullable = false)
    private String listingType; // "WASTE" ya "REQUIREMENT"

    @Column(name = "category")
    private String category; // e.g., "Wood Ash", "Cotton Scrap"

    @Column(name = "quantity_in_tons")
    private Double quantityInTons;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "target_price")
    private Double targetPrice;

    @Column(name = "purity_specifications")
    private String puritySpecifications;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}