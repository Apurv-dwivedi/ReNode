package com.renode.controller;

import com.renode.repository.CompanyRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import com.renode.entity.Company;
import com.renode.entity.ResourceListing;
import com.renode.security.JwtUtil;
import org.springframework.security.core.userdetails.UserDetails;
import com.renode.security.CustomUserDetailsService;
import com.renode.dto.MatchResponse;
import com.renode.service.CompanyService;
import com.renode.service.MatchmakingService;
import com.renode.repository.ResourceListingRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/companies")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class CompanyController {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private ResourceListingRepository resourceListingRepository;

    @Autowired
    private MatchmakingService matchmakingService;

    @GetMapping("/{id}/nearby")
    public List<Company> getNearby(@PathVariable Integer id, @RequestParam double radius) {
        return companyService.getNearbyFactories(id, radius);
    }

    @GetMapping("/{id}/resources")
    public List<ResourceListing> getCompanyResources(@PathVariable Integer id) {
        return resourceListingRepository.findByCompanyId(id);
    }

    @GetMapping("/{id}/matches")
    public List<MatchResponse> getPerfectMatches(@PathVariable Integer id, @RequestParam double radius) {
        return matchmakingService.findMatchesForCompany(id, radius);
    }

    @PostMapping("/{id}/resources")
    public ResourceListing addResource(@PathVariable Integer id, @RequestBody ResourceListing newResource) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        newResource.setCompany(company);
        return resourceListingRepository.save(newResource);
    }

    // --- REGISTER NEW FACTORY ---
    @PostMapping("/register")
    public ResponseEntity<?> registerFactory(@RequestBody Map<String, Object> payload) {
        try {
            Company company = new Company();

            // Safe parsing: Null check lagaya hai taaki crash na ho
            if (payload.get("companyName") != null) {
                company.setCompanyName(String.valueOf(payload.get("companyName")));
            }
            if (payload.get("industryType") != null) {
                company.setIndustryType(String.valueOf(payload.get("industryType")));
            }
            if (payload.get("contactEmail") != null) {
                company.setContactEmail(String.valueOf(payload.get("contactEmail")));
            }
            if (payload.get("password") != null) {
                company.setPassword(passwordEncoder.encode(String.valueOf(payload.get("password"))));
            }

            // Location ki safe parsing
            if (payload.get("latitude") != null && payload.get("longitude") != null) {
                double lat = Double.parseDouble(String.valueOf(payload.get("latitude")));
                double lon = Double.parseDouble(String.valueOf(payload.get("longitude")));

                org.locationtech.jts.geom.GeometryFactory geometryFactory = new org.locationtech.jts.geom.GeometryFactory();
                org.locationtech.jts.geom.Point location = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(lon, lat));
                location.setSRID(4326);
                company.setLocation(location);
            }

            companyRepository.save(company);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Factory Registered Successfully!");
            return ResponseEntity.ok(response);

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Agar email pehle se exist karta hai, toh yeh clear error dega
            Map<String, String> error = new HashMap<>();
            error.put("error", "Email already exists! Please use a different email or login.");
            return ResponseEntity.status(400).body(error);
        } catch (Exception e) {
            // Backend console me exact error line print karega
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Server Error: " + e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }
    // --- LOGIN ENDPOINT ---
    @PostMapping("/login")
    public ResponseEntity<?> loginFactory(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        Company company = companyRepository.findByContactEmail(email).orElse(null);
        if (company == null) {
            return ResponseEntity.status(401).body("Company not found with this email!");
        }

        // Password match check karna
        if (passwordEncoder.matches(password, company.getPassword())) {

            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
            String token = jwtUtil.generateToken(userDetails.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login Successful!");
            response.put("companyId", company.getId());
            response.put("companyName", company.getCompanyName());
            response.put("token", token); // <-- YEH NAYA ADD KIYA HAI

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body("Invalid Password!");
        }
    }

    @GetMapping("/stats")
    public Map<String, Object> getPlatformStats() {
        Map<String, Object> stats = new HashMap<>();
        long totalFactories = companyRepository.count();
        stats.put("totalFactories", totalFactories);

        long totalResources = resourceListingRepository.count();
        stats.put("totalResources", totalResources);

        double totalWasteTons = 0;
        List<ResourceListing> allResources = resourceListingRepository.findAll();
        for(ResourceListing res : allResources) {
            if(res.getListingType().equalsIgnoreCase("WASTE")) {
                totalWasteTons += res.getQuantityInTons();
            }
        }
        stats.put("totalWasteTons", totalWasteTons);

        return stats;
    }

    @DeleteMapping("/{companyId}/resources/{resourceId}")
    public ResponseEntity<?> deleteResource(@PathVariable Integer companyId, @PathVariable Integer resourceId) {
        try {
            resourceListingRepository.deleteById(resourceId);
            return ResponseEntity.ok().body("{\"message\": \"Resource deleted successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"Could not delete resource\"}");
        }
    }
    // --- UPDATE COMPANY PROFILE ---
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCompanyProfile(@PathVariable Integer id, @RequestBody Map<String, String> updates) {
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            return ResponseEntity.status(404).body("Company not found!");
        }

        if (updates.containsKey("companyName")) {
            company.setCompanyName(updates.get("companyName"));
        }
        if (updates.containsKey("industryType")) {
            company.setIndustryType(updates.get("industryType"));
        }
        if (updates.containsKey("latitude") && updates.containsKey("longitude")) {
            double lat = Double.parseDouble(updates.get("latitude"));
            double lon = Double.parseDouble(updates.get("longitude"));

            GeometryFactory geometryFactory = new GeometryFactory();
            Point newLocation = geometryFactory.createPoint(new Coordinate(lon, lat));
            newLocation.setSRID(4326);

            company.setLocation(newLocation);
        }

        companyRepository.save(company);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Profile updated successfully!");
        return ResponseEntity.ok(response);
    }
}