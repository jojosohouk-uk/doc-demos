package com.example.demo.docgen.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NamespaceResolver
 * Tests namespace normalization, path resolution, and cross-namespace references
 */
@DisplayName("Namespace Resolver Tests")
public class NamespaceResolverTest {

    private final NamespaceResolver resolver = new NamespaceResolver();

    @Nested
    @DisplayName("Namespace Normalization Tests")
    class NamespaceNormalizationTests {
        
        @Test
        @DisplayName("Should normalize valid namespace")
        public void testNormalizeValidNamespace() {
            String normalized = resolver.normalizeNamespace("tenant-a");
            assertEquals("tenant-a", normalized, "Should return the same namespace");
        }

        @Test
        @DisplayName("Should convert null to default namespace")
        public void testNormalizeNullNamespace() {
            String normalized = resolver.normalizeNamespace(null);
            assertEquals("common-templates", normalized, "Should default to common-templates");
        }

        @Test
        @DisplayName("Should convert empty string to default namespace")
        public void testNormalizeEmptyNamespace() {
            String normalized = resolver.normalizeNamespace("");
            assertEquals("common-templates", normalized, "Should default to common-templates");
        }

        @Test
        @DisplayName("Should convert whitespace to default namespace")
        public void testNormalizeWhitespaceNamespace() {
            String normalized = resolver.normalizeNamespace("   ");
            assertEquals("common-templates", normalized, "Should default to common-templates for whitespace");
        }

        @Test
        @DisplayName("Should preserve common-templates namespace")
        public void testNormalizeCommonTemplatesNamespace() {
            String normalized = resolver.normalizeNamespace("common-templates");
            assertEquals("common-templates", normalized, "Should preserve common-templates");
        }
    }

    @Nested
    @DisplayName("Template Path Resolution Tests")
    class TemplatePathResolutionTests {
        
        @Test
        @DisplayName("Should resolve template path to namespace/templates/templateId")
        public void testResolveTemplatePathBasic() {
            String resolved = resolver.resolveTemplatePath("tenant-a", "enrollment-form.yaml");
            assertEquals("tenant-a/templates/enrollment-form.yaml", resolved, 
                "Should resolve to namespace/templates/templateId");
        }

        @Test
        @DisplayName("Should resolve template path with default namespace")
        public void testResolveTemplatePathWithDefaultNamespace() {
            String resolved = resolver.resolveTemplatePath("common-templates", "base-form.yaml");
            assertEquals("common-templates/templates/base-form.yaml", resolved, 
                "Should resolve correctly for common-templates");
        }

        @Test
        @DisplayName("Should resolve template path with nested template ID")
        public void testResolveTemplatePathWithNestedId() {
            String resolved = resolver.resolveTemplatePath("tenant-a", "forms/enrollment.yaml");
            assertEquals("tenant-a/templates/forms/enrollment.yaml", resolved, 
                "Should handle nested template IDs");
        }

        @Test
        @DisplayName("Should handle null namespace in template path resolution")
        public void testResolveTemplatePathWithNullNamespace() {
            String resolved = resolver.resolveTemplatePath(null, "form.yaml");
            assertEquals("common-templates/templates/form.yaml", resolved, 
                "Should default to common-templates when namespace is null");
        }
    }

    @Nested
    @DisplayName("Resource Path Resolution Tests")
    class ResourcePathResolutionTests {
        
        @Test
        @DisplayName("Should resolve simple resource path in namespace")
        public void testResolveResourcePathSimple() {
            String resolved = resolver.resolveResourcePath("signature.ftl", "tenant-a");
            assertEquals("tenant-a/templates/signature.ftl", resolved, 
                "Should resolve resource to namespace/templates/resource");
        }

        @Test
        @DisplayName("Should resolve nested resource path in namespace")
        public void testResolveResourcePathNested() {
            String resolved = resolver.resolveResourcePath("forms/header.pdf", "tenant-a");
            assertEquals("tenant-a/templates/forms/header.pdf", resolved, 
                "Should resolve nested resource correctly");
        }

        @Test
        @DisplayName("Should handle resource path with leading templates/")
        public void testResolveResourcePathWithLeadingTemplates() {
            String resolved = resolver.resolveResourcePath("templates/signature.ftl", "tenant-a");
            assertEquals("tenant-a/templates/signature.ftl", resolved, 
                "Should normalize leading templates/ prefix");
        }

        @Test
        @DisplayName("Should resolve cross-namespace resource with common: prefix")
        public void testResolveResourcePathWithCommonPrefix() {
            String resolved = resolver.resolveResourcePath("common:forms/header.pdf", "tenant-a");
            assertEquals("common-templates/templates/forms/header.pdf", resolved, 
                "Should resolve common: prefix to common-templates namespace");
        }

        @Test
        @DisplayName("Should resolve common: prefix without namespace context")
        public void testResolveCommonPrefixWithoutContext() {
            String resolved = resolver.resolveResourcePath("common:base.ftl", null);
            assertEquals("common-templates/templates/base.ftl", resolved, 
                "Should resolve common: prefix even without context namespace");
        }

        @Test
        @DisplayName("Should handle complex resource path with common: prefix")
        public void testResolveComplexCommonPrefixPath() {
            String resolved = resolver.resolveResourcePath("common:nested/deep/resource.pdf", "tenant-a");
            assertEquals("common-templates/templates/nested/deep/resource.pdf", resolved, 
                "Should resolve complex nested paths with common: prefix");
        }

