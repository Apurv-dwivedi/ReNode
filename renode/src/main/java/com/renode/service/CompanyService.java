package com.renode.service;

import com.renode.entity.Company;
import com.renode.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    public List<Company> getNearbyFactories(Integer factoryId, double radiusInKm) {
        Company currentFactory = companyRepository.findById(factoryId)
                .orElseThrow(() -> new RuntimeException("Factory not found with ID: " + factoryId));

        // 🔒 FIX: Gracefully handle companies without established coordinates
        if (currentFactory.getLocation() == null) {
            throw new IllegalStateException("Company location coordinates are not set. Cannot determine nearby factories.");
        }

        double lon = currentFactory.getLocation().getX();
        double lat = currentFactory.getLocation().getY();

        List<Company> nearby = companyRepository.findNearbyFactories(lon, lat, radiusInKm);

        // 🔒 SECURE: Strip out sensitive data from neighboring companies before sending to frontend map
        return nearby.stream().map(company -> {
            company.setPassword(null);
            company.setApiKey(null);
            company.setWebhookUrl(null);
            return company;
        }).collect(Collectors.toList());
    }
}