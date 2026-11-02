
package com.renode.controller;

import com.renode.entity.Company;
import com.renode.entity.ResourceListing;
import com.renode.entity.TradeRequest;
import com.renode.repository.CompanyRepository;
import com.renode.repository.ResourceListingRepository;
import com.renode.repository.TradeRequestRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    private TradeRequestRepository tradeRequestRepository;

    @Autowired
    private CompanyRepository companyRepository;

    // Get authenticated company
    private Company getAuthenticatedCompany() {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User not authenticated"
            );
        }

        return companyRepository.findByContactEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Company not found"
                ));
    }

    // Dashboard Statistics API
    @GetMapping("/{companyId}/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @PathVariable Integer companyId) {

        Company authCompany = getAuthenticatedCompany();

        if (!authCompany.getId().equals(companyId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error",
                            "Unauthorized access to dashboard"
                    ));
        }

        Map<String, Object> stats = new HashMap<>();

        try {

            // Incoming and outgoing trade requests
            List<TradeRequest> incoming =
                    tradeRequestRepository.findByReceiverId(companyId);

            List<TradeRequest> outgoing =
                    tradeRequestRepository.findBySenderId(companyId);

            // Completed matches
            long completedMatches =
                    incoming.stream()
                            .filter(r -> "COMPLETED".equalsIgnoreCase(r.getStatus()))
                            .count()
                            +
                            outgoing.stream()
                                    .filter(r -> "COMPLETED".equalsIgnoreCase(r.getStatus()))
                                    .count();

            // Pending requests
            long pendingRequests =
                    incoming.stream()
                            .filter(r -> "PENDING".equalsIgnoreCase(r.getStatus()))
                            .count()
                            +
                            outgoing.stream()
                                    .filter(r -> "PENDING".equalsIgnoreCase(r.getStatus()))
                                    .count();

            stats.put("successfulMatches", completedMatches);
            stats.put("pendingRequests", pendingRequests);

            // Resource listings
            List<ResourceListing> myListings =
                    listingRepository.findByCompanyId(companyId);

            long totalWaste = myListings.stream()
                    .filter(l -> l.getListingType() != null &&
                            l.getListingType().toUpperCase().contains("WASTE"))
                    .count();

            long totalReq = myListings.stream()
                    .filter(l -> l.getListingType() != null &&
                            l.getListingType().toUpperCase().contains("REQ"))
                    .count();

            stats.put("totalWasteListed", totalWaste);
            stats.put("activeRequirements", totalReq);

            // Sustainability calculations
            double totalReusedTons = incoming.stream()
                    .filter(req -> "COMPLETED".equalsIgnoreCase(req.getStatus()))
                    .mapToDouble(TradeRequest::getRequestedQuantity)
                    .sum();

            stats.put("sustainabilityTons", totalReusedTons);
            stats.put("sustainabilityCO2", totalReusedTons * 1.5);

            // Completed transactions
            stats.put(
                    "completedTransactions",
                    incoming.stream()
                            .filter(r -> "COMPLETED".equalsIgnoreCase(r.getStatus()))
                            .count()
            );

            // Recent activity
            List<TradeRequest> recentActivity = incoming.stream()
                    .sorted((a, b) ->
                            Integer.compare(b.getId(), a.getId()))
                    .limit(3)
                    .collect(Collectors.toList());

            stats.put("recentActivity", recentActivity);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {

            stats.put(
                    "error",
                    "Error fetching dashboard stats: " + e.getMessage()
            );

            return ResponseEntity.badRequest().body(stats);
        }
    }
}
