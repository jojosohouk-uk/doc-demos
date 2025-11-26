package com.example.demoproject;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;


import java.util.regex.Pattern;

public class EnhancedPathResolver {
    
    // Pattern to match array filters: [field='value'] or [field=123]
    private static final Pattern ARRAY_FILTER_PATTERN = 
        Pattern.compile("\\[(.*?)='(.*?)'\\]|\\[(.*?)=(\\d+)\\]");
    
    // Pattern to match simple dots: applicants.addresses.city
    private static final Pattern SIMPLE_DOT_PATTERN = 
        Pattern.compile("([a-zA-Z0-9_]+)\\.");

    public static Object read(Object jsonContext, String simplePath) {
        if (simplePath == null || simplePath.trim().isEmpty()) {
            return null;
        }
        
        try {
            String jsonPath = convertToValidJsonPath(simplePath);
            return JsonPath.read(jsonContext, jsonPath);
        } catch (PathNotFoundException | IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Converts simple dot notation to JsonPath with array filtering support
     * Examples:
     *   "applicants.addresses.type" → "$.applicants[*].addresses[*].type"
     *   "applicants[relationship='primary'].firstName" → "$.applicants[?(@.relationship == 'primary')].firstName"
     *   "applicants.addresses[type='home'].street" → "$.applicants[*].addresses[?(@.type == 'home')].street"
     */
    public static String convertToValidJsonPath(String simplePath) {
        String path = simplePath.trim();
        
        // Handle root-level array filters: applicants[relationship='primary']
        path = ARRAY_FILTER_PATTERN.matcher(path).replaceAll(match -> {
            if (match.group(1) != null) {
                // String value: [field='value']
                return String.format("[?(@.%s == '%s')]", match.group(1), match.group(2));
            } else {
                // Numeric value: [field=123]
                return String.format("[?(@.%s == %s)]", match.group(3), match.group(4));
            }
        });
        
        // Handle nested array filters: applicants.addresses[type='home']
        path = path.replaceAll("\\.([a-zA-Z0-9_]+)\\[", "[*].$1[");
        
        // Convert remaining dots to JsonPath dots
        path = path.replace(".", ".");
        
        // Prepend root selector if not present
        if (!path.startsWith("$")) {
            path = "$." + path;
        }
        
        // Handle array wildcards: applicants[].addresses[] → applicants[*].addresses[*]
        path = path.replace("[]", "[*]");
        
        return path;
    }
}