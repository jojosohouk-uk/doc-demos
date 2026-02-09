package com.example.demo.docgen.renderer;

import com.example.demo.docgen.core.RenderContext;
import com.example.demo.docgen.exception.ResourceLoadingException;
import com.example.demo.docgen.mapper.JsonPathMappingStrategy;
import com.example.demo.docgen.model.PageSection;
import com.example.demo.docgen.model.SectionType;
import com.example.demo.docgen.service.TemplateLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ResourceLoadingException error handling in AcroFormRenderer
 * Tests that missing PDF/resource files are properly caught and converted to ResourceLoadingException
 */
public class ResourceLoadingExceptionTest {

    private AcroFormRenderer renderer;
    private TemplateLoader templateLoader;

    @BeforeEach
    public void setup() {
        templateLoader = Mockito.mock(TemplateLoader.class);
        renderer = new AcroFormRenderer(
                Arrays.asList(new JsonPathMappingStrategy()),
                templateLoader
        );
    }

    @Test
    @DisplayName("Should throw ResourceLoadingException when PDF resource is not found")
    public void testMissingPdfResource() throws IOException {
        // Setup: Mock TemplateLoader.getResourceBytes to throw IOException for missing PDF
        when(templateLoader.getResourceBytes(anyString()))
                .thenThrow(new com.example.demo.docgen.exception.TemplateLoadingException(
                        "RESOURCE_READ_ERROR",
                        "File not found: templates/missing-form.pdf",
                        new IOException("File not found: templates/missing-form.pdf")
                ));

        // Create a section that references the missing PDF
        PageSection section = PageSection.builder()
                .sectionId("test-section")
                .type(SectionType.ACROFORM)
                .templatePath("missing-form.pdf")
                .build();

        RenderContext context = new RenderContext(null, new HashMap<>());

        // Act & Assert: Should throw ResourceLoadingException with RESOURCE_NOT_FOUND code
        ResourceLoadingException exception = assertThrows(
                ResourceLoadingException.class,
                () -> renderer.render(section, context),
                "Should throw ResourceLoadingException for missing PDF"
        );

        assertEquals("RESOURCE_NOT_FOUND", exception.getCode(),
                "Error code should be RESOURCE_NOT_FOUND");
        assertTrue(exception.getDescription().contains("missing-form.pdf"),
                "Error description should mention the missing file");
        assertNotNull(exception.getCause(),
                "Exception should have IOException as cause");
        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    @DisplayName("Should throw ResourceLoadingException with proper description for missing resource")
    public void testResourceLoadingExceptionMessage() throws IOException {
        // Setup: Mock TemplateLoader to throw IOException
        IOException ioException = new IOException("File not found");
        when(templateLoader.getResourceBytes(anyString()))
                .thenThrow(new com.example.demo.docgen.exception.TemplateLoadingException(
                        "RESOURCE_READ_ERROR",
                        "File not found",
                        ioException
                ));

        // Create a section
        PageSection section = PageSection.builder()
                .sectionId("enrollment-form")
                .type(SectionType.ACROFORM)
                .templatePath("applicant-form.pdf")
                .build();

        RenderContext context = new RenderContext(null, new HashMap<>());

        // Act & Assert
        ResourceLoadingException exception = assertThrows(
                ResourceLoadingException.class,
                () -> renderer.render(section, context)
        );

        // Verify exception details
        assertEquals("RESOURCE_NOT_FOUND", exception.getCode());
        assertTrue(exception.getDescription().contains("applicant-form.pdf"),
                "Description should include resource filename");
        assertTrue(exception.getDescription().contains("not found") || 
                   exception.getDescription().contains("Failed to load"),
                "Description should describe the resource loading failure");
    }

    @Test
    @DisplayName("Should preserve IOException cause for debugging")
    public void testResourceLoadingExceptionCause() throws IOException {
        // Setup: TemplateLoader throws IOException which should be converted to ResourceLoadingException
        IOException originalCause = new IOException("Connection timeout loading resource");
        when(templateLoader.getResourceBytes(anyString()))
                .thenThrow(new com.example.demo.docgen.exception.TemplateLoadingException(
                        "RESOURCE_READ_ERROR",
                        "Connection timeout loading resource",
                        originalCause
                ));

        PageSection section = PageSection.builder()
                .sectionId("test")
                .type(SectionType.ACROFORM)
                .templatePath("some-form.pdf")
                .build();

        RenderContext context = new RenderContext(null, new HashMap<>());

        // Act
        ResourceLoadingException exception = assertThrows(
                ResourceLoadingException.class,
                () -> renderer.render(section, context)
        );

        // Assert: Verify exception chain is preserved
        assertEquals("RESOURCE_NOT_FOUND", exception.getCode());
        assertEquals(originalCause, exception.getCause(),
                "Original IOException should be preserved as cause");
    }

    @Test
    @DisplayName("Should include resource path in error description for debugging")
    public void testResourceExceptionIncludesResolvedPath() throws IOException {
        // Setup
        IOException ioException = new IOException("Access denied");
        when(templateLoader.getResourceBytes(anyString()))
                .thenThrow(new com.example.demo.docgen.exception.TemplateLoadingException(
                        "RESOURCE_READ_ERROR",
                        "Access denied",
                        ioException
                ));

        PageSection section = PageSection.builder()
                .sectionId("invoice-form")
                .type(SectionType.ACROFORM)
                .templatePath("forms/invoice-acroform.pdf")
                .build();

        RenderContext context = new RenderContext(null, new HashMap<>());

        // Act & Assert
        ResourceLoadingException exception = assertThrows(
                ResourceLoadingException.class,
                () -> renderer.render(section, context)
        );

        // Description should help developers debug where the file was attempted to be loaded from
        String description = exception.getDescription();
        assertNotNull(description);
        assertFalse(description.isEmpty(),
                "Description should provide helpful debugging information");
        assertTrue(description.contains("invoice-acroform.pdf") || description.contains("forms/invoice-acroform"),
                "Description should mention which resource failed to load");
    }

    @Test
    @DisplayName("Should throw ResourceLoadingException from render() method")
    public void testRenderPropagatesResourceLoadingException() throws IOException {
        // Setup: Resource not found
        when(templateLoader.getResourceBytes(anyString()))
                .thenThrow(new com.example.demo.docgen.exception.TemplateLoadingException(
                        "RESOURCE_READ_ERROR",
                        "Resource unavailable",
                        new IOException("Resource unavailable")
                ));

        PageSection section = PageSection.builder()
                .sectionId("test")
                .type(SectionType.ACROFORM)
                .templatePath("unavailable.pdf")
                .build();

        RenderContext context = new RenderContext(null, new HashMap<>());

        // Act & Assert: Verify ResourceLoadingException is thrown and not wrapped
        assertThrows(
                ResourceLoadingException.class,
                () -> renderer.render(section, context),
                "render() should throw ResourceLoadingException for missing resources"
        );
    }
}
