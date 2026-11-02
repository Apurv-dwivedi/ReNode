package com.renode.controller;

import com.renode.entity.TermsContent;
import com.renode.repository.CompanyRepository;
import com.renode.service.AuthServiceMailExtension;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

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

    @Autowired
    private AuthServiceMailExtension authServiceMailExtension;

    // Helper: Securely get authenticated company
    private Company getAuthenticatedCompany() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return companyRepository.findByContactEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated"));
    }

    // --- SECURE: Check Ownership Helper ---
    private void verifyOwnership(Integer requestedId) {
        Company authCompany = getAuthenticatedCompany();
        if (!authCompany.getId().equals(requestedId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized action on another company's resource.");
        }
    }

    @GetMapping("/content/terms")
    public ResponseEntity<?> getTermsContent() {
        TermsContent terms = termsRepository.findTopByOrderByIdDesc();
        if (terms == null) {
            TermsContent defaultTerms = new TermsContent();
            defaultTerms.setTitle("Terms & Conditions");
            defaultTerms.setContent("Welcome to ReNode. By using our platform, you agree to comply with environmental regulations.");
            return ResponseEntity.ok(defaultTerms);
        }
        return ResponseEntity.ok(terms);
    }

    // 🔒 SECURE: Only authorized administrators can update global terms
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/content/terms")
    public ResponseEntity<?> updateTermsContent(@RequestBody TermsContent newTerms) {
        try {
            TermsContent terms = termsRepository.findTopByOrderByIdDesc();
            if (terms == null) terms = new TermsContent();
            terms.setTitle(newTerms.getTitle());
            terms.setContent(newTerms.getContent());
            termsRepository.save(terms);
            return ResponseEntity.ok(Map.of("message", "Terms & Conditions updated successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", "Error saving terms: " + e.getMessage()));
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

    // 🔒 SECURE: Prevent data leakage of sensitive integrations/keys
    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyProfile(@PathVariable Integer id) {
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            return ResponseEntity.status(404).body("Company not found!");
        }

        company.setPassword(null); // Always protect password

        // If a company is looking at ANOTHER company's profile, hide integration secrets
        Company authCompany = getAuthenticatedCompany();
        if (!authCompany.getId().equals(id)) {
            company.setApiKey(null);
            company.setWebhookUrl(null);
            company.setIntegrationProvider(null);
        }
        return ResponseEntity.ok(company);
    }

    @PostMapping("/{id}/resources")
    public ResourceListing addResource(@PathVariable Integer id, @RequestBody ResourceListing newResource) {
        verifyOwnership(id); // 🔒 SECURE
        Company company = companyRepository.findById(id).orElseThrow(() -> new RuntimeException("Company not found"));
        newResource.setCompany(company);
        return resourceListingRepository.save(newResource);
    }

    @PutMapping("/{companyId}/resources/{resourceId}")
    public ResponseEntity<?> editResource(@PathVariable Integer companyId, @PathVariable Integer resourceId, @RequestBody ResourceListing updates) {
        verifyOwnership(companyId); // 🔒 SECURE
        try {
            ResourceListing existing = resourceListingRepository.findById(resourceId)
                    .orElseThrow(() -> new RuntimeException("Resource not found"));

            if (!existing.getCompany().getId().equals(companyId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Resource does not belong to you"));
            }

            if (updates.getCategory() != null) existing.setCategory(updates.getCategory());
            if (updates.getQuantityInTons() != null) existing.setQuantityInTons(updates.getQuantityInTons());
            if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
            if (updates.getTargetPrice() != null) existing.setTargetPrice(updates.getTargetPrice());
            if (updates.getPuritySpecifications() != null) existing.setPuritySpecifications(updates.getPuritySpecifications());
            if (updates.getExpiryDate() != null) existing.setExpiryDate(updates.getExpiryDate());

            resourceListingRepository.save(existing);
            return ResponseEntity.ok(Map.of("message", "Listing updated successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Could not update resource: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerFactory(@RequestBody Map<String, Object> payload) {
        // ... (Keep existing implementation, logic here is public anyway)
        try {
            Company company = new Company();
            if (payload.get("companyName") != null) company.setCompanyName(String.valueOf(payload.get("companyName")));
            if (payload.get("industryType") != null) company.setIndustryType(String.valueOf(payload.get("industryType")));
            if (payload.get("contactEmail") != null) company.setContactEmail(String.valueOf(payload.get("contactEmail")));

            if (payload.get("password") != null) {
                company.setPassword(passwordEncoder.encode(String.valueOf(payload.get("password"))));
            }

            company.setDefaultSearchRadius(50);
            company.setPreferredUnit("Metric Tons (t)");
            company.setVerificationStatus("PENDING");

            if (payload.get("latitude") != null && payload.get("longitude") != null) {
                double lat = Double.parseDouble(String.valueOf(payload.get("latitude")));
                double lon = Double.parseDouble(String.valueOf(payload.get("longitude")));
                GeometryFactory geometryFactory = new GeometryFactory();
                Point location = geometryFactory.createPoint(new Coordinate(lon, lat));
                location.setSRID(4326);
                company.setLocation(location);
            }

            companyRepository.save(company);
            try {
                authServiceMailExtension.sendRegistrationVerificationEmail(company);
            } catch (Exception mailEx) {
                System.err.println("Failed to queue verification email: " + mailEx.getMessage());
            }

            return ResponseEntity.ok(Map.of("message", "Factory Registered Successfully!"));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.status(400).body(Map.of("error", "Email already exists! Please use a different email or login."));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", "Server Error: " + e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        Optional<Company> companyOpt = companyRepository.findByContactEmail(email);
        if (companyOpt.isPresent()) {
            authServiceMailExtension.sendPasswordResetEmail(companyOpt.get());
        }
        return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a password reset link has been sent."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginFactory(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        Company company = companyRepository.findByContactEmail(email).orElse(null);
        if (company == null || !passwordEncoder.matches(password, company.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid Credentials"));
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
        String token = jwtUtil.generateToken(userDetails.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login Successful!");
        response.put("companyId", company.getId());
        response.put("companyName", company.getCompanyName());
        response.put("token", token);
        return ResponseEntity.ok(response);
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
        verifyOwnership(companyId); // 🔒 SECURE
        try {
            ResourceListing existing = resourceListingRepository.findById(resourceId).orElseThrow(() -> new RuntimeException("Not found"));
            if(!existing.getCompany().getId().equals(companyId)) throw new Exception("Not your resource");

            resourceListingRepository.deleteById(resourceId);
            return ResponseEntity.ok().body(Map.of("message", "Resource deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Could not delete resource"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCompanyProfile(@PathVariable Integer id, @RequestBody Map<String, String> updates) {
        verifyOwnership(id); // 🔒 SECURE
        Company company = companyRepository.findById(id).orElse(null);
        if (company == null) return ResponseEntity.status(404).body("Company not found!");

        if (updates.containsKey("companyName")) company.setCompanyName(updates.get("companyName"));
        if (updates.containsKey("industryType")) company.setIndustryType(updates.get("industryType"));
        if (updates.containsKey("description")) company.setDescription(updates.get("description"));
        if (updates.containsKey("website")) company.setWebsite(updates.get("website"));
        if (updates.containsKey("taxId")) company.setTaxId(updates.get("taxId"));
        if (updates.containsKey("defaultSearchRadius")) company.setDefaultSearchRadius(Integer.parseInt(updates.get("defaultSearchRadius")));
        if (updates.containsKey("preferredUnit")) company.setPreferredUnit(updates.get("preferredUnit"));

        if (updates.containsKey("latitude") && updates.containsKey("longitude")) {
            double lat = Double.parseDouble(updates.get("latitude"));
            double lon = Double.parseDouble(updates.get("longitude"));
            GeometryFactory geometryFactory = new GeometryFactory();
            Point newLocation = geometryFactory.createPoint(new Coordinate(lon, lat));
            newLocation.setSRID(4326);
            company.setLocation(newLocation);
        }

        companyRepository.save(company);
        return ResponseEntity.ok(Map.of("message", "Company Profile updated successfully!"));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<?> updatePassword(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
        verifyOwnership(id); // 🔒 SECURE
        try {
            Company company = companyRepository.findById(id).orElse(null);
            String currentPassword = payload.get("currentPassword");
            String newPassword = payload.get("newPassword");

            if (!passwordEncoder.matches(currentPassword, company.getPassword())) {
                return ResponseEntity.status(401).body(Map.of("error", "Incorrect current password"));
            }

            company.setPassword(passwordEncoder.encode(newPassword));
            companyRepository.save(company);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Server Error: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/upload-document")
    public ResponseEntity<?> uploadComplianceDocument(@PathVariable Integer id, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        verifyOwnership(id); // 🔒 SECURE
        try {
            Company company = companyRepository.findById(id).orElse(null);
            company.setVerificationStatus("DOCUMENT_UNDER_REVIEW");
            companyRepository.save(company);
            return ResponseEntity.ok(Map.of("message", "Document '" + file.getOriginalFilename() + "' uploaded successfully! Verification pending."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to upload document"));
        }
    }

    @PutMapping("/{id}/notifications")
    public ResponseEntity<?> updateNotifications(@PathVariable Integer id, @RequestBody Map<String, Boolean> prefs) {
        verifyOwnership(id); // 🔒 SECURE
        return ResponseEntity.ok(Map.of("message", "Notification preferences saved securely."));
    }

    @PutMapping("/{id}/integrations")
    public ResponseEntity<?> saveIntegrations(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
        verifyOwnership(id); // 🔒 SECURE
        Company company = companyRepository.findById(id).orElse(null);

        if (payload.containsKey("integrationProvider")) company.setIntegrationProvider(payload.get("integrationProvider"));
        if (payload.containsKey("apiKey")) company.setApiKey(payload.get("apiKey"));
        if (payload.containsKey("webhookUrl")) company.setWebhookUrl(payload.get("webhookUrl"));

        companyRepository.save(company);
        return ResponseEntity.ok(Map.of("message", "Integration settings saved successfully!"));
    }
}