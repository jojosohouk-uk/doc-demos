package com.example.demo.docgen.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AcroFormViewModelBuilder
 * Demonstrates clear, testable transformation logic - much better than JSONATA expressions
 */
public class AcroFormViewModelBuilderTest {

    private AcroFormViewModelBuilder builder;

    @BeforeEach
    public void setup() {
        builder = new AcroFormViewModelBuilder();
    }

    @Test
    public void testBuildViewModelWithPrimaryApplicant() {
        // Setup raw nested data
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> applicant = new HashMap<>();
        Map<String, Object> demographic = new HashMap<>();
        
        demographic.put("firstName", "John");
        demographic.put("lastName", "Doe");
        demographic.put("middleName", "Robert");
        demographic.put("dateOfBirth", "1990-01-15");
        demographic.put("ssn", "123-45-6789");
        demographic.put("gender", "M");
        demographic.put("email", "john@example.com");
        demographic.put("phone", "555-1234");
        
        applicant.put("type", "PRIMARY");
        applicant.put("demographic", demographic);
        
        data.put("applicants", Arrays.asList(applicant));
        data.put("applicationId", "APP-2026-001");
        data.put("applicationDate", "2026-02-11");
        data.put("applicationStatus", "ACTIVE");
        
        // Build ViewModel
        AcroFormViewModel viewModel = builder.build(data);
        
        // Verify primary applicant fields are properly set
        assertEquals("John Robert Doe", viewModel.getPrimaryFullName());
        assertEquals("John", viewModel.getPrimaryFirstName());
        assertEquals("Doe", viewModel.getPrimaryLastName());
        assertEquals("01/15/1990", viewModel.getPrimaryDateOfBirth());
        assertEquals("123-45-6789", viewModel.getPrimarySSN());
        assertEquals("M", viewModel.getPrimaryGender());
        assertEquals("john@example.com", viewModel.getPrimaryEmail());
        assertEquals("555-1234", viewModel.getPrimaryPhone());
        
        // Verify application info
        assertEquals("APP-2026-001", viewModel.getApplicationId());
        assertEquals("02/11/2026", viewModel.getApplicationDate());
        assertEquals("ACTIVE", viewModel.getApplicationStatus());
        assertTrue(viewModel.getIsActive());
    }

    @Test
    public void testBuildViewModelWithSpouse() {
        Map<String, Object> data = new HashMap<>();
        
        // Primary applicant
        Map<String, Object> primary = new HashMap<>();
        Map<String, Object> primaryDemo = new HashMap<>();
        primaryDemo.put("firstName", "John");
        primaryDemo.put("lastName", "Doe");
        primary.put("type", "PRIMARY");
        primary.put("demographic", primaryDemo);
        
        // Spouse applicant
        Map<String, Object> spouse = new HashMap<>();
        Map<String, Object> spouseDemo = new HashMap<>();
        spouseDemo.put("firstName", "Jane");
        spouseDemo.put("lastName", "Doe");
        spouseDemo.put("dateOfBirth", "1992-05-20");
        spouseDemo.put("ssn", "987-65-4321");
        spouse.put("type", "SPOUSE");
        spouse.put("demographic", spouseDemo);
        
        data.put("applicants", Arrays.asList(primary, spouse));
        
        AcroFormViewModel viewModel = builder.build(data);
        
        // Verify spouse info is properly transformed
        assertTrue(viewModel.getHasSpouse());
        assertEquals("Jane Doe", viewModel.getSpouseFullName());
        assertEquals("Jane", viewModel.getSpouseFirstName());
        assertEquals("Doe", viewModel.getSpouseLastName());
        assertEquals("05/20/1992", viewModel.getSpouseDateOfBirth());
        assertEquals("987-65-4321", viewModel.getSpouseSSN());
        
        // Verify computed family size
        assertEquals(2, viewModel.getFamilySize());
        assertTrue(viewModel.getIsFamilyApplication());
    }

    @Test
    public void testBuildViewModelWithDependents() {
        Map<String, Object> data = new HashMap<>();
        
        Map<String, Object> primary = new HashMap<>();
        Map<String, Object> primaryDemo = new HashMap<>();
        primaryDemo.put("firstName", "John");
        primaryDemo.put("lastName", "Doe");
        primary.put("type", "PRIMARY");
        primary.put("demographic", primaryDemo);
        
        // Child 1
        Map<String, Object> child1 = new HashMap<>();
        Map<String, Object> child1Demo = new HashMap<>();
        child1Demo.put("firstName", "Alice");
        child1.put("type", "DEPENDENT");
        child1.put("demographic", child1Demo);
        
        // Child 2
        Map<String, Object> child2 = new HashMap<>();
        Map<String, Object> child2Demo = new HashMap<>();
        child2Demo.put("firstName", "Bob");
        child2.put("type", "CHILD");
        child2.put("demographic", child2Demo);
        
        data.put("applicants", Arrays.asList(primary, child1, child2));
        
        AcroFormViewModel viewModel = builder.build(data);
        
        // Verify dependent info
        assertEquals(2, viewModel.getDependentCount());
        assertTrue(viewModel.getHasDependents());
        assertEquals("Alice, Bob", viewModel.getDependentNames());
        
        // Verify computed fields
        assertEquals(3, viewModel.getFamilySize());  // Primary + 2 dependents
        assertTrue(viewModel.getIsFamilyApplication());
    }

