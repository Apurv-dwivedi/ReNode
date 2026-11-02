package com.renode.controller;

import com.renode.entity.TradeMessage;
import com.renode.entity.TradeRequest;
import com.renode.entity.Company;
import com.renode.entity.ResourceListing;
import com.renode.repository.TradeMessageRepository;
import com.renode.repository.TradeRequestRepository;
import com.renode.repository.CompanyRepository;
import com.renode.repository.ResourceListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "*")
public class RequestController {

    @Autowired
    private TradeRequestRepository tradeRequestRepository;

    @Autowired
    private TradeMessageRepository tradeMessageRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ResourceListingRepository resourceListingRepository;

    // Helper: Securely get authenticated company from JWT
    private Company getAuthenticatedCompany() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return companyRepository.findByContactEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated"));
    }

    // Send Deal / Trade Request
    @PostMapping("/send")
    public ResponseEntity<?> sendRequest(@RequestBody Map<String, Object> payload) {
        try {
            Company sender = getAuthenticatedCompany(); // 🔒 SECURE: Ignore senderId from frontend
            Integer receiverId = Integer.parseInt(String.valueOf(payload.get("receiverId")));
            Integer resourceId = Integer.parseInt(String.valueOf(payload.get("resourceId")));
            Double qty = Double.parseDouble(String.valueOf(payload.get("requestedQuantity")));

            Company receiver = companyRepository.findById(receiverId)
                    .orElseThrow(() -> new RuntimeException("Receiver not found"));
            ResourceListing resource = resourceListingRepository.findById(resourceId)
                    .orElseThrow(() -> new RuntimeException("Resource not found"));

            TradeRequest request = new TradeRequest();
            request.setSender(sender);
            request.setReceiver(receiver);
            request.setResource(resource);
            request.setRequestedQuantity(qty);
            request.setStatus("PENDING");

            tradeRequestRepository.save(request);

            return ResponseEntity.ok(Map.of("message", "Trade request sent successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/incoming/{companyId}")
    public ResponseEntity<List<TradeRequest>> getIncomingRequests(@PathVariable Integer companyId) {
        Company authCompany = getAuthenticatedCompany();
        if (!authCompany.getId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view another company's requests"); // 🔒 SECURE: IDOR Fix
        }
        return ResponseEntity.ok(tradeRequestRepository.findByReceiverId(companyId));
    }

    @GetMapping("/outgoing/{companyId}")
    public ResponseEntity<List<TradeRequest>> getOutgoingRequests(@PathVariable Integer companyId) {
        Company authCompany = getAuthenticatedCompany();
        if (!authCompany.getId().equals(companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view another company's requests"); // 🔒 SECURE: IDOR Fix
        }
        return ResponseEntity.ok(tradeRequestRepository.findBySenderId(companyId));
    }

    @PostMapping("/{requestId}/messages")
    public ResponseEntity<?> sendTradeMessage(@PathVariable Integer requestId, @RequestBody Map<String, Object> payload) {
        try {
            Company authCompany = getAuthenticatedCompany();
            String content = String.valueOf(payload.get("messageContent"));

            TradeRequest request = tradeRequestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Trade request not found"));

            // 🔒 SECURE: Ensure only participants can send messages
            if (!request.getSender().getId().equals(authCompany.getId()) && !request.getReceiver().getId().equals(authCompany.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Not a participant in this trade"));
            }

            TradeMessage msg = new TradeMessage();
            msg.setTradeRequest(request);
            msg.setSenderCompany(authCompany); // Force authentic sender
            msg.setMessageContent(content);

            tradeMessageRepository.save(msg);
            return ResponseEntity.ok(Map.of("message", "Message sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
        Company authCompany = getAuthenticatedCompany();
        TradeRequest request = tradeRequestRepository.findById(id).orElseThrow(() -> new RuntimeException("Request not found"));

        // 🔒 SECURE: Only receiver can accept/reject. Sender can only CANCEL.
        String newStatus = payload.get("status");
        if (newStatus != null) {
            boolean isReceiver = request.getReceiver().getId().equals(authCompany.getId());
            boolean isSender = request.getSender().getId().equals(authCompany.getId());

            if (!isReceiver && !isSender) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Unauthorized to update this request"));
            }
            if (isSender && !newStatus.equalsIgnoreCase("CANCELLED")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Sender can only CANCEL requests"));
            }

            request.setStatus(newStatus.toUpperCase());
            tradeRequestRepository.save(request);
        }

        return ResponseEntity.ok(Map.of("message", "Request status updated to " + newStatus));
    }

    @GetMapping("/{requestId}/messages")
    public ResponseEntity<List<TradeMessage>> getTradeMessages(@PathVariable Integer requestId) {
        Company authCompany = getAuthenticatedCompany();
        TradeRequest request = tradeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));

        // 🔒 SECURE: Ensure participant access only
        if (!request.getSender().getId().equals(authCompany.getId()) && !request.getReceiver().getId().equals(authCompany.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized to view these messages");
        }

        return ResponseEntity.ok(tradeMessageRepository.findByTradeRequestId(requestId));
    }
}