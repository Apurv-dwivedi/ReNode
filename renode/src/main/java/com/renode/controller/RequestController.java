package com.renode.controller;

import com.renode.entity.Company;
import com.renode.entity.RequestStatus;
import com.renode.entity.ResourceListing; // EXACT NAME IMPORTED
import com.renode.entity.ResourceRequest;
import com.renode.repository.CompanyRepository;
import com.renode.repository.ResourceListingRepository; // EXACT REPO IMPORTED
import com.renode.repository.ResourceRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "*")
public class RequestController {

    @Autowired
    private ResourceRequestRepository requestRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ResourceListingRepository resourceRepository; // UPDATED HERE

    // 1. Send a new request
    @PostMapping("/send")
    public ResponseEntity<?> sendRequest(@RequestBody Map<String, Object> payload) {
        try {
            Integer senderId = Integer.parseInt(payload.get("senderId").toString());
            Integer receiverId = Integer.parseInt(payload.get("receiverId").toString());
            Integer resourceId = Integer.parseInt(payload.get("resourceId").toString());
            Double qty = Double.parseDouble(payload.get("requestedQuantity").toString());

            Company sender = companyRepository.findById(senderId).orElseThrow();
            Company receiver = companyRepository.findById(receiverId).orElseThrow();
            ResourceListing resource = resourceRepository.findById(resourceId).orElseThrow(); // UPDATED HERE

            ResourceRequest request = new ResourceRequest();
            request.setSender(sender);
            request.setReceiver(receiver);
            request.setResource(resource);
            request.setRequestedQuantity(qty);

            requestRepository.save(request);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Request sent successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to send request: " + e.getMessage());
        }
    }

    // 2. Get incoming requests for a company
    @GetMapping("/incoming/{companyId}")
    public ResponseEntity<List<ResourceRequest>> getIncomingRequests(@PathVariable Integer companyId) {
        return ResponseEntity.ok(requestRepository.findByReceiverIdOrderByCreatedAtDesc(companyId));
    }

    // 3. Get outgoing requests for a company
    @GetMapping("/outgoing/{companyId}")
    public ResponseEntity<List<ResourceRequest>> getOutgoingRequests(@PathVariable Integer companyId) {
        return ResponseEntity.ok(requestRepository.findBySenderIdOrderByCreatedAtDesc(companyId));
    }

    // 4. Update request status (Accept/Reject/Complete)
    @PutMapping("/{requestId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Integer requestId, @RequestBody Map<String, String> payload) {
        try {
            ResourceRequest request = requestRepository.findById(requestId).orElseThrow();
            RequestStatus newStatus = RequestStatus.valueOf(payload.get("status").toUpperCase());

            request.setStatus(newStatus);
            requestRepository.save(request);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Status updated to " + newStatus);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update status");
        }
    }
}