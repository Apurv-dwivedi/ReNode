package com.renode.service;

import com.renode.dto.MatchResponse;
import com.renode.entity.Company;
import com.renode.entity.ResourceListing;
import com.renode.repository.ResourceListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MatchmakingService {

    @Autowired
    private CompanyService companyService; // Radius search ke liye

    @Autowired
    private ResourceListingRepository resourceRepo; // Waste/Requirements nikalne ke liye

    public List<MatchResponse> findMatchesForCompany(Integer companyId, double radius) {
        List<MatchResponse> successfulMatches = new ArrayList<>();

        // 1. Current factory ke saare resources nikalo aur sirf "REQUIREMENTS" filter karo
        List<ResourceListing> myResources = resourceRepo.findByCompanyId(companyId);
        List<ResourceListing> myRequirements = new ArrayList<>();
        for (ResourceListing res : myResources) {
            if (res.getListingType().equalsIgnoreCase("REQUIREMENT")) {
                myRequirements.add(res);
            }
        }

        // Agar is factory ko kuch chahiye hi nahi, toh blank return kardo
        if (myRequirements.isEmpty()) {
            return successfulMatches;
        }

        // 2. Aas-paas ki saari factories nikalo (Haversine formula use karke)
        List<Company> nearbyFactories = companyService.getNearbyFactories(companyId, radius);

        // 3. MATCHMAKING LOGIC
        for (ResourceListing req : myRequirements) {
            for (Company nearby : nearbyFactories) {
                // Aas paas wali factory ka resource nikalo
                List<ResourceListing> nearbyResources = resourceRepo.findByCompanyId(nearby.getId());

                for (ResourceListing supply : nearbyResources) {
                    // Check: Kya yeh 'WASTE' hai? Aur kya category match ho rahi hai? (e.g., Cotton Scrap == Cotton Scrap)
                    if (supply.getListingType().equalsIgnoreCase("WASTE") &&
                            supply.getCategory().equalsIgnoreCase(req.getCategory())) {

                        // Boom! Match mil gaya. Ise DTO me daal kar list me add kar do.
                        successfulMatches.add(new MatchResponse(
                                req.getCategory(),
                                req.getQuantityInTons(),
                                nearby.getCompanyName(),
                                nearby.getContactEmail(),
                                supply.getQuantityInTons()
                        ));
                    }
                }
            }
        }

        return successfulMatches;
    }
}