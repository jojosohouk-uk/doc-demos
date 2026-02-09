package com.example.demo.docgen.service;

import com.example.demo.docgen.model.DocumentTemplate;
import com.example.demo.docgen.model.PageSection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TemplateLoader
 * Tests template loading, inheritance, and merging
 */
public class TemplateLoaderTest {

    private final TemplateLoader templateLoader = new TemplateLoader(new NamespaceResolver());
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Test
    public void testTemplateInheritanceAndMerging() {
        // Load the child template which inherits from base
        DocumentTemplate template = templateLoader.loadTemplate("test-inheritance-child.yaml");

        assertNotNull(template);
        assertEquals("test-inheritance-child", template.getTemplateId());
        
        // Should have 2 sections (section1 merged, section2 excluded, section3 added)
        assertEquals(2, template.getSections().size());

        // Verify section1 merging
        Optional<PageSection> section1Opt = template.getSections().stream()
                .filter(s -> s.getSectionId().equals("section1"))
                .findFirst();
        
        assertTrue(section1Opt.isPresent(), "section1 should exist in merged template");
        PageSection section1 = section1Opt.get();
        
        assertEquals("base-path.pdf", section1.getTemplatePath()); // From base
        assertEquals("$.child.field1", section1.getFieldMappings().get("field1")); // Overridden by child
        assertEquals("$.child.field2", section1.getFieldMappings().get("field2")); // Added by child

        // Verify section2 exclusion
        assertFalse(template.getSections().stream().anyMatch(s -> s.getSectionId().equals("section2")));

        // Verify section3 addition
        assertTrue(template.getSections().stream().anyMatch(s -> s.getSectionId().equals("section3")));
    }

    @Test
    public void testTemplateLoading() throws IOException {
        // Simple test to verify we can load a template from classpath
        DocumentTemplate template = templateLoader.loadTemplate("test-inheritance-base.yaml");
        
        assertNotNull(template);
        assertEquals("test-inheritance-base", template.getTemplateId());
        assertFalse(template.getSections().isEmpty());
    }
}

