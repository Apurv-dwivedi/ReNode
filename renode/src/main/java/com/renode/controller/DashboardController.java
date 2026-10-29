package com.renode.controller;

import com.renode.entity.RequestStatus;
import com.renode.entity.ResourceListing;
import com.renode.repository.ResourceListingRepository;
import com.renode.repository.ResourceRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private ResourceListingRepository listingRepository;

    @Autowired
    private ResourceRequestRepository requestRepository;

    @GetMapping("/{companyId}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable Integer companyId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 1. Total Successful Matches (Jo requests COMPLETED ho chuki hain)
            long completedIncoming = requestRepository.countByReceiverIdAndStatus(companyId, RequestStatus.COMPLETED);
            long completedOutgoing = requestRepository.countBySenderIdAndStatus(companyId, RequestStatus.COMPLETED);
            stats.put("successfulMatches", completedIncoming + completedOutgoing);

            // 2. Pending Requests (Jin par abhi action lena baaki hai)
            long pendingIncoming = requestRepository.countByReceiverIdAndStatus(companyId, RequestStatus.PENDING);
            long pendingOutgoing = requestRepository.countBySenderIdAndStatus(companyId, RequestStatus.PENDING);
            stats.put("pendingRequests", pendingIncoming + pendingOutgoing);

            // 3. Active Listings (Waste aur Requirement ka count)
            List<ResourceListing> allListings = listingRepository.findAll();
            long totalWaste = 0;
            long totalReq = 0;

            for (ResourceListing listing : allListings) {
                if (listing.getCompany() != null && listing.getCompany().getId().equals(companyId)) {
                    String type = String.valueOf(listing.getListingType()).toUpperCase();
                    if (type.contains("WASTE")) {
                        totalWaste++;
                    } else if (type.contains("REQ")) {
                        totalReq++;
                    }
                }
            }

            stats.put("totalWasteListed", totalWaste);
            stats.put("activeRequirements", totalReq);

            // NEW: Sustainability Impact & Recent Activity Calculation
            List<com.renode.entity.ResourceRequest> allIncoming = requestRepository.findByReceiverIdOrderByCreatedAtDesc(companyId);

            double totalReusedTons = 0;
            int completedTransactions = 0;

            for(com.renode.entity.ResourceRequest req : allIncoming) {
                if(req.getStatus() == RequestStatus.COMPLETED) {
                    totalReusedTons += req.getRequestedQuantity();
                    completedTransactions++;
                }
            }

            stats.put("sustainabilityTons", totalReusedTons);
            stats.put("sustainabilityCO2", totalReusedTons * 1.5); // CO2 formula
            stats.put("completedTransactions", completedTransactions);

            // Recent Activity Preview (Top 3 requests)
            List<com.renode.entity.ResourceRequest> recentActivity = allIncoming.stream().limit(3).collect(Collectors.toList());
            stats.put("recentActivity", recentActivity);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            stats.put("error", "Error fetching dashboard stats: " + e.getMessage());
            return ResponseEntity.badRequest().body(stats);
        }
    }
}