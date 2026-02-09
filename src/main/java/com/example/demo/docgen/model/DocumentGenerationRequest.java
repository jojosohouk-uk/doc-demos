package com.example.demo.docgen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Request object for document generation containing template reference and runtime data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentGenerationRequest {
    /**
     * Namespace (tenant) where the template resides (e.g., "tenant-a", "common-templates")
     * If null, defaults to "common-templates"
     */
    private String namespace;
    
    /**
     * Template ID to use for generation (relative to namespace/templates folder)
     */
    private String templateId;
    
    /**
     * Runtime data to merge with the template
     */
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();
    
    /**
     * Optional generation options
     */
    @Builder.Default
    private Map<String, Object> options = new HashMap<>();
}
