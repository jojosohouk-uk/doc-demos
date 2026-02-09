package com.example.demo.docgen.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client for fetching resources (PDF, FTL, XLS) from external resource storage service.
 * Supports both explicit per-tenant URLs and pattern-based URL resolution.
 * 
 * Configuration:
 * - docgen.resources.storage-enabled: Enable external storage (default: false)
 * - docgen.resources.base-url: Base URL for pattern-based resolution
 * - docgen.resources.path-pattern: Pattern for resource path (e.g., {namespace}/templates/{resource})
 * - docgen.resources.{namespace}.url: Explicit URL for specific namespace
 */
@Slf4j
@Component
public class ResourceStorageClient {

    @Value("${docgen.resources.storage-enabled:false}")
    private boolean storageEnabled;

    @Value("${docgen.resources.base-url:http://localhost:9090}")
    private String baseUrl;

    @Value("${docgen.resources.path-pattern:{namespace}/templates/{resource}}")
    private String pathPattern;

    @Value("${docgen.resources.namespaces:}")
    private String namespacesConfig;

    private final RestClient restClient = RestClient.create();
    
    // Cache for explicit namespace URLs (loaded from config)
    private final Map<String, String> namespaceUrls = new ConcurrentHashMap<>();

    /**
     * Fetch resource bytes from external storage service.
     * 
     * @param namespace The namespace/tenant (e.g., "common-templates", "tenant-a")
     * @param resourcePath The resource path relative to namespace/templates (e.g., "forms/enrollment-form.pdf")
     * @return Resource bytes
     * @throws IOException if resource not found or fetch fails
     */
    public byte[] getResourceBytes(String namespace, String resourcePath) throws IOException {
        log.info("ResourceStorageClient.getResourceBytes called: storageEnabled={}, namespace={}, resourcePath={}", storageEnabled, namespace, resourcePath);
        if (!storageEnabled) {
            throw new IOException("External resource storage is not enabled");
        }

        String url = buildResourceUrl(namespace, resourcePath);
        log.info("Fetching resource from external storage: {} for {}/{}", url, namespace, resourcePath);

        try {
            byte[] data = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(byte[].class);

            if (data == null || data.length == 0) {
                throw new IOException("Resource returned empty response: " + url);
            }

            log.info("Successfully fetched resource: {} ({} bytes)", url, data.length);
            return data;
        } catch (RestClientException e) {
            throw new IOException(
                    "Failed to fetch resource from external storage: " + url + 
                    " (namespace: " + namespace + ", resource: " + resourcePath + ")",
                    e
            );
        }
    }

    /**
     * Build the resource URL using either explicit per-tenant URL or pattern-based resolution.
     * 
     * Priority:
     * 1. Explicit per-tenant URL (docgen.resources.{namespace}.url)
     * 2. Pattern-based URL (docgen.resources.base-url + docgen.resources.path-pattern)
     */
    private String buildResourceUrl(String namespace, String resourcePath) {
        // Try explicit per-tenant URL first
        String explicitUrl = getExplicitNamespaceUrl(namespace);
        if (explicitUrl != null && !explicitUrl.isEmpty()) {
            return explicitUrl + "/" + resourcePath;
        }

        // Fall back to pattern-based URL
        String resolvedPath = pathPattern
                .replace("{namespace}", namespace)
                .replace("{resource}", resourcePath);

        return baseUrl + "/" + resolvedPath;
    }

    /**
     * Get explicitly configured URL for a namespace.
     * Supports formats:
     * - docgen.resources.common-templates.url=http://storage.example.com/v1/files
     * - Or via docgen.resources.namespaces config
     */
    @Value("${docgen.resources.common-templates.url:}")
    private String commonTemplatesUrl;

    @Value("${docgen.resources.tenant-a.url:}")
    private String tenantAUrl;

    @Value("${docgen.resources.tenant-b.url:}")
    private String tenantBUrl;

    private String getExplicitNamespaceUrl(String namespace) {
        switch (namespace) {
            case "common-templates":
                return commonTemplatesUrl;
            case "tenant-a":
                return tenantAUrl;
            case "tenant-b":
                return tenantBUrl;
            default:
                // Could extend this to support dynamic namespaces via namespaces config
                return null;
        }
    }

    /**
     * Check if external storage is enabled.
     */
    public boolean isEnabled() {
        return storageEnabled;
    }

    /**
     * Get resource URL without fetching (useful for logging/debugging).
     */
    public String getResourceUrl(String namespace, String resourcePath) {
        if (!storageEnabled) {
            return null;
        }
        return buildResourceUrl(namespace, resourcePath);
    }
}
