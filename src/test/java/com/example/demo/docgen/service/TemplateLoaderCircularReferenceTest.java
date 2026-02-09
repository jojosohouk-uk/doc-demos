package com.example.demo.docgen.service;

import com.example.demo.docgen.exception.TemplateLoadingException;
import com.example.demo.docgen.model.DocumentTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for circular reference detection in TemplateLoader
 * Verifies that circular template inheritance and fragments are detected and prevented
 */
public class TemplateLoaderCircularReferenceTest {

    private TemplateLoader templateLoader;
    private NamespaceResolver namespaceResolver;

    @BeforeEach
    public void setup() {
        namespaceResolver = new NamespaceResolver();
        templateLoader = new TemplateLoader(namespaceResolver);
    }

    @Test
    @DisplayName("Should detect direct circular reference (template inherits from itself)")
    public void testDirectCircularReference() {
        // This test documents the expected behavior when templates reference themselves
        // The actual template file would need to have: baseTemplateId: circular-self.yaml
        
        TemplateLoadingException exception = assertThrows(
                TemplateLoadingException.class,
                () -> templateLoader.loadTemplate("test-circular-self.yaml"),
                "Should throw TemplateLoadingException for direct circular reference"
        );

        assertEquals("CIRCULAR_REFERENCE", exception.getCode(),
                "Error code should be CIRCULAR_REFERENCE");
        assertTrue(exception.getDescription().contains("Circular template reference"),
                "Error description should mention circular reference");
        assertTrue(exception.getDescription().contains("test-circular-self.yaml"),
                "Error description should mention the problematic template");
    }

    @Test
    @DisplayName("Should detect circular reference through inheritance chain")
    public void testIndirectCircularReference() {
        // Test case: A -> B -> C -> A (circular chain)
        // This tests the behavior when templates create a cycle through inheritance
        
        TemplateLoadingException exception = assertThrows(
                TemplateLoadingException.class,
                () -> templateLoader.loadTemplate("test-circular-a.yaml"),
                "Should throw TemplateLoadingException for indirect circular reference"
        );

        assertEquals("CIRCULAR_REFERENCE", exception.getCode());
        assertTrue(exception.getDescription().contains("Circular template reference"));
    }

    @Test
    @DisplayName("Should detect circular reference in fragments")
    public void testCircularReferenceInFragments() {
        // Test case: Template includes a fragment that eventually includes the original template
        
        TemplateLoadingException exception = assertThrows(
                TemplateLoadingException.class,
                () -> templateLoader.loadTemplate("test-fragment-circular.yaml"),
                "Should throw TemplateLoadingException for circular fragment reference"
        );

        assertEquals("CIRCULAR_REFERENCE", exception.getCode());
    }

    @Test
    @DisplayName("Should allow valid inheritance chains without false positives")
    public void testValidInheritanceChain() {
        // Test that normal, non-circular inheritance still works
        // base -> child -> grandchild (linear chain is fine)
        
        DocumentTemplate template = templateLoader.loadTemplate("test-inheritance-grandchild.yaml");
        
        assertNotNull(template);
        assertEquals("test-inheritance-grandchild", template.getTemplateId());
        assertFalse(template.getSections().isEmpty(),
                "Template should have sections merged from inheritance chain");
    }

    @Test
    @DisplayName("Should prevent stack overflow from circular references")
    public void testPreventStackOverflow() {
        // Verify that circular references are caught before causing stack overflow
        // This is important for production stability
        
        // Create a namespace context where circular reference might occur
        String namespace = "test-namespace";
        
        // This should throw TemplateLoadingException, not StackOverflowError
        assertThrows(
                TemplateLoadingException.class,
                () -> templateLoader.loadTemplate(namespace, "test-circular-self.yaml"),
                "Should throw TemplateLoadingException, not StackOverflowError"
        );
    }

    @Test
    @DisplayName("Should handle multiple concurrent template loads without interference")
    public void testConcurrentLoadingWithCircularDetection() throws InterruptedException {
        // Verify that ThreadLocal stack is properly isolated between threads
        
        Thread thread1 = new Thread(() -> {
            try {
                templateLoader.loadTemplate("test-inheritance-base.yaml");
            } catch (Exception e) {
                fail("Thread 1 should not throw exception: " + e.getMessage());
            }
        });

        Thread thread2 = new Thread(() -> {
            // Thread 2 tries to load a circular reference
            TemplateLoadingException exception = assertThrows(
                    TemplateLoadingException.class,
                    () -> templateLoader.loadTemplate("test-circular-self.yaml")
            );
            assertEquals("CIRCULAR_REFERENCE", exception.getCode());
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // If we reach here, both threads completed without interference
    }

    @Test
    @DisplayName("Should clean up loading stack after template load completes")
    public void testLoadingStackCleanup() {
        // Verify that ThreadLocal is properly cleaned up after loading
        
        // First load
        DocumentTemplate template1 = templateLoader.loadTemplate("test-inheritance-base.yaml");
        assertNotNull(template1);
        
        // Second load should work fine (ThreadLocal should be clean)
        DocumentTemplate template2 = templateLoader.loadTemplate("test-inheritance-child.yaml");
        assertNotNull(template2);
        
        // Both loads should succeed, indicating ThreadLocal was cleaned up properly
    }

    @Test
    @DisplayName("Should detect circular reference with descriptive error message")
    public void testCircularReferenceErrorMessage() {
        TemplateLoadingException exception = assertThrows(
                TemplateLoadingException.class,
                () -> templateLoader.loadTemplate("test-circular-self.yaml")
        );

        String description = exception.getDescription();
        assertNotNull(description);
        assertFalse(description.isEmpty());
        
        // Error message should help developers debug the issue
        assertTrue(
                description.contains("Circular") || 
                description.contains("circular") ||
                description.contains("already being loaded"),
                "Description should clearly indicate a circular reference issue"
        );
    }
}
