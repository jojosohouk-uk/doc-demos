# Namespace-Aware Template System

## Overview

The document generation system supports **namespace-aware (multi-tenant) template loading**, allowing different tenants or applications to maintain isolated template libraries while sharing common templates.

## Concepts

### Namespaces
A **namespace** is a logical grouping of templates for a specific tenant or application context. Templates in one namespace are isolated from templates in other namespaces.

**Examples:**
- `common-templates` - Shared templates used by all tenants
- `tenant-a` - Templates specific to Tenant A
- `tenant-b` - Templates specific to Tenant B
- `integration-templates` - Templates for integration tests

### Cross-Namespace References
Templates can reference templates in other namespaces using the `common:` prefix for common templates.

**Example:**
```yaml
# In tenant-a template
baseTemplateId: common:base-enrollment  # References common-templates/templates/base-enrollment.yaml
```

Without the `common:` prefix, references are resolved within the current namespace.

## File Structure

Namespaced templates follow this directory structure:

```
src/main/resources/
├── config-repo/                          # Local filesystem backup
│   ├── common-templates/
│   │   └── templates/
│   │       ├── base-enrollment.yaml
│   │       ├── invoice.yaml
│   │       └── forms/
│   │           └── header.pdf
│   ├── tenant-a/
│   │   └── templates/
│   │       ├── enrollment-request.yaml
│   │       └── forms/
│   │           └── tenant-a-form.pdf
│   └── tenant-b/
│       └── templates/
│           └── enrollment-v2.yaml
├── templates/                            # Legacy non-namespaced location (classpath)
│   └── ...
```

On the **Config Server**, templates are stored with each namespace as the top-level folder:
```
config-repo/
├── common-templates/
│   └── templates/
│       ├── base-enrollment.yaml
│       ├── invoice.yaml
│       └── forms/
│           └── header.pdf
├── tenant-a/
│   └── templates/
│       ├── enrollment-request.yaml
│       └── forms/
│           └── tenant-a-form.pdf
└── tenant-b/
    └── templates/
        ├── enrollment-v2.yaml
        └── forms/
            └── tenant-b-form.pdf
```

The Config Server plain-text API serves these via:
```
GET http://localhost:8888/{namespace}/{profile}/{label}/{templatePath}
```

**Example requests:**
```
GET http://localhost:8888/common-templates/dev/main/base-enrollment.yaml
GET http://localhost:8888/tenant-a/dev/main/enrollment-request.yaml
GET http://localhost:8888/tenant-a/dev/main/forms/tenant-a-form.pdf
```

## Usage

### Loading Namespace-Aware Templates

```java
// Load from specific namespace
DocumentTemplate template = templateLoader.loadTemplate(
    "tenant-a",                           // namespace
    "enrollment-request.yaml"             // template ID (relative to namespace/templates/)
);

// Load with variable substitution
DocumentTemplate template = templateLoader.loadTemplate(
    "common-templates",
    "base-enrollment.yaml",
    Map.of("state", "CA")                 // Variables for placeholder resolution
);
```

### Template Inheritance Within Namespace

```yaml
# common-templates/templates/base-enrollment.yaml
templateId: base-enrollment
sections:
  - sectionId: applicant-info
    type: ACROFORM
    templatePath: forms/applicant-form.pdf
```

```yaml
# tenant-a/templates/enrollment-request.yaml
templateId: enrollment-request
baseTemplateId: base-enrollment              # Inherits from base-enrollment.yaml
sections:
  - sectionId: tenant-specific-section
    type: FREEMARKER
    templatePath: tenant-specific.ftl
```

### Cross-Namespace References

```yaml
# tenant-a/templates/enrollment-request.yaml
templateId: enrollment-request
baseTemplateId: common:base-enrollment      # References common-templates
includedFragments:
  - common:shared-footer                    # References common-templates fragment
sections: [...]
```

## Configuration

### Enable Remote Templates (Config Server)

**application-dev.properties:**
```properties
spring.cloud.config.uri=http://localhost:8888
docgen.templates.remote-enabled=true
```

When `remote-enabled=true`:
- Templates are fetched from Config Server first
- **No fallback** to local templates if Config Server is unreachable
- Throws `CONFIG_SERVER_ERROR` if fetch fails

**application.properties (default):**
```properties
docgen.templates.remote-enabled=false
```

When `remote-enabled=false`:
- Templates loaded from classpath/filesystem in this order:
  1. Classpath resources (`src/main/resources/`)
  2. Filesystem (`config-repo/` directory)

## Resource Resolution

### Resource Paths
Resources (PDFs, images, FreeMarker templates) are resolved using namespace context:

```yaml
# In tenant-a/templates/my-template.yaml
sections:
  - sectionId: form-section
    type: ACROFORM
    templatePath: forms/enrollment-form.pdf
    # Resolves to: tenant-a/templates/forms/enrollment-form.pdf
```

### Cross-Namespace Resources
Use `common:` prefix for resources in common-templates:

```yaml
sections:
  - sectionId: header
    type: PDFBOX_COMPONENT
    templatePath: common:resources/header.pdf
    # Resolves to: common-templates/templates/resources/header.pdf
```

## Error Codes

| Code | Description | Example |
|------|-------------|---------|
| `TEMPLATE_NOT_FOUND` | Template file not found in namespace | Template "enroll.yaml" not found in tenant-a |
| `CONFIG_SERVER_ERROR` | Remote template fetch failed (no fallback) | Failed to fetch from Config Server at http://localhost:8888 |
| `RESOURCE_NOT_FOUND` | PDF/FTL/image resource file not found | PDF template "forms/my-form.pdf" not found |
| `CIRCULAR_REFERENCE` | Circular template inheritance detected | Template "a.yaml" references itself through inheritance chain |

