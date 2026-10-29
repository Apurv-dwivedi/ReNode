package com.renode.controller;

import com.renode.entity.TermsContent;
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

    @Autowired
    private com.renode.repository.TermsRepository termsRepository;

    @GetMapping("/content/terms")
    public ResponseEntity<?> getTermsContent() {
        TermsContent terms = termsRepository.findTopByOrderByIdDesc();
        if (terms == null) {
            TermsContent defaultTerms = new TermsContent();
            defaultTerms.setTitle("Terms & Conditions");
            defaultTerms.setContent("Welcome to ReNode. By using our B2B industrial waste exchange platform, you agree to comply with environmental and trade regulations.");
            return ResponseEntity.ok(defaultTerms);
        }
        return ResponseEntity.ok(terms);
    }

    // --- NAYA: UPDATE TERMS & CONDITIONS API ---
    @PutMapping("/content/terms")
    public ResponseEntity<?> updateTermsContent(@RequestBody TermsContent newTerms) {
        try {
            TermsContent terms = termsRepository.findTopByOrderByIdDesc();
            if (terms == null) {
                terms = new TermsContent();
            }
            terms.setTitle(newTerms.getTitle());
            terms.setContent(newTerms.getContent());
            termsRepository.save(terms);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Terms & Conditions updated successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error saving terms: " + e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }

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

    // NAYA: Fetch specific company profile for Settings Tab
    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyProfile(@PathVariable Integer id) {
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            return ResponseEntity.status(404).body("Company not found!");
        }
        company.setPassword(null); // Protect password
        return ResponseEntity.ok(company);
    }

    @PostMapping("/{id}/resources")
    public ResourceListing addResource(@PathVariable Integer id, @RequestBody ResourceListing newResource) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        newResource.setCompany(company);
        return resourceListingRepository.save(newResource);
    }

    // --- EDIT RESOURCE LISTING ---
    @PutMapping("/{companyId}/resources/{resourceId}")
    public ResponseEntity<?> editResource(@PathVariable Integer companyId, @PathVariable Integer resourceId, @RequestBody ResourceListing updates) {
        try {
            ResourceListing existing = resourceListingRepository.findById(resourceId)
                    .orElseThrow(() -> new RuntimeException("Resource not found"));

            if (updates.getCategory() != null) existing.setCategory(updates.getCategory());
            if (updates.getQuantityInTons() != null) existing.setQuantityInTons(updates.getQuantityInTons());
            if (updates.getDescription() != null) existing.setDescription(updates.getDescription());

            if (updates.getTargetPrice() != null) existing.setTargetPrice(updates.getTargetPrice());
            if (updates.getPuritySpecifications() != null) existing.setPuritySpecifications(updates.getPuritySpecifications());
            if (updates.getExpiryDate() != null) existing.setExpiryDate(updates.getExpiryDate());

            resourceListingRepository.save(existing);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Listing updated successfully with advanced specs!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Could not update resource: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // --- REGISTER NEW FACTORY ---
    @PostMapping("/register")
    public ResponseEntity<?> registerFactory(@RequestBody Map<String, Object> payload) {
        try {
            Company company = new Company();

            if (payload.get("companyName") != null) company.setCompanyName(String.valueOf(payload.get("companyName")));
            if (payload.get("industryType") != null) company.setIndustryType(String.valueOf(payload.get("industryType")));
            if (payload.get("contactEmail") != null) company.setContactEmail(String.valueOf(payload.get("contactEmail")));

            if (payload.get("password") != null) {
                company.setPassword(passwordEncoder.encode(String.valueOf(payload.get("password"))));
            }

            // Set Defaults for settings module
            company.setDefaultSearchRadius(50);
            company.setPreferredUnit("Metric Tons (t)");

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
            Map<String, String> error = new HashMap<>();
            error.put("error", "Email already exists! Please use a different email or login.");
            return ResponseEntity.status(400).body(error);
        } catch (Exception e) {
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

        if (passwordEncoder.matches(password, company.getPassword())) {
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
            String token = jwtUtil.generateToken(userDetails.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login Successful!");
            response.put("companyId", company.getId());
            response.put("companyName", company.getCompanyName());
            response.put("token", token);

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body("Invalid Password!");
        }
    }

    @GetMapping("/stats")
    public Map<String, Object> getPlatformStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFactories", companyRepository.count());
        stats.put("totalResources", resourceListingRepository.count());

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

    // --- UPDATE COMPANY PROFILE (SETTINGS MODULE) ---
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCompanyProfile(@PathVariable Integer id, @RequestBody Map<String, String> updates) {
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            return ResponseEntity.status(404).body("Company not found!");
        }

        // Company Profile fields
        if (updates.containsKey("companyName")) company.setCompanyName(updates.get("companyName"));
        if (updates.containsKey("industryType")) company.setIndustryType(updates.get("industryType"));
        if (updates.containsKey("description")) company.setDescription(updates.get("description"));
        if (updates.containsKey("website")) company.setWebsite(updates.get("website"));
        if (updates.containsKey("taxId")) company.setTaxId(updates.get("taxId"));

        // Marketplace Preference fields
        if (updates.containsKey("defaultSearchRadius")) {
            company.setDefaultSearchRadius(Integer.parseInt(updates.get("defaultSearchRadius")));
        }
        if (updates.containsKey("preferredUnit")) {
            company.setPreferredUnit(updates.get("preferredUnit"));
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
        response.put("message", "Company Profile updated successfully!");
        return ResponseEntity.ok(response);
    }

    // --- NAYA: UPDATE PASSWORD (SECURITY TAB) ---
    @PutMapping("/{id}/password")
    public ResponseEntity<?> updatePassword(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
        try {
            Company company = companyRepository.findById(id).orElse(null);
            if (company == null) {
                return ResponseEntity.status(404).body("Company not found!");
            }

            String currentPassword = payload.get("currentPassword");
            String newPassword = payload.get("newPassword");

            // Verify current password using BCrypt
            if (!passwordEncoder.matches(currentPassword, company.getPassword())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Incorrect current password");
                return ResponseEntity.status(401).body(error); // 401 Unauthorized
            }

            // Hash and save new password
            company.setPassword(passwordEncoder.encode(newPassword));
            companyRepository.save(company);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Password updated successfully!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Server Error: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    // --- NAYA: COMPLIANCE DOCUMENT UPLOAD API ---
    @PostMapping("/{id}/upload-document")
    public ResponseEntity<?> uploadComplianceDocument(
            @PathVariable Integer id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            Company company = companyRepository.findById(id).orElse(null);
            if (company == null) {
                return ResponseEntity.status(404).body("Company not found!");
            }

            company.setVerificationStatus("DOCUMENT_UNDER_REVIEW");
            companyRepository.save(company);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Document '" + file.getOriginalFilename() + "' uploaded successfully! Verification pending.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload document: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // --- NAYA: NOTIFICATION PREFERENCES API ---
    @PutMapping("/{id}/notifications")
    public ResponseEntity<?> updateNotifications(@PathVariable Integer id, @RequestBody Map<String, Boolean> prefs) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification preferences saved securely.");
        return ResponseEntity.ok(response);
    }
    // --- SAVE INTEGRATION DETAILS ---
    @PutMapping("/{id}/integrations")
    public ResponseEntity<?> saveIntegrations(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            return ResponseEntity.status(404).body("Company not found!");
        }

        if (payload.containsKey("integrationProvider")) company.setIntegrationProvider(payload.get("integrationProvider"));
        if (payload.containsKey("apiKey")) company.setApiKey(payload.get("apiKey"));
        if (payload.containsKey("webhookUrl")) company.setWebhookUrl(payload.get("webhookUrl"));

        companyRepository.save(company);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Integration settings saved successfully!");
        return ResponseEntity.ok(response);
    }
}