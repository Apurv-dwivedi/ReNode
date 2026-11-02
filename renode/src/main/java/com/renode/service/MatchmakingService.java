package com.renode.service;

import com.renode.dto.MatchResponse;
import com.renode.entity.Company;
import com.renode.entity.ResourceListing;
import com.renode.repository.CompanyRepository;
import com.renode.repository.ResourceListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MatchmakingService {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ResourceListingRepository resourceListingRepository;

    public List<MatchResponse> findMatchesForCompany(Integer companyId, double radiusKm) {
        Company sourceCompany = companyRepository.findById(companyId).orElse(null);
        List<MatchResponse> matches = new ArrayList<>();

        // 🔒 FIX: Safely handle null source company or missing location
        if (sourceCompany == null || sourceCompany.getLocation() == null) {
            return matches;
        }

        List<ResourceListing> myListings = resourceListingRepository.findByCompanyId(companyId);
        List<ResourceListing> allListings = resourceListingRepository.findAll();

        // 🔒 FIX: Prevent duplicate matches if a company matches on multiple listings
        Set<Integer> matchedListingIds = new HashSet<>();

        for (ResourceListing mine : myListings) {
            // 🔒 FIX: Prevent NullPointerException on empty listings
            if (mine.getListingType() == null || mine.getCategory() == null) continue;

            for (ResourceListing other : allListings) {
                if (other.getCompany() == null || other.getCompany().getId().equals(companyId)) continue;
                if (other.getListingType() == null || other.getCategory() == null) continue;
                if (matchedListingIds.contains(other.getId())) continue;

                boolean isComplementary = !mine.getListingType().equalsIgnoreCase(other.getListingType());
                boolean isCategoryMatch = mine.getCategory().trim().equalsIgnoreCase(other.getCategory().trim());

                if (isComplementary && isCategoryMatch) {
                    // 🔒 FIX: If target company has no location, do not invent a fake distance
                    if (other.getCompany().getLocation() == null) {
                        continue;
                    }

                    double distance = calculateHaversineDistance(sourceCompany, other.getCompany());

                    if (distance <= radiusKm) {
                        MatchResponse match = new MatchResponse();

                        // 🔒 SECURE: Redact sensitive company details before sending MatchResponse
                        Company safeTargetCompany = other.getCompany();
                        safeTargetCompany.setPassword(null);
                        safeTargetCompany.setApiKey(null);

                        match.setMatchedCompany(safeTargetCompany);
                        match.setMatchedResourceId(other.getId());
                        match.setMatchedResourceType(other.getListingType());
                        match.setMatchedCategory(other.getCategory());
                        match.setMatchedQuantity(other.getQuantityInTons());
                        match.setDistanceKm(distance);

                        int score = calculateMatchScore(mine.getQuantityInTons(), other.getQuantityInTons(), distance, radiusKm);
                        match.setMatchScore(score);

                        matches.add(match);
                        matchedListingIds.add(other.getId());
                    }
                }
            }
        }
        return matches;
    }

    private double calculateHaversineDistance(Company c1, Company c2) {
        double lat1 = c1.getLocation().getY();
        double lon1 = c1.getLocation().getX();
        double lat2 = c2.getLocation().getY();
        double lon2 = c2.getLocation().getX();

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c;
    }

    private int calculateMatchScore(double myQty, double theirQty, double distance, double maxRadius) {
        double qtyDiffRatio = Math.abs(myQty - theirQty) / Math.max(myQty, theirQty);
        int qtyScore = (int)((1.0 - qtyDiffRatio) * 50);

        // Safely avoid division by zero if maxRadius is 0
        double distRatio = (maxRadius > 0) ? (distance / maxRadius) : 1.0;
        int distScore = (int)((1.0 - distRatio) * 50);

        return Math.max(20, Math.min(100, qtyScore + distScore));
    }
}