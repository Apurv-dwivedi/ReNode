package com.renode.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthCheckController {

    @GetMapping("/")
    public String rootMessage() {
        return "ReNode Backend is running successfully! 🚀";
    }

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "ReNode Backend");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}