    @Test
    public void testBuildViewModelWithAddress() {
        Map<String, Object> data = new HashMap<>();
        
        Map<String, Object> primary = new HashMap<>();
        Map<String, Object> primaryDemo = new HashMap<>();
        primaryDemo.put("firstName", "John");
        primaryDemo.put("lastName", "Doe");
        primary.put("type", "PRIMARY");
        primary.put("demographic", primaryDemo);
        
        // Add address
        Map<String, Object> address = new HashMap<>();
        address.put("type", "HOME");
        address.put("street", "123 Main St");
        address.put("city", "Boston");
        address.put("state", "MA");
        address.put("zipCode", "02101");
        primary.put("addresses", Arrays.asList(address));
        
        data.put("applicants", Arrays.asList(primary));
        
        AcroFormViewModel viewModel = builder.build(data);
        
        // Verify address is flattened and formatted
        assertEquals("123 Main St, Boston, MA 02101", viewModel.getPrimaryAddress());
        assertEquals("Boston", viewModel.getPrimaryCity());
        assertEquals("MA", viewModel.getPrimaryState());
        assertEquals("02101", viewModel.getPrimaryZipCode());
    }

    @Test
    public void testBuildViewModelWithNullValues() {
        // Test robustness: should handle missing/null values gracefully
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> primary = new HashMap<>();
        primary.put("type", "PRIMARY");
        // Note: no demographic data
        
        data.put("applicants", Arrays.asList(primary));
        
        // Should not throw exception
        AcroFormViewModel viewModel = builder.build(data);
        
        // Should have empty/default values
        assertEquals("", viewModel.getPrimaryFirstName());
        assertEquals("", viewModel.getPrimaryFullName());
        assertFalse(viewModel.getHasSpouse());
        assertEquals(0, viewModel.getDependentCount());
    }

    @Test
    public void testComputedFieldsNoSpouseNoDependents() {
        Map<String, Object> data = new HashMap<>();
        
        Map<String, Object> primary = new HashMap<>();
        Map<String, Object> primaryDemo = new HashMap<>();
        primaryDemo.put("firstName", "John");
        primaryDemo.put("lastName", "Doe");
        primary.put("type", "PRIMARY");
        primary.put("demographic", primaryDemo);
        
        data.put("applicants", Arrays.asList(primary));
        
        AcroFormViewModel viewModel = builder.build(data);
        
        // Verify computed fields
        assertFalse(viewModel.getHasSpouse());
        assertFalse(viewModel.getHasDependents());
        assertEquals(1, viewModel.getFamilySize());  // Just primary
        assertFalse(viewModel.getIsFamilyApplication());
    }

    @Test
    public void testCompleteScenarioWithAllApplicants() {
        // Comprehensive test with all applicant types
        Map<String, Object> data = new HashMap<>();
        
        // Primary
        Map<String, Object> primary = new HashMap<>();
        Map<String, Object> primaryDemo = new HashMap<>();
        primaryDemo.put("firstName", "John");
        primaryDemo.put("lastName", "Doe");
        primaryDemo.put("dateOfBirth", "1960-01-15");  // Will be 65+ years old
        primary.put("type", "PRIMARY");
        primary.put("demographic", primaryDemo);
        
        // Spouse
        Map<String, Object> spouse = new HashMap<>();
        Map<String, Object> spouseDemo = new HashMap<>();
        spouseDemo.put("firstName", "Jane");
        spouseDemo.put("lastName", "Doe");
        spouse.put("type", "SPOUSE");
        spouse.put("demographic", spouseDemo);
        
        // Dependents
        Map<String, Object> child = new HashMap<>();
        Map<String, Object> childDemo = new HashMap<>();
        childDemo.put("firstName", "Alice");
        child.put("type", "DEPENDENT");
        child.put("demographic", childDemo);
        
        data.put("applicants", Arrays.asList(primary, spouse, child));
        data.put("applicationId", "APP-2026-001");
        data.put("applicationStatus", "ACTIVE");
        
        AcroFormViewModel viewModel = builder.build(data);
        
        // All flags should be set correctly
        assertEquals("John Doe", viewModel.getPrimaryFullName());
        assertEquals("Jane Doe", viewModel.getSpouseFullName());
        assertEquals(1, viewModel.getDependentCount());
        assertTrue(viewModel.getIsActive());
        assertTrue(viewModel.getHasSpouse());
        assertTrue(viewModel.getHasDependents());
        assertTrue(viewModel.getIsSenior());  // Born in 1960
        assertEquals(3, viewModel.getFamilySize());
        assertTrue(viewModel.getIsFamilyApplication());
    }
}
