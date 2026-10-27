package com.renode.dto;

public class MatchResponse {
    private String requirementCategory;
    private double requiredQuantity;
    private String supplierCompanyName;
    private String supplierEmail;
    private double availableQuantity;

    // Constructor
    public MatchResponse(String requirementCategory, double requiredQuantity, String supplierCompanyName, String supplierEmail, double availableQuantity) {
        this.requirementCategory = requirementCategory;
        this.requiredQuantity = requiredQuantity;
        this.supplierCompanyName = supplierCompanyName;
        this.supplierEmail = supplierEmail;
        this.availableQuantity = availableQuantity;
    }

    // Getters
    public String getRequirementCategory() { return requirementCategory; }
    public double getRequiredQuantity() { return requiredQuantity; }
    public String getSupplierCompanyName() { return supplierCompanyName; }
    public String getSupplierEmail() { return supplierEmail; }
    public double getAvailableQuantity() { return availableQuantity; }
}