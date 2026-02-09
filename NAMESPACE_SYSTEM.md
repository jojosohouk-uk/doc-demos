# Multi-Tenant Namespace Template System

This document describes the multi-tenant namespace-aware template system for the document generation service.

## Overview

The system supports organizing templates into separate namespaces (tenants), allowing each tenant to have its own set of YAML template definitions and template resource files (FTL, PDF, XLS, etc.).

## Directory Structure

Templates are organized in the `src/main/resources/` directory with the following structure:

```
src/main/resources/
├── common-templates/          # Shared templates accessible by all tenants
│   └── templates/
│       ├── base-enrollment.yaml
│       ├── *.ftl              # FreeMarker templates
│       ├── *.pdf              # PDF form templates
│       └── forms/
│           └── applicant.pdf
│
├── tenant-a/                  # Tenant-specific templates
│   └── templates/
│       ├── enrollment-form.yaml
│       ├── *.ftl
│       ├── *.pdf
│       └── forms/
│
├── tenant-b/                  # Another tenant
│   └── templates/
│       ├── enrollment-form.yaml
│       ├── *.ftl
│       └── *.pdf
│
└── other-resources/
```

## API Usage

### Request Format

When making a document generation request, include a `namespace` field to specify the tenant:

```json
{
  "namespace": "tenant-a",
  "templateId": "enrollment-form.yaml",
  "data": {
    "applicant": {
      "firstName": "John",
      "lastName": "Doe"
    }
  }
}
```

### Namespace Parameter

- **namespace** (optional): The tenant/namespace name where the template resides
  - Examples: `"tenant-a"`, `"tenant-b"`, `"common-templates"`
  - Default: `"common-templates"` (if omitted)
  - Case-sensitive

- **templateId** (required): The template file identifier relative to `{namespace}/templates/`
  - Examples: `"enrollment-form.yaml"`, `"composite-enrollment.yaml"`
  - Should NOT include the `{namespace}/templates/` prefix

- **data**: Runtime data for template substitution

## Cross-Namespace Resource References

Templates in one namespace can reference resources from the `common-templates` namespace using the `common:` prefix.

### Example: YAML Template with Cross-Namespace References

```yaml
templateId: tenant-a-enrollment
sections:
  - sectionId: header
    type: FREEMARKER
    templatePath: common:templates/header.ftl    # From common-templates
    mappingType: DIRECT
    
  - sectionId: applicant-form
    type: ACROFORM
    templatePath: forms/applicant.pdf            # From tenant-a/templates/forms/
    mappingType: DIRECT
    fieldMappings:
      firstName: applicant.firstName
      lastName: applicant.lastName
```

### Resource Path Resolution

- **Local reference** (no prefix):
  - `"header.ftl"` → `{currentNamespace}/templates/header.ftl`
  - `"forms/applicant.pdf"` → `{currentNamespace}/templates/forms/applicant.pdf`

- **Cross-namespace reference** (with `common:` prefix):
  - `"common:header.ftl"` → `common-templates/templates/header.ftl`
  - `"common:forms/base.pdf"` → `common-templates/templates/forms/base.pdf`

## Examples

### Example 1: Tenant-Specific Template

Request:
```json
{
  "namespace": "tenant-a",
  "templateId": "enrollment.yaml"
}
```

Loads: `src/main/resources/tenant-a/templates/enrollment.yaml`

### Example 2: Default to Common-Templates

Request:
```json
{
  "templateId": "base-form.yaml"
}
```

Loads: `src/main/resources/common-templates/templates/base-form.yaml` (default namespace)

### Example 3: Cross-Namespace Resource Reference

Template file: `tenant-a/templates/enrollment.yaml`

```yaml
sections:
  - sectionId: header
    type: FREEMARKER
    templatePath: common:shared-header.ftl
```

Resolves to: `common-templates/templates/shared-header.ftl`

### Example 4: Template with Inheritance from Common

Template file: `tenant-a/templates/enrollment.yaml`

```yaml
baseTemplateId: common:base-enrollment.yaml
sections:
  - sectionId: custom-section
    type: FREEMARKER
    templatePath: tenant-specific.ftl
```

