package com.renode.service;

import com.renode.dto.MatchResponse;
import com.renode.entity.Company;
import com.renode.entity.ResourceListing;
import com.renode.repository.CompanyRepository;
import com.renode.repository.ResourceListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MatchmakingService {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ResourceListingRepository resourceListingRepository;

    public List<MatchResponse> findMatchesForCompany(Integer companyId, double radiusKm) {
        Company sourceCompany = companyRepository.findById(companyId).orElse(null);
        List<MatchResponse> matches = new ArrayList<>();

        if (sourceCompany == null) return matches;

        // 1. Company ki apni listings nikaalo (Supply vs Demand)
        List<ResourceListing> myListings = resourceListingRepository.findByCompanyId(companyId);

        // 2. Baaki saari companies ke listings fetch karo
        List<ResourceListing> allListings = resourceListingRepository.findAll();

        for (ResourceListing mine : myListings) {
            for (ResourceListing other : allListings) {
                // Apni hi company ke listing ko match nahi karna
                if (other.getCompany().getId().equals(companyId)) continue;

                // Hard Filter: Listing type opposite hona chahiye (WASTE <-> REQUIREMENT)
                boolean isComplementary = !mine.getListingType().equalsIgnoreCase(other.getListingType());

                // Hard Filter: Category match honi chahiye (case-insensitive)
                boolean isCategoryMatch = mine.getCategory().trim().equalsIgnoreCase(other.getCategory().trim());

                if (isComplementary && isCategoryMatch) {
                    // Distance calculate karo (agar location available hai)
                    double distance = calculateApproxDistance(sourceCompany, other.getCompany());

                    if (distance <= radiusKm) {
                        MatchResponse match = new MatchResponse();
                        match.setMatchedCompany(other.getCompany());
                        match.setMatchedResourceId(other.getId());
                        match.setMatchedResourceType(other.getListingType());
                        match.setMatchedCategory(other.getCategory());
                        match.setMatchedQuantity(other.getQuantityInTons());
                        match.setDistanceKm(distance);

                        // Scoring logic (Quantity fit aur distance ke aadhar par)
                        int score = calculateMatchScore(mine.getQuantityInTons(), other.getQuantityInTons(), distance, radiusKm);
                        match.setMatchScore(score);

                        matches.add(match);
                    }
                }
            }
        }

        return matches;
    }

    private double calculateApproxDistance(Company c1, Company c2) {
        if (c1.getLocation() == null || c2.getLocation() == null) {
            return 10.0; // Default fallback distance agar coordinates missing ho
        }
        // JTS Point se coordinates nikal kar Haversine formula ya simple euclidean distance
        double lat1 = c1.getLocation().getY();
        double lon1 = c1.getLocation().getX();
        double lat2 = c2.getLocation().getY();
        double lon2 = c2.getLocation().getX();

        // Simple approximate distance calculation in KM
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c; // Earth radius in KM
    }

    private int calculateMatchScore(double myQty, double theirQty, double distance, double maxRadius) {
        double qtyDiffRatio = Math.abs(myQty - theirQty) / Math.max(myQty, theirQty);
        int qtyScore = (int)((1.0 - qtyDiffRatio) * 50); // Max 50 points for quantity fit

        int distScore = (int)((1.0 - (distance / maxRadius)) * 50); // Max 50 points for distance proximity

        int totalScore = Math.max(20, Math.min(100, qtyScore + distScore));
        return totalScore;
    }
}