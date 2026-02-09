package com.example.demo.docgen.controller;

import com.example.demo.docgen.model.DocumentGenerationRequest;
import com.example.demo.docgen.service.DocumentComposer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for DocumentController
 * Tests the controller's error handling and response formatting
 */
public class DocumentControllerIntegrationTest {

    private DocumentController controller;
    private DocumentComposer mockComposer;

    @BeforeEach
    public void setup() {
        mockComposer = mock(DocumentComposer.class);
        controller = new DocumentController(mockComposer);
    }

    @Test
    public void generate_withTemplateLoadingException_returnsNotFoundJson() {
        // Setup
        DocumentGenerationRequest request = DocumentGenerationRequest.builder()
                .namespace("common-templates")
                .templateId("nonexistent-template.yaml")
                .data(new HashMap<>())
                .build();

        com.example.demo.docgen.exception.TemplateLoadingException exception = 
            new com.example.demo.docgen.exception.TemplateLoadingException(
                "TEMPLATE_NOT_FOUND", 
                "Template not found for id 'nonexistent-template.yaml'"
            );
        
        when(mockComposer.generateDocument(any(DocumentGenerationRequest.class)))
            .thenThrow(exception);

        // Execute
        ResponseEntity<?> response = controller.generateDocument(request);

        // Verify
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("TEMPLATE_NOT_FOUND", body.get("code"));
        assertTrue(body.get("description").contains("Template not found"));
    }

    @Test
    public void generate_withUnresolvablePlaceholder_returnsBadRequestJson() {
        // Setup
        DocumentGenerationRequest request = DocumentGenerationRequest.builder()
                .namespace("common-templates")
                .templateId("template-with-placeholder.yaml")
                .data(new HashMap<>())
                .build();

        com.example.demo.docgen.exception.TemplateLoadingException exception = 
            new com.example.demo.docgen.exception.TemplateLoadingException(
                "UNRESOLVED_PLACEHOLDER", 
                "Unresolved placeholder 'data.missing' in template id: template-with-placeholder.yaml"
            );
        
        when(mockComposer.generateDocument(any(DocumentGenerationRequest.class)))
            .thenThrow(exception);

        // Execute
        ResponseEntity<?> response = controller.generateDocument(request);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("UNRESOLVED_PLACEHOLDER", body.get("code"));
        assertTrue(body.get("description").contains("Unresolved placeholder"));
    }
}
