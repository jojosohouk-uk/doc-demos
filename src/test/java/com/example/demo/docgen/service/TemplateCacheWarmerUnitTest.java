package com.example.demo.docgen.service;

import com.example.demo.docgen.model.DocumentTemplate;
import com.example.demo.docgen.model.PageSection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TemplateCacheWarmerUnitTest {

    private TemplateLoader loader;
    private TemplateCacheWarmer warmer;

    @BeforeEach
    void setup() {
        loader = Mockito.mock(TemplateLoader.class);
        warmer = new TemplateCacheWarmer(loader);
    }

    @Test
    void warmsSimpleTemplateAndResources() throws Exception {
        DocumentTemplate t = new DocumentTemplate();
        PageSection s = new PageSection();
        s.setTemplatePath("common-templates/templates/signature-page.ftl");
        t.setSections(List.of(s));

        when(loader.loadTemplate("base-enrollment")).thenReturn(t);

        // set fields via reflection
        TestUtils.setField(warmer, "preloadTemplateIds", List.of("base-enrollment"));
        TestUtils.setField(warmer, "cacheEnabled", true);

        warmer.warmCache();

        verify(loader, times(1)).loadTemplate("base-enrollment");
        verify(loader, times(1)).getResourceBytes("common-templates/templates/signature-page.ftl");
    }

    @Test
    void warmsNamespacedTemplate() throws Exception {
        DocumentTemplate t = new DocumentTemplate();
        PageSection s = new PageSection();
        s.setTemplatePath("tenant-a/templates/forms/applicant-form.pdf");
        t.setSections(List.of(s));

        when(loader.loadTemplate("tenant-a", "composite-enrollment")).thenReturn(t);

        TestUtils.setField(warmer, "preloadNamespaces", Map.of("tenant-a", List.of("composite-enrollment")));
        TestUtils.setField(warmer, "cacheEnabled", true);

        warmer.warmCache();

        verify(loader, times(1)).loadTemplate("tenant-a", "composite-enrollment");
        verify(loader, times(1)).getResourceBytes("tenant-a/templates/forms/applicant-form.pdf");
    }
}
