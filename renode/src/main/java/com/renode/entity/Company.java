package com.renode.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point; // Spatial data ke liye

@Entity
@Table(name = "companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "industry_type")
    private String industryType;

    @Column(name = "contact_email", unique = true, nullable = false)
    private String contactEmail;

    @Column(nullable = true)
    private String password;

    // --- Settings Module Fields ---
    @Column(columnDefinition = "TEXT")
    private String description;

    private String website;

    private String taxId;

    // For Verification Tab
    private String verificationStatus;

    // For Marketplace Preferences Tab
    private Integer defaultSearchRadius;
    private String preferredUnit;
    // --- Integrations Fields ---
    private String integrationProvider; // e.g., "SAP", "Tally", "Custom ERP"
    private String apiKey;
    private String webhookUrl;
    // -----------------------------

    @JsonIgnore
    @Column(columnDefinition = "POINT SRID 4326")
    private Point location;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}