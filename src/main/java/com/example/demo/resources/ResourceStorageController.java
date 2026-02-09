package com.example.demo.resources;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.util.AntPathMatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resource storage controller that serves PDF, FTL, and other resource files.
 * 
 * In dev profile, loads from filesystem (config-repo directory).
 * In production, this would be replaced with calls to real object storage (S3, GCS, etc).
 * 
 * Configuration:
 * - docgen.resources.storage-basepath: Filesystem path to storage directory (optional)
 * - If not set, falls back to classpath resources
 * 
 * API:
 * GET /api/resources/{namespace}/{resource}
 * 
 * Examples:
 * GET /api/resources/common-templates/templates/forms/enrollment-form.pdf
 * GET /api/resources/tenant-a/templates/freemarker/enrollment.ftl
 */
@Slf4j
@RestController
@RequestMapping("/api/resources")
public class ResourceStorageController {

    private final ResourceLoader resourceLoader;
    
    @Value("${docgen.resources.storage-basepath:}")
    private String storageBasePath;

    public ResourceStorageController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Fetch a resource file for a specific namespace.
     * 
     * @param namespace The namespace/tenant
     * @param request The HTTP request to extract resource path from
     * @return The resource bytes with appropriate content type
     */
    @GetMapping("/{namespace}/**")
    public ResponseEntity<byte[]> getResource(
            @PathVariable String namespace,
            HttpServletRequest request) throws IOException {

        // Extract the remaining path (everything after /{namespace}/)
        String pathWithinHandlerMapping = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchingPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String resourcePath = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, pathWithinHandlerMapping);
        
        log.debug("Fetching resource: {} for namespace {}", resourcePath, namespace);

        // Try to load from filesystem first if storage path is configured
        if (storageBasePath != null && !storageBasePath.isEmpty()) {
            try {
                Path filePath = Paths.get(storageBasePath, namespace, resourcePath);
                log.debug("Attempting to load from filesystem: {}", filePath);
                
                if (Files.exists(filePath)) {
                    byte[] fileContent = Files.readAllBytes(filePath);
                    MediaType contentType = getContentType(resourcePath);
                    
                    log.info("Successfully served resource from filesystem: {}/{} ({} bytes)", 
                             namespace, resourcePath, fileContent.length);
                    
                    return ResponseEntity.ok()
                            .contentType(contentType)
                            .body(fileContent);
                }
            } catch (IOException e) {
                log.warn("Failed to load from filesystem: {} - will try classpath", storageBasePath, e);
            }
        }

        // Fall back to classpath
        String classpathResourcePath = "classpath:config-repo/" + namespace + "/" + resourcePath;
        log.debug("Attempting to load from classpath: {}", classpathResourcePath);

        // Load from classpath
        Resource resource = resourceLoader.getResource(classpathResourcePath);
        
        if (!resource.exists()) {
            log.warn("Resource not found: {} for namespace {} (filesystem: {}, classpath: {})", 
                     resourcePath, namespace, storageBasePath, classpathResourcePath);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(("Resource not found: " + resourcePath).getBytes());
        }

        // Read file
        byte[] fileContent = resource.getContentAsByteArray();
        
        // Determine content type
        MediaType contentType = getContentType(resourcePath);
        
        log.info("Successfully served resource from classpath: {}/{} ({} bytes)", namespace, resourcePath, fileContent.length);
        
        return ResponseEntity.ok()
                .contentType(contentType)
                .body(fileContent);
    }

    /**
     * Determine the appropriate content type based on file extension.
     */
    private MediaType getContentType(String resourcePath) {
        int lastDot = resourcePath.lastIndexOf('.');
        if (lastDot < 0) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        
        String extension = resourcePath.substring(lastDot + 1).toLowerCase();
        
        switch (extension) {
            case "pdf":
                return MediaType.APPLICATION_PDF;
            case "ftl":
            case "txt":
                return MediaType.TEXT_PLAIN;
            case "xls":
            case "xlsx":
                return MediaType.parseMediaType("application/vnd.ms-excel");
            case "png":
                return MediaType.IMAGE_PNG;
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG;
            case "json":
                return MediaType.APPLICATION_JSON;
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Resource storage service is running");
    }
}
