package com.example.demo.docgen.service;

import com.example.demo.docgen.model.DocumentTemplate;
import com.example.demo.docgen.model.PageSection;
import com.example.demo.docgen.model.SectionType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for placeholder interpolation inside template fields and
 * behavior when a child template supplies a templatePath while the base omits it.
 */
public class TemplateLoaderInterpolationTest {

    private final TemplateLoader templateLoader = new TemplateLoader(new NamespaceResolver());

    @Test
    public void testInterpolateTemplateFieldsReplacesPlaceholdersInTemplatePath() {
        DocumentTemplate tmp = new DocumentTemplate();
        PageSection sec = PageSection.builder()
                .sectionId("s1")
                .type(SectionType.ACROFORM)
                .templatePath("forms/${formType}.pdf")
                .order(1)
                .build();
        tmp.setSections(List.of(sec));

        Map<String, Object> vars = new HashMap<>();
        vars.put("formType", "applicant-form");

        // Perform interpolation
        templateLoader.interpolateTemplateFields(tmp, vars);

        assertEquals("forms/applicant-form.pdf", tmp.getSections().get(0).getTemplatePath());
    }

    @Test
    public void testChildProvidesTemplatePathWhenBaseOmits() {
        // Load child template which inherits from base that lacks templatePath
        DocumentTemplate template = templateLoader.loadTemplate("child-with-path.yaml");

        assertNotNull(template, "Template should be loaded");

        // Find section sec1
        PageSection section = template.getSections().stream()
                .filter(s -> "sec1".equals(s.getSectionId()))
                .findFirst()
                .orElse(null);

        assertNotNull(section, "Section sec1 should exist");
        assertEquals("forms/child-form.pdf", section.getTemplatePath(), "Child's templatePath should be applied to merged section");
    }
}
