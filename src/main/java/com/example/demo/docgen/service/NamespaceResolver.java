package com.example.demo.docgen.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resolves resource paths within namespaced (tenant-specific) template folders.
 * 
 * Supports:
 * - Namespace/tenant-specific template loading: namespace:templateId
 * - Cross-namespace resource references: common:resourcePath
 * - Default namespace fallback: "common-templates"
 */
@Slf4j
@Component
public class NamespaceResolver {
    
    private static final String DEFAULT_NAMESPACE = "common-templates";
    private static final String TEMPLATES_FOLDER = "templates";
    private static final String COMMON_PREFIX = "common:";
    
    /**
     * Resolve a template path within a namespace
     * 
     * @param namespace The namespace/tenant name (e.g., "tenant-a", "common-templates")
     * @param templateId The template ID relative to namespace/templates folder
     * @return Full classpath resource path (e.g., "tenant-a/templates/enrollment-form.yaml")
     */
    public String resolveTemplatePath(String namespace, String templateId) {
        String resolvedNamespace = namespace != null && !namespace.isEmpty() ? namespace : DEFAULT_NAMESPACE;
        
        log.debug("Resolving template in namespace '{}': {}", resolvedNamespace, templateId);
        
        return String.format("%s/%s/%s", resolvedNamespace, TEMPLATES_FOLDER, templateId);
    }
    
    /**
     * Resolve a resource path with support for cross-namespace references.
     * 
     * Examples:
     * - "enrollment-header.ftl" → "{currentNamespace}/templates/enrollment-header.ftl"
     * - "common:base-enrollment.ftl" → "common-templates/templates/base-enrollment.ftl"
     * - "forms/applicant.pdf" → "{currentNamespace}/templates/forms/applicant.pdf"
     * - "common:forms/header.pdf" → "common-templates/templates/forms/header.pdf"
     * 
     * NOTE: Resource paths should NOT include the "templates/" prefix. If a path starts with
     * "templates/", it will be normalized by removing that prefix.
     * 
     * @param resourcePath The resource path (may include common: prefix, should not include templates/)
     * @param currentNamespace The current namespace context
     * @return Full classpath resource path
     */
    public String resolveResourcePath(String resourcePath, String currentNamespace) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            return null;
        }
        
        // Normalize: remove "templates/" prefix if present (legacy path handling)
        String normalizedPath = resourcePath;
        if (resourcePath.startsWith(TEMPLATES_FOLDER + "/")) {
            normalizedPath = resourcePath.substring((TEMPLATES_FOLDER + "/").length());
            log.debug("Normalized resource path (removed templates/ prefix): {} → {}", resourcePath, normalizedPath);
        }
        
        // Check for cross-namespace reference (common:resourcePath)
        if (normalizedPath.startsWith(COMMON_PREFIX)) {
            String actualPath = normalizedPath.substring(COMMON_PREFIX.length());
            log.debug("Resolving cross-namespace resource: {} → {}", normalizedPath, actualPath);
            return resolveTemplatePath(DEFAULT_NAMESPACE, actualPath);
        }
        
        // Resolve within current namespace
        String resolvedNamespace = currentNamespace != null && !currentNamespace.isEmpty() 
            ? currentNamespace 
            : DEFAULT_NAMESPACE;
        
        return resolveTemplatePath(resolvedNamespace, normalizedPath);
    }
    
    /**
     * Normalize a namespace name (remove leading/trailing spaces, default to common-templates if empty)
     * 
     * @param namespace The namespace name
     * @return Normalized namespace name
     */
    public String normalizeNamespace(String namespace) {
        if (namespace == null || namespace.trim().isEmpty()) {
            return DEFAULT_NAMESPACE;
        }
        return namespace.trim();
    }
    
    /**
     * Check if a path reference is a cross-namespace reference
     * 
     * @param resourcePath The resource path
     * @return true if the path starts with "common:" prefix
     */
    public boolean isCrossNamespaceReference(String resourcePath) {
        return resourcePath != null && resourcePath.startsWith(COMMON_PREFIX);
    }
    
    /**
     * Extract the actual resource path from a potentially prefixed reference
     * 
     * @param resourcePath The resource path (may include common: prefix)
     * @return The resource path without the prefix
     */
    public String extractResourcePath(String resourcePath) {
        if (resourcePath != null && resourcePath.startsWith(COMMON_PREFIX)) {
            return resourcePath.substring(COMMON_PREFIX.length());
        }
        return resourcePath;
    }
    
    /**
     * Extract the namespace from a full resource path
     * 
     * Examples:
     * - "tenant-a/templates/form.yaml" → "tenant-a"
     * - "common-templates/templates/form.yaml" → "common-templates"
     * - "form.yaml" → "common-templates" (default)
     * - "templates/form.yaml" → "common-templates" (default)
     * 
     * @param path The full resource path
     * @return The namespace portion of the path
     */
    public String extractNamespaceFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return DEFAULT_NAMESPACE;
        }
        
        // If path contains "/" separator
        int firstSlash = path.indexOf('/');
        if (firstSlash > 0) {
            String potentialNamespace = path.substring(0, firstSlash);
            // Check if this looks like a namespace (contains namespace-like characters)
            // and is not just "templates" folder
            if (!potentialNamespace.equals(TEMPLATES_FOLDER)) {
                return potentialNamespace;
            }
        }
        
        // Default to common-templates if we can't extract a namespace
        return DEFAULT_NAMESPACE;
    }
}
