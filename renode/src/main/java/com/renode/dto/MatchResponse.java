package com.renode.dto;

import com.renode.entity.Company;
import lombok.Data;

@Data
public class MatchResponse {
    private Company matchedCompany;
    private Integer matchedResourceId;
    private String matchedResourceType;
    private String matchedCategory;
    private Double matchedQuantity;
    private double distanceKm;
    private int matchScore;
}