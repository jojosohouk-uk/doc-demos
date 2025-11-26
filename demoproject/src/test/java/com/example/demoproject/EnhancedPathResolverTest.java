package com.example.demoproject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class EnhancedPathResolverTest {
    @Test
void testNestedArrayFilteringWithApplicationStructure() {
    Map<String, Object> data = Map.of(
        "application", Map.of(
            "id", "APP-2025-001",
            "applicants", List.of(
                Map.of(
                    "id", "A1",
                    "relationship", "primary",
                    "firstName", "John",
                    "addresses", List.of(
                        Map.of("type", "home", "street", "123 Main St", "city", "NYC", "zip", "10001"),
                        Map.of("type", "billing", "street", "456 Business Ave", "city", "NYC", "zip", "10002"),
                        Map.of("type", "mailing", "street", "123 Main St", "city", "NYC", "zip", "10001")
                    )
                ),
                Map.of(
                    "id", "A2",
                    "relationship", "dependent",
                    "firstName", "Alice",
                    "addresses", List.of(
                        Map.of("type", "home", "street", "123 Main St", "city", "NYC", "zip", "10001")
                    )
                )
            )
        )
    );
    
    // Test nested array filtering
    Object primaryBillingStreet = EnhancedPathResolver.read(data, 
        "application.applicants[relationship='primary'].addresses[type='billing'].street");
   // assertEquals("456 Business Ave", primaryBillingStreet);
    
    Object allHomeStreets = EnhancedPathResolver.read(data, 
        "application.applicants.addresses[type='home'].street");
    assertEquals(List.of("123 Main St", "123 Main St"), allHomeStreets);
    
    Object primaryHomeZip = EnhancedPathResolver.read(data, 
        "application.applicants[relationship='primary'].addresses[type='home'].zip");
    assertEquals("10001", primaryHomeZip);
    
    // Test filtering by multiple conditions
    Object nycMailingAddresses = EnhancedPathResolver.read(data, 
        "application.applicants.addresses[city='NYC' and zip='10001' and type='mailing'].street");
    assertEquals("123 Main St", nycMailingAddresses);
}

@Test
void testNestedArrayFiltering() {
    Map<String, Object> data = Map.of(
        "applicants", List.of(
            Map.of(
                "firstName", "John",
                "addresses", List.of(
                    Map.of("type", "home", "street", "123 Main St"),
                    Map.of("type", "billing", "street", "456 Business Ave")
                )
            )
        )
    );
    
    // Test nested array filtering
    Object homeStreet = EnhancedPathResolver.read(data, "applicants.addresses[type='home'].street");
    assertEquals("123 Main St", homeStreet);
    
    Object billingStreet = EnhancedPathResolver.read(data, "applicants.addresses[type='billing'].street");
    assertEquals("456 Business Ave", billingStreet);
    
    // Test filtering non-existent type
    Object workStreet = EnhancedPathResolver.read(data, "applicants.addresses[type='work'].street");
    assertNull(workStreet);
}

}
