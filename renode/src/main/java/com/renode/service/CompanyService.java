package com.renode.service;

import com.renode.entity.Company;
import com.renode.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    public List<Company> getNearbyFactories(Integer factoryId, double radiusInKm) {
        Company currentFactory = companyRepository.findById(factoryId)
                .orElseThrow(() -> new RuntimeException("Factory not found with ID: " + factoryId));

        // Point object se exact coordinates nikal lo
        double lon = currentFactory.getLocation().getX();
        double lat = currentFactory.getLocation().getY();

        return companyRepository.findNearbyFactories(lon, lat, radiusInKm);
    }
}