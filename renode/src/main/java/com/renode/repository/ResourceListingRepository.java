package com.renode.repository;

import com.renode.entity.ResourceListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResourceListingRepository extends JpaRepository<ResourceListing, Integer> {
    // Spring Data JPA automatically yeh query bana dega!
    List<ResourceListing> findByCompanyId(Integer companyId);
}