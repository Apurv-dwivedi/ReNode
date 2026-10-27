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
    private String industryType; // e.g., "Cement", "Textile"

    @Column(name = "contact_email", unique = true, nullable = false)
    private String contactEmail;

    // Purani companies ke liye null chalega, naye registration me password hoga
    @Column(nullable = true)
    private String password;

    // MySQL me spatial data store karne ke liye
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