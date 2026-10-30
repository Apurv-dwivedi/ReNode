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
    private TradeRequestRepository tradeRequestRepository;

    @Autowired
    private TradeMessageRepository tradeMessageRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ResourceListingRepository resourceListingRepository;

    // Send Deal / Trade Request
    @PostMapping("/send")
    public ResponseEntity<?> sendRequest(@RequestBody Map<String, Object> payload) {
        try {
            Integer senderId = Integer.parseInt(String.valueOf(payload.get("senderId")));
            Integer receiverId = Integer.parseInt(String.valueOf(payload.get("receiverId")));
            Integer resourceId = Integer.parseInt(String.valueOf(payload.get("resourceId")));
            Double qty = Double.parseDouble(String.valueOf(payload.get("requestedQuantity")));

            Company sender = companyRepository.findById(senderId).orElseThrow(() -> new RuntimeException("Sender not found"));
            Company receiver = companyRepository.findById(receiverId).orElseThrow(() -> new RuntimeException("Receiver not found"));
            ResourceListing resource = resourceListingRepository.findById(resourceId).orElseThrow(() -> new RuntimeException("Resource not found"));

            TradeRequest request = new TradeRequest();
            request.setSender(sender);
            request.setReceiver(receiver);
            request.setResource(resource);
            request.setRequestedQuantity(qty);
            request.setStatus("PENDING");

            tradeRequestRepository.save(request);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Trade request sent successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // Get Incoming Requests for a Company
    @GetMapping("/incoming/{companyId}")
    public List<TradeRequest> getIncomingRequests(@PathVariable Integer companyId) {
        return tradeRequestRepository.findByReceiverId(companyId);
    }

    // Get Outgoing Requests by a Company
    @GetMapping("/outgoing/{companyId}")
    public List<TradeRequest> getOutgoingRequests(@PathVariable Integer companyId) {
        return tradeRequestRepository.findBySenderId(companyId);
    }

    @PostMapping("/{requestId}/messages")
    public ResponseEntity<?> sendTradeMessage(@PathVariable Integer requestId, @RequestBody Map<String, Object> payload) {
        try {
            Integer companyId = Integer.parseInt(String.valueOf(payload.get("companyId")));
            String content = String.valueOf(payload.get("messageContent"));

            TradeRequest request = tradeRequestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Trade request not found"));
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new RuntimeException("Company not found"));

            TradeMessage msg = new TradeMessage();
            msg.setTradeRequest(request);
            msg.setSenderCompany(company);
            msg.setMessageContent(content);

            tradeMessageRepository.save(msg);
            return ResponseEntity.ok(Map.of("message", "Message sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // Update Request Status (Accept, Reject, Complete)
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
        TradeRequest request = tradeRequestRepository.findById(id).orElseThrow(() -> new RuntimeException("Request not found"));
        String newStatus = payload.get("status");

        if (newStatus != null) {
            request.setStatus(newStatus);
            tradeRequestRepository.save(request);
        }

        return ResponseEntity.ok(Map.of("message", "Request status updated to " + newStatus));
    }
    @GetMapping("/{requestId}/messages")
    public List<TradeMessage> getTradeMessages(@PathVariable Integer requestId) {
        return tradeMessageRepository.findByTradeRequestId(requestId);
    }
}