- Base template from: `common-templates/templates/base-enrollment.yaml`
- Tenant-specific template from: `tenant-a/templates/tenant-specific.ftl`

## Implementation Details

### NamespaceResolver Component

The `NamespaceResolver` utility class handles all namespace-aware path resolution:

- `resolveTemplatePath(namespace, templateId)` - Builds full path to YAML template
- `resolveResourcePath(resourcePath, namespace)` - Resolves resource path with `common:` prefix support
- `isCrossNamespaceReference(path)` - Checks if path uses `common:` prefix
- `normalizeNamespace(namespace)` - Normalizes namespace name (handles null/empty)

### RenderContext Enhancement

The `RenderContext` now stores the current namespace to enable namespace-aware resource resolution throughout the rendering pipeline:

```java
context.setNamespace("tenant-a");
String namespace = context.getNamespace();  // "tenant-a"
```

### Renderer Updates

- **FreeMarkerRenderer**: Resolves namespace-aware FTL template paths
- **AcroFormRenderer**: Resolves namespace-aware PDF form paths
- **TemplateLoader**: New overloaded methods for namespace-aware loading:
  - `loadTemplate(String namespace, String templateId)`
  - `loadTemplate(String namespace, String templateId, Map<String, Object> variables)`
  - `getNamespaceResourceBytes(String namespace, String resourcePath)`

## Migration Guide

### From Non-Namespaced to Namespaced System

**Before** (old request):
```json
{
  "templateId": "templates/enrollment-form.yaml",
  "data": { }
}
```

**After** (new request with namespace):
```json
{
  "namespace": "tenant-a",
  "templateId": "enrollment-form.yaml",
  "data": { }
}
```

Key changes:
1. Add `"namespace"` field to the request
2. Remove `"templates/"` prefix from `templateId` (it's added automatically)
3. Reorganize YAML/resource files into namespace folders

## Namespace Metadata

Template definitions can optionally include metadata about their namespace:

```yaml
templateId: tenant-a-enrollment
metadata:
  namespace: "tenant-a"
  region: "us-west"
  version: "v2"
  owner: "tenant-a-team"
```

This metadata is automatically populated during template loading.

## Best Practices

1. **Use common-templates for shared resources**: Store base templates, headers, footers, and common PDFs in `common-templates`.

2. **Minimize namespace duplication**: Use template inheritance (`baseTemplateId`) and cross-namespace references (`common:`) to avoid duplicating templates.

3. **Keep template IDs simple**: Within the namespace context, use simple IDs without path prefixes.

4. **Document namespace usage**: Clearly document which templates belong to which namespace for maintainability.

5. **Default namespace consideration**: If most requests use the same namespace, consider making it the default through configuration.

## Configuration

The system can be extended with configuration properties:

```properties
docgen.default-namespace=common-templates
docgen.namespace-enabled=true
```

## Troubleshooting

### Template Not Found

**Error**: `Template not found for id 'xyz.yaml'`

**Solution**: 
- Check that the template file exists in `{namespace}/templates/xyz.yaml`
- Verify the namespace spelling (case-sensitive)
- Ensure the request `namespace` field matches the folder name

### Cross-Namespace Reference Not Found

**Error**: `Template not found for id 'common:xyz.yaml'`

**Solution**:
- Check that the resource exists in `common-templates/templates/xyz.yaml`
- Verify the resource path after the `common:` prefix
- Check that all directory levels are correct

### Resource Load Failure

**Error**: `Failed to load resource: tenant-a/templates/xyz.pdf`

**Solution**:
- Verify the file exists and is readable
- Check path separators (use forward slashes `/`)
- For PDF forms, ensure they are valid PDF files with AcroForm fields

## Future Enhancements

Potential improvements to the namespace system:

1. **Namespace Access Control**: Add authentication/authorization per namespace
2. **Namespace-Specific Caching**: Configure cache TTL per namespace
3. **Namespace Configuration**: Allow per-namespace configuration properties
4. **Namespace Templates Registry**: Monitor and list all namespaces and their templates
5. **Template Fallback Chain**: Define inheritance chains across namespaces
