package com.renode.repository;

import com.renode.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {

    // 1. Map ke aas-paas ki factories dhoondhne ke liye
    @Query(value = "SELECT * FROM companies WHERE ST_Distance_Sphere(location, ST_GeomFromText(CONCAT('POINT(', :lat, ' ', :lon, ')'), 4326)) <= (:radius * 1000)", nativeQuery = true)
    List<Company> findNearbyFactories(@Param("lon") double lon, @Param("lat") double lat, @Param("radius") double radius);

    // 2. Email se company fetch karne ke liye (Login ke liye zaroori hai)
    Company findByContactEmail(String contactEmail);

    // 3. Password ke sath nayi factory register karne ke liye
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO companies (company_name, industry_type, contact_email, password, location) VALUES (:name, :industry, :email, :password, ST_GeomFromText(CONCAT('POINT(', :lat, ' ', :lon, ')'), 4326))", nativeQuery = true)
    void insertCompanyWithPassword(@Param("name") String name, @Param("industry") String industry, @Param("email") String email, @Param("password") String password, @Param("lat") double lat, @Param("lon") double lon);
}