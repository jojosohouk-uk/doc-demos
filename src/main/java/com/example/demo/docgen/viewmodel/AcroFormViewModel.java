package com.example.demo.docgen.viewmodel;

import lombok.Builder;
import lombok.Data;

/**
 * ViewModel for AcroForm PDF field population.
 * Demonstrates how ViewModels can flatten, transform, and compute data
 * into form-ready fields.
 * 
 * This ViewModel is particularly useful when:
 * - You need to flatten nested data structures
 * - You need to concatenate multiple fields (full names, addresses)
 * - You need to format data (dates, numbers, enums)
 * - You need to compute derived fields (flags, aggregations)
 * - You want to avoid complex JSONATA expressions
 */
@Data
@Builder
public class AcroFormViewModel {
    
    // ============ PRIMARY APPLICANT FIELDS ============
    
    /**
     * Full name: concatenated from firstName + middleName (if present) + lastName
     * Example: "John Robert Doe" or "Jane Smith"
     * Computed from nested demographic data
     */
    private String primaryFullName;
    
    /**
     * Primary applicant first name
     */
    private String primaryFirstName;
    
    /**
     * Primary applicant last name
     */
    private String primaryLastName;
    
    /**
     * Formatted date of birth (MM/dd/yyyy)
     * Null-safe with error handling
     */
    private String primaryDateOfBirth;
    
    /**
     * Primary applicant SSN (formatted or raw)
     */
    private String primarySSN;
    
    /**
     * Primary applicant gender
     */
    private String primaryGender;
    
    /**
     * Primary applicant email
     */
    private String primaryEmail;
    
    /**
     * Primary applicant phone
     */
    private String primaryPhone;
    
    /**
     * Formatted address: "123 Main St, Springfield, IL 62701"
     * Flattened from nested address object
     */
    private String primaryAddress;
    
    /**
     * City of residence
     */
    private String primaryCity;
    
    /**
     * State of residence
     */
    private String primaryState;
    
    /**
     * Zip code
     */
    private String primaryZipCode;
    
    // ============ SPOUSE/SECONDARY APPLICANT FIELDS ============
    
    /**
     * Flag indicating if spouse information should be shown
     * Computed: true if spouse name is present
     */
    private Boolean hasSpouse;
    
    /**
     * Full name of spouse (concatenated)
     */
    private String spouseFullName;
    
    /**
     * Spouse first name
     */
    private String spouseFirstName;
    
    /**
     * Spouse last name
     */
    private String spouseLastName;
    
    /**
     * Formatted spouse date of birth (MM/dd/yyyy)
     */
    private String spouseDateOfBirth;
    
    /**
     * Spouse SSN
     */
    private String spouseSSN;
    
    /**
     * Formatted spouse address
     */
    private String spouseAddress;
    
    // ============ DEPENDENT/CHILDREN FIELDS ============
    
    /**
     * Number of dependents/children
     * Computed: count of children in the applicants list
     */
    private Integer dependentCount;
    
    /**
     * Flag indicating if dependents exist
     * Computed: dependentCount > 0
     */
    private Boolean hasDependents;
    
    /**
     * Comma-separated list of dependent names
     * Computed: concatenate all child first names
     */
    private String dependentNames;
    
    // ============ APPLICATION INFO ============
    
    /**
     * Application or document ID
     */
    private String applicationId;
    
    /**
     * Application date (formatted)
     */
    private String applicationDate;
    
    /**
     * Application status (PENDING, ACTIVE, etc.)
     */
    private String applicationStatus;
    
    /**
     * Flag indicating if application is active
     * Computed: status equals "ACTIVE"
     */
    private Boolean isActive;
    
    // ============ COMPUTED/DERIVED FIELDS ============
    
    /**
     * Family size: 1 (primary) + 1 (if spouse) + dependentCount
     * Computed field useful for conditional form sections
     */
    private Integer familySize;
    
    /**
     * Indicates if this is a family application
     * Computed: familySize > 1
     */
    private Boolean isFamilyApplication;
    
    /**
     * Indicates if primary applicant is senior (age >= 65)
     * Computed based on DOB
     */
    private Boolean isSenior;
}
