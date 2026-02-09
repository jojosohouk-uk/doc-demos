package com.example.demo.docgen.service;

import com.example.demo.docgen.model.DocumentTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Collections;

/**
 * Integration tests for template loading with Config Server
 * Validates error handling when remote is enabled vs disabled
 */
@DisplayName("Config Server Error Handling Tests")
public class IntegrationConfigServerTest {

    @Test
    @DisplayName("Should fail with clear error when remote is enabled but Config Server is unreachable")
    public void testFailWithErrorWhenRemoteServerUnreachable() throws Exception {
        NamespaceResolver resolver = new NamespaceResolver();
        TemplateLoader loader = new TemplateLoader(resolver);

        // Remote enabled with unreachable server
        java.lang.reflect.Field remoteEnabledField = TemplateLoader.class.getDeclaredField("remoteEnabled");
        remoteEnabledField.setAccessible(true);
        remoteEnabledField.set(loader, true);

        java.lang.reflect.Field configUriField = TemplateLoader.class.getDeclaredField("configServerUri");
        configUriField.setAccessible(true);
        configUriField.set(loader, "http://localhost:9999");  // Non-existent server

        // Should throw RuntimeException (wrapping IOException with CONFIG_SERVER_ERROR), not fall back to local
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            loader.loadTemplate("integration-templates", "does-not-exist.yaml", Collections.emptyMap());
        }, "Should throw RuntimeException when Config Server is unreachable and remote is enabled");

        // Verify error chain contains CONFIG_SERVER_ERROR
        Throwable current = ex;
        boolean foundConfigServerError = false;
        while (current != null) {
            String msg = current.getMessage() != null ? current.getMessage() : "";
            if (msg.contains("CONFIG_SERVER_ERROR")) {
                foundConfigServerError = true;
                break;
            }
            current = current.getCause();
        }
        
        assertTrue(foundConfigServerError, 
            "Error chain should contain CONFIG_SERVER_ERROR indicating remote Config Server failure, full error: " + ex);
    }
}
