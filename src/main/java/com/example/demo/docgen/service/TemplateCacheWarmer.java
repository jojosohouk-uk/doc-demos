package com.example.demo.docgen.service;

import com.example.demo.docgen.model.DocumentTemplate;
import com.example.demo.docgen.model.PageSection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Warms the template cache at startup to avoid latency on the first request.
 * 
 * Supports two preload strategies:
 * 1. Simple: preload-ids - loads templates from default namespace
 * 2. Tenant-specific: separate properties per namespace (recommended)
 * 
 * Configuration examples:
 * 
 * Strategy 1 - Simple (default namespace):
 * docgen.templates.preload-ids=base-enrollment,composite-enrollment
 * 
 * Strategy 2 - Tenant-specific (clean, separate entries per namespace):
 * 
 * Via Properties:
 * docgen.templates.preload.tenant-a[0]=base-enrollment
 * docgen.templates.preload.tenant-a[1]=composite-enrollment
 * docgen.templates.preload.common-templates[0]=base-enrollment
 * 
 * Via YAML (recommended):
 * docgen:
 *   templates:
 *     preload:
 *       tenant-a:
 *         - base-enrollment
 *         - composite-enrollment
 *       common-templates:
 *         - base-enrollment
 * 
 * This avoids parsing errors and makes each namespace's templates independent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateCacheWarmer {

    private final TemplateLoader templateLoader;

    @Value("${docgen.templates.preload-ids:}")
    private List<String> preloadTemplateIds;

    @Value("#{${docgen.templates.preload:{}}}")
    private Map<String, List<String>> preloadNamespaces;

    @Value("${docgen.templates.cache-enabled:true}")
    private boolean cacheEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void warmCache() {
        if (!cacheEnabled) {
            log.info("Template cache warming skipped (caching disabled)");
            return;
        }

        boolean hasSimplePreload = preloadTemplateIds != null && !preloadTemplateIds.isEmpty();
        boolean hasNamespacePreload = preloadNamespaces != null && !preloadNamespaces.isEmpty();

        if (!hasSimplePreload && !hasNamespacePreload) {
            log.info("Template cache warming skipped (no templates configured)");
            return;
        }

        log.info("Starting template cache warming");
        long startTime = System.currentTimeMillis();

        // Strategy 1: Load simple template IDs (default namespace)
        if (hasSimplePreload) {
            log.info("Preloading {} templates from default namespace", preloadTemplateIds.size());
            for (String templateId : preloadTemplateIds) {
                warmTemplate(templateId);
            }
        }

        // Strategy 2: Load tenant-specific templates (separate entries per namespace)
        if (hasNamespacePreload) {
            log.info("Preloading {} namespace(s) with tenant-specific templates", preloadNamespaces.size());
            for (String namespace : preloadNamespaces.keySet()) {
                List<String> templateIds = preloadNamespaces.get(namespace);
                if (templateIds != null && !templateIds.isEmpty()) {
                    log.info("  Namespace '{}': {} template(s)", namespace, templateIds.size());
                    for (String templateId : templateIds) {
                        warmNamespacedTemplate(namespace, templateId);
                    }
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Template cache warming completed in {}ms", duration);
    }

    /**
     * Warm a template from the default namespace
     */
    private void warmTemplate(String templateId) {
        try {
            DocumentTemplate template = templateLoader.loadTemplate(templateId);
            log.info("  Warmed: {}", templateId);
            warmResources(template);
        } catch (Exception e) {
            log.error("  Failed to warm template: {}", templateId, e);
        }
    }

    /**
     * Warm a template from a specific namespace (tenant-aware)
     */
    private void warmNamespacedTemplate(String namespace, String templateId) {
        try {
            DocumentTemplate template = templateLoader.loadTemplate(namespace, templateId);
            log.info("    Warmed: {}/{}", namespace, templateId);
            warmResources(template);
        } catch (Exception e) {
            log.error("    Failed to warm template: {}/{}", namespace, templateId, e);
        }
    }

    /**
     * Pre-load all resources (PDFs, FTLs) referenced in template sections
     */
    private void warmResources(DocumentTemplate template) {
        if (template.getSections() != null) {
            for (PageSection section : template.getSections()) {
                if (section.getTemplatePath() != null && !section.getTemplatePath().isEmpty()) {
                    try {
                        templateLoader.getResourceBytes(section.getTemplatePath());
                        log.debug("      Resource: {}", section.getTemplatePath());
                    } catch (Exception e) {
                        log.warn("      Failed to warm resource: {}", section.getTemplatePath());
                    }
                }
            }
        }
    }
}
