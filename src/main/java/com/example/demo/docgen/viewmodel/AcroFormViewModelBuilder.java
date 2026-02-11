package com.example.demo.docgen.viewmodel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builder for AcroFormViewModel.
 * 
 * This demonstrates how to use ViewModels to handle complex data transformations
 * in a clear, maintainable, debuggable way - much better than complex JSONATA expressions.
 * 
 * Benefits over JSONATA:
 * - Type-safe: compile-time checking instead of runtime string parsing
 * - Debuggable: step through with debugger, full stack traces
 * - Testable: unit test each transformation independently
 * - Maintainable: clear Java code with comments, logging, error handling
 * - Reusable: same builder for multiple AcroForm sections or templates
 * - IDE Support: autocomplete, refactoring, navigation
 */
@Slf4j
@Component
public class AcroFormViewModelBuilder implements ViewModelBuilder<AcroFormViewModel> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final int SENIOR_AGE_THRESHOLD = 65;

    @Override
    @SuppressWarnings("unchecked")
    public AcroFormViewModel build(Map<String, Object> rawData) {
        log.debug("Building AcroFormViewModel from raw data");
        
        try {
            // Extract application info
            String applicationId = getStringValue(rawData, "applicationId", "");
            String applicationDate = formatDate(getStringValue(rawData, "applicationDate", ""));
            String applicationStatus = getStringValue(rawData, "applicationStatus", "");
            Boolean isActive = "ACTIVE".equalsIgnoreCase(applicationStatus);

            // Extract applicants list
            List<Map<String, Object>> applicants = (List<Map<String, Object>>) rawData.get("applicants");
            if (applicants == null) {
                applicants = new ArrayList<>();
            }

            // Extract PRIMARY applicant (first one or find by type)
            Map<String, Object> primaryApplicant = applicants.stream()
                .filter(a -> isPrimaryApplicant(a))
                .findFirst()
                .orElse(applicants.isEmpty() ? null : applicants.get(0));

            // Extract SPOUSE/secondary applicant
            Map<String, Object> spouseApplicant = applicants.stream()
                .filter(a -> isSpouseApplicant(a))
                .findFirst()
                .orElse(null);

            // Extract DEPENDENT/CHILD applicants
            List<Map<String, Object>> dependents = applicants.stream()
                .filter(a -> isDependentApplicant(a))
                .collect(Collectors.toList());

            // ============ BUILD PRIMARY APPLICANT FIELDS ============
            String primaryFirstName = "";
            String primaryLastName = "";
            String primaryFullName = "";
            String primaryDateOfBirth = "";
            String primarySSN = "";
            String primaryGender = "";
            String primaryEmail = "";
            String primaryPhone = "";
            String primaryAddress = "";
            String primaryCity = "";
            String primaryState = "";
            String primaryZipCode = "";
            Boolean isSenior = false;

            if (primaryApplicant != null) {
                Map<String, Object> demographic = (Map<String, Object>) primaryApplicant.get("demographic");
                if (demographic != null) {
                    primaryFirstName = getStringValue(demographic, "firstName", "");
                    primaryLastName = getStringValue(demographic, "lastName", "");
                    String middleName = getStringValue(demographic, "middleName", "");
                    
                    // Concatenate full name
                    primaryFullName = middleName.isEmpty()
                        ? (primaryFirstName + " " + primaryLastName).trim()
                        : (primaryFirstName + " " + middleName + " " + primaryLastName).trim();
                    
                    primaryDateOfBirth = formatDate(getStringValue(demographic, "dateOfBirth", ""));
                    primarySSN = getStringValue(demographic, "ssn", "");
                    primaryGender = getStringValue(demographic, "gender", "");
                    primaryEmail = getStringValue(demographic, "email", "");
                    primaryPhone = getStringValue(demographic, "phone", "");
                    
                    // Check if senior (65+)
                    isSenior = calculateAge(getStringValue(demographic, "dateOfBirth", "")) >= SENIOR_AGE_THRESHOLD;
                }

                // Extract address - flatten nested structure
                List<Map<String, Object>> addresses = (List<Map<String, Object>>) primaryApplicant.get("addresses");
                if (addresses != null && !addresses.isEmpty()) {
                    Map<String, Object> homeAddress = addresses.stream()
                        .filter(a -> "HOME".equalsIgnoreCase((String) a.get("type")))
                        .findFirst()
                        .orElse(addresses.get(0));
                    
                    primaryAddress = formatAddress(homeAddress);
                    primaryCity = getStringValue(homeAddress, "city", "");
                    primaryState = getStringValue(homeAddress, "state", "");
                    primaryZipCode = getStringValue(homeAddress, "zipCode", "");
                }
            }

            // ============ BUILD SPOUSE FIELDS ============
            Boolean hasSpouse = spouseApplicant != null;
            String spouseFirstName = "";
            String spouseLastName = "";
            String spouseFullName = "";
            String spouseDateOfBirth = "";
            String spouseSSN = "";
            String spouseAddress = "";

            if (hasSpouse) {
                Map<String, Object> demographic = (Map<String, Object>) spouseApplicant.get("demographic");
                if (demographic != null) {
                    spouseFirstName = getStringValue(demographic, "firstName", "");
                    spouseLastName = getStringValue(demographic, "lastName", "");
                    String middleName = getStringValue(demographic, "middleName", "");
                    spouseFullName = middleName.isEmpty()
                        ? (spouseFirstName + " " + spouseLastName).trim()
                        : (spouseFirstName + " " + middleName + " " + spouseLastName).trim();
                    
                    spouseDateOfBirth = formatDate(getStringValue(demographic, "dateOfBirth", ""));
                    spouseSSN = getStringValue(demographic, "ssn", "");
                }

                List<Map<String, Object>> addresses = (List<Map<String, Object>>) spouseApplicant.get("addresses");
                if (addresses != null && !addresses.isEmpty()) {
                    spouseAddress = formatAddress(addresses.get(0));
                }
            }

            // ============ BUILD DEPENDENT FIELDS ============
            Integer dependentCount = dependents.size();
            Boolean hasDependents = dependentCount > 0;
            String dependentNames = dependents.stream()
                .map(d -> {
                    Map<String, Object> demo = (Map<String, Object>) d.get("demographic");
                    return demo != null ? getStringValue(demo, "firstName", "") : "";
                })
                .filter(name -> !name.isEmpty())
                .collect(Collectors.joining(", "));

            // ============ COMPUTE DERIVED FIELDS ============
            Integer familySize = 1 + (hasSpouse ? 1 : 0) + dependentCount;
            Boolean isFamilyApplication = familySize > 1;

            log.debug("Successfully built AcroFormViewModel: primaryName={}, familySize={}, dependentCount={}",
                primaryFullName, familySize, dependentCount);

            // ============ BUILD AND RETURN VIEWMODEL ============
            return AcroFormViewModel.builder()
                // Primary applicant
                .primaryFullName(primaryFullName)
                .primaryFirstName(primaryFirstName)
                .primaryLastName(primaryLastName)
                .primaryDateOfBirth(primaryDateOfBirth)
                .primarySSN(primarySSN)
                .primaryGender(primaryGender)
                .primaryEmail(primaryEmail)
                .primaryPhone(primaryPhone)
                .primaryAddress(primaryAddress)
                .primaryCity(primaryCity)
                .primaryState(primaryState)
                .primaryZipCode(primaryZipCode)
                
                // Spouse
                .hasSpouse(hasSpouse)
                .spouseFullName(spouseFullName)
                .spouseFirstName(spouseFirstName)
                .spouseLastName(spouseLastName)
                .spouseDateOfBirth(spouseDateOfBirth)
                .spouseSSN(spouseSSN)
                .spouseAddress(spouseAddress)
                
                // Dependents
                .dependentCount(dependentCount)
                .hasDependents(hasDependents)
                .dependentNames(dependentNames)
                
                // Application info
                .applicationId(applicationId)
                .applicationDate(applicationDate)
                .applicationStatus(applicationStatus)
                .isActive(isActive)
                
                // Computed fields
                .familySize(familySize)
                .isFamilyApplication(isFamilyApplication)
                .isSenior(isSenior)
                
                .build();
                
        } catch (Exception e) {
            log.error("Error building AcroFormViewModel", e);
            // Return minimal ViewModel on error instead of crashing
            return AcroFormViewModel.builder().build();
        }
    }

    // ============ HELPER METHODS ============

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "";
        }
        try {
            LocalDate date = LocalDate.parse(dateString);
            return date.format(DATE_FORMATTER);
        } catch (Exception e) {
            log.debug("Could not format date: {}", dateString);
            return dateString;
        }
    }

    private int calculateAge(String dateOfBirthString) {
        if (dateOfBirthString == null || dateOfBirthString.isEmpty()) {
            return 0;
        }
        try {
            LocalDate dob = LocalDate.parse(dateOfBirthString);
            return (int) ChronoUnit.YEARS.between(dob, LocalDate.now());
        } catch (Exception e) {
            log.debug("Could not calculate age from: {}", dateOfBirthString);
            return 0;
        }
    }

    private String formatAddress(Map<String, Object> address) {
        if (address == null) {
            return "";
        }
        String street = getStringValue(address, "street", "");
        String city = getStringValue(address, "city", "");
        String state = getStringValue(address, "state", "");
        String zipCode = getStringValue(address, "zipCode", "");
        
        return String.format("%s, %s, %s %s",
            street, city, state, zipCode).replaceAll(",\\s*,", ",").trim();
    }

    private boolean isPrimaryApplicant(Map<String, Object> applicant) {
        String type = (String) applicant.get("type");
        return "PRIMARY".equalsIgnoreCase(type);
    }

    private boolean isSpouseApplicant(Map<String, Object> applicant) {
        String type = (String) applicant.get("type");
        return "SPOUSE".equalsIgnoreCase(type) || "SECONDARY".equalsIgnoreCase(type);
    }

    private boolean isDependentApplicant(Map<String, Object> applicant) {
        String type = (String) applicant.get("type");
        return "DEPENDENT".equalsIgnoreCase(type) || "CHILD".equalsIgnoreCase(type);
    }
}