### Example Error Response
```json
{
  "error": "TEMPLATE_NOT_FOUND",
  "message": "Template not found for id 'enrollment.yaml'. Check src/main/resources/templates or enable remote templates.",
  "timestamp": "2026-02-08T20:30:00Z"
}
```

## Migration Guide

### From Non-Namespaced to Namespaced Templates

**Before (non-namespaced):**
```
src/main/resources/templates/
├── base-enrollment.yaml
├── invoice.yaml
└── forms/
    └── applicant-form.pdf
```

**After (namespaced):**
```
src/main/resources/
├── config-repo/
│   ├── common-templates/templates/
│   │   ├── base-enrollment.yaml
│   │   ├── invoice.yaml
│   │   └── forms/applicant-form.pdf
│   ├── tenant-a/templates/
│   │   └── custom-enrollment.yaml
│   └── tenant-b/templates/
│       └── ...
```

**Code Changes:**

```java
// Before
DocumentTemplate template = templateLoader.loadTemplate("base-enrollment.yaml");

// After
DocumentTemplate template = templateLoader.loadTemplate(
    "common-templates",
    "base-enrollment.yaml"
);
```

### Coexistence
Both approaches can coexist:
- Non-namespaced templates: `loadTemplate(String templateId)`
- Namespaced templates: `loadTemplate(String namespace, String templateId)`

## Best Practices

### 1. Organize by Tenant
```
common-templates/         # Shared across all tenants
tenant-a/                 # Only for Tenant A
tenant-b/                 # Only for Tenant B
```

### 2. Use Common Templates for Shared Sections
```yaml
# Avoid duplication
baseTemplateId: common:base-form
# Instead of copying common sections to each tenant
```

### 3. Handle Cross-Namespace References Carefully
```yaml
# Good - explicit cross-namespace reference
baseTemplateId: common:base-enrollment

# Avoid - implicit reference that only works in one namespace
baseTemplateId: base-enrollment
```

### 4. Consistent Naming
- Use kebab-case for template IDs: `enrollment-request.yaml`
- Use semantic names: `base-enrollment` not `template1`
- Match file names to template IDs

### 5. Document Dependencies
```yaml
templateId: enrollment-request
# Depends on: common:base-enrollment, common:header-footer
baseTemplateId: common:base-enrollment
sections: [...]
```

## Testing

### Unit Tests for Templates

```java
@Test
public void testNamespaceIsolation() {
    DocumentTemplate tenantA = templateLoader.loadTemplate("tenant-a", "enrollment.yaml");
    DocumentTemplate tenantB = templateLoader.loadTemplate("tenant-b", "enrollment.yaml");
    
    // Different templates despite same ID
    assertNotEquals(tenantA, tenantB);
}

@Test
public void testCrossNamespaceReference() {
    DocumentTemplate template = templateLoader.loadTemplate(
        "tenant-a",
        "enrollment-with-common.yaml"
    );
    
    // Should inherit from common-templates
    assertTrue(template.getSections().size() > 0);
}

@Test
public void testCircularReferenceDetection() {
    assertThrows(
        TemplateLoadingException.class,
        () -> templateLoader.loadTemplate("circular-a.yaml")
    );
}
```

## Troubleshooting

### Issue: "Template not found for id 'base-enrollment.yaml'"

**Cause:** Loading without namespace prefix when template is in `common-templates`

**Solution:**
```java
// Wrong
templateLoader.loadTemplate("base-enrollment.yaml");

// Correct - use namespace or cross-namespace reference
templateLoader.loadTemplate("common-templates", "base-enrollment.yaml");

// Or in YAML
baseTemplateId: common:base-enrollment
```

### Issue: "Circular template reference detected"

**Cause:** Template inheritance forms a cycle (A → B → A)

**Solution:** Review template inheritance chain:
```yaml
# Check these files for circular dependencies
# - template A's baseTemplateId
# - template B's baseTemplateId
# - ensure they don't eventually reference A
```

### Issue: "Config Server Error - Template not found"

**Cause:** When `remote-enabled=true`, Config Server must be available

**Solution:**
```properties
# Set to false during development if Config Server unavailable
docgen.templates.remote-enabled=false

# Or ensure Config Server is running
spring.cloud.config.uri=http://localhost:8888
```

## Performance Considerations

### Caching
Templates are cached using Spring's `@Cacheable` annotation:
```java
@Cacheable(value = "documentTemplates", key = "{ #namespace, #templateId }")
public DocumentTemplate loadTemplate(String namespace, String templateId)
```

Clear cache when templates change:
```java
@CacheEvict(value = "documentTemplates", allEntries = true)
public void clearTemplateCache()
```

### Circular Reference Detection
Uses ThreadLocal stack - minimal performance impact:
- ~O(1) check on each load
- Automatic cleanup with try-finally
- No memory leaks (ThreadLocal properly managed)

## Security Considerations

### Namespace Isolation
Each namespace is isolated - templates in `tenant-a` cannot directly access templates in `tenant-b`:
```yaml
# This does NOT work (no implicit cross-tenant access)
baseTemplateId: tenant-b:some-template

# Must use common: prefix for explicitly shared templates
baseTemplateId: common:some-template
```

### Config Server Authentication
When using Config Server, configure authentication:
```properties
spring.cloud.config.username=app-user
spring.cloud.config.password=secure-password
```

## References

- [Spring Cloud Config Server](https://cloud.spring.io/spring-cloud-config/reference/html/)
- [Template Inheritance Pattern](ARCHITECTURE_DIAGRAMS.md)
- [Error Handling Guide](../docs/README.md)
