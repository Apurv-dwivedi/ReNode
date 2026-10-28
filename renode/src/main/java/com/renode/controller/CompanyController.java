package com.renode.controller;

import com.renode.repository.CompanyRepository;
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
// YAHAN CORS POLICY UPDATE KI HAI TAHO KI 'DELETE' ALLOW HO JAYE
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

    // --- SECURE REGISTER ENDPOINT (With Password) ---
    @PostMapping("/register")
    public String registerNewFactory(@RequestBody Map<String, String> payload) {
        String name = payload.get("companyName");
        String industry = payload.get("industryType");
        String email = payload.get("contactEmail");
        String rawPassword = payload.get("password");
        double lat = Double.parseDouble(payload.get("latitude"));
        double lon = Double.parseDouble(payload.get("longitude"));

        // Password ko secure hash me convert karna
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Database me password ke sath insert call karna
        companyRepository.insertCompanyWithPassword(name, industry, email, encodedPassword, lat, lon);
        return "Factory Registered Successfully!";
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

            // 1. JWT Token Generate Karo
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
            String token = jwtUtil.generateToken(userDetails.getUsername());

            // 2. Token ko response me add karo
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

    // --- YEH NAYA DELETE API METHOD ADD KIYA HAI ---
    @DeleteMapping("/{companyId}/resources/{resourceId}")
    public ResponseEntity<?> deleteResource(@PathVariable Integer companyId, @PathVariable Integer resourceId) {
        try {
            resourceListingRepository.deleteById(resourceId);
            return ResponseEntity.ok().body("{\"message\": \"Resource deleted successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"Could not delete resource\"}");
        }
    }
}