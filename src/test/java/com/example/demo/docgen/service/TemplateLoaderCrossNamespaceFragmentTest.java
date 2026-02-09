package com.example.demo.docgen.service;

import com.example.demo.docgen.model.DocumentTemplate;
import com.example.demo.docgen.model.PageSection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test to verify cross-namespace fragment inclusion using the "common:" prefix.
 */
public class TemplateLoaderCrossNamespaceFragmentTest {

    private final TemplateLoader templateLoader = new TemplateLoader(new NamespaceResolver());

    @Test
    public void testCommonPrefixedFragmentIsIncluded() {
        DocumentTemplate template = templateLoader.loadTemplate("tenant-a", "template-with-common-fragment.yaml", java.util.Collections.emptyMap());

        assertNotNull(template, "Template should be loaded");

        // Should include both main section and footer section from common fragment
        boolean hasMain = template.getSections().stream().anyMatch(s -> "main".equals(s.getSectionId()));
        boolean hasFooter = template.getSections().stream().anyMatch(s -> "footer".equals(s.getSectionId()));

        assertTrue(hasMain, "Main section should be present");
        assertTrue(hasFooter, "Footer section from common fragment should be included");
    }
}