        @Test
        @DisplayName("Should handle default namespace for resource resolution")
        public void testResolveResourcePathDefaultNamespace() {
            String resolved = resolver.resolveResourcePath("footer.ftl", "common-templates");
            assertEquals("common-templates/templates/footer.ftl", resolved, 
                "Should work with default common-templates namespace");
        }

        @Test
        @DisplayName("Should handle null namespace in resource path resolution")
        public void testResolveResourcePathWithNullNamespace() {
            String resolved = resolver.resolveResourcePath("signature.ftl", null);
            assertEquals("common-templates/templates/signature.ftl", resolved, 
                "Should default to common-templates when namespace is null");
        }
    }

    @Nested
    @DisplayName("Namespace Extraction Tests")
    class NamespaceExtractionTests {
        
        @Test
        @DisplayName("Should extract namespace from full path")
        public void testExtractNamespaceFromPath() {
            String namespace = resolver.extractNamespaceFromPath("tenant-a/templates/form.yaml");
            assertEquals("tenant-a", namespace, "Should extract tenant-a from path");
        }

        @Test
        @DisplayName("Should extract common-templates namespace from path")
        public void testExtractCommonTemplatesNamespaceFromPath() {
            String namespace = resolver.extractNamespaceFromPath("common-templates/templates/form.yaml");
            assertEquals("common-templates", namespace, "Should extract common-templates from path");
        }

        @Test
        @DisplayName("Should return default namespace for path without namespace prefix")
        public void testExtractNamespaceFromPathWithoutPrefix() {
            String namespace = resolver.extractNamespaceFromPath("form.yaml");
            assertEquals("common-templates", namespace, "Should default to common-templates");
        }

        @Test
        @DisplayName("Should handle nested paths with namespace")
        public void testExtractNamespaceFromNestedPath() {
            String namespace = resolver.extractNamespaceFromPath("tenant-a/templates/forms/nested/form.yaml");
            assertEquals("tenant-a", namespace, "Should extract tenant-a from nested path");
        }

        @Test
        @DisplayName("Should handle paths with only templates/ prefix")
        public void testExtractNamespaceFromTemplatesOnlyPath() {
            String namespace = resolver.extractNamespaceFromPath("templates/form.yaml");
            assertEquals("common-templates", namespace, "Should default when only templates/ prefix exists");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Should handle multiple slashes in resource path")
        public void testResolveResourcePathWithMultipleSlashes() {
            String resolved = resolver.resolveResourcePath("forms//header.pdf", "tenant-a");
            assertTrue(resolved.contains("forms"), "Should handle malformed paths gracefully");
        }

        @Test
        @DisplayName("Should handle resource path with special characters")
        public void testResolveResourcePathWithSpecialChars() {
            String resolved = resolver.resolveResourcePath("forms/header-2024.pdf", "tenant-a");
            assertEquals("tenant-a/templates/forms/header-2024.pdf", resolved, 
                "Should handle paths with special characters");
        }

        @Test
        @DisplayName("Should handle namespace with special characters")
        public void testResolveWithSpecialCharNamespace() {
            String resolved = resolver.resolveTemplatePath("tenant-prod-2", "form.yaml");
            assertEquals("tenant-prod-2/templates/form.yaml", resolved, 
                "Should handle namespace names with special characters");
        }

        @Test
        @DisplayName("Should preserve case in namespace names")
        public void testPreserveCaseInNamespace() {
            String resolved = resolver.resolveTemplatePath("Tenant-A", "form.yaml");
            assertEquals("Tenant-A/templates/form.yaml", resolved, 
                "Should preserve case in namespace names");
        }

        @Test
        @DisplayName("Should handle resource path ending with slash")
        public void testResolveResourcePathEndingWithSlash() {
            String resolved = resolver.resolveResourcePath("forms/", "tenant-a");
            assertTrue(resolved.contains("forms"), "Should handle trailing slashes");
        }
    }

    @Nested
    @DisplayName("Consistency Tests")
    class ConsistencyTests {
        
        @Test
        @DisplayName("Template path resolution should be consistent")
        public void testTemplatePathResolutionConsistency() {
            String resolved1 = resolver.resolveTemplatePath("tenant-a", "form.yaml");
            String resolved2 = resolver.resolveTemplatePath("tenant-a", "form.yaml");
            assertEquals(resolved1, resolved2, "Resolution should be consistent");
        }

        @Test
        @DisplayName("Resource path resolution should be consistent")
        public void testResourcePathResolutionConsistency() {
            String resolved1 = resolver.resolveResourcePath("header.pdf", "tenant-a");
            String resolved2 = resolver.resolveResourcePath("header.pdf", "tenant-a");
            assertEquals(resolved1, resolved2, "Resolution should be consistent");
        }

        @Test
        @DisplayName("Namespace normalization should be consistent")
        public void testNamespaceNormalizationConsistency() {
            String norm1 = resolver.normalizeNamespace(null);
            String norm2 = resolver.normalizeNamespace(null);
            assertEquals(norm1, norm2, "Normalization should be consistent");
            assertEquals("common-templates", norm1, "Should consistently default to common-templates");
        }

        @Test
        @DisplayName("Combined namespace and resource path operations should be consistent")
        public void testCombinedOperationsConsistency() {
            String namespace = resolver.normalizeNamespace("tenant-a");
            String resourcePath = resolver.resolveResourcePath("header.pdf", namespace);
            String templatePath = resolver.resolveTemplatePath(namespace, "form.yaml");
            
            assertEquals("tenant-a/templates/header.pdf", resourcePath);
            assertEquals("tenant-a/templates/form.yaml", templatePath);
        }
    }
}
