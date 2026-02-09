# Multi-Tenant Namespace System - Quick Start Guide

## 5-Minute Setup

### 1. Organize Your Templates

Create the following directory structure in `src/main/resources/`:

```
src/main/resources/
├── common-templates/templates/        # Shared templates
│   ├── base-enrollment.yaml
│   └── shared-header.ftl
│
├── tenant-a/templates/                # Tenant A templates
│   ├── enrollment-form.yaml
│   └── applicant-form.pdf
│
└── tenant-b/templates/                # Tenant B templates
    ├── enrollment-form.yaml
    └── applicant-form.pdf
```

### 2. Create a Simple YAML Template

Create `src/main/resources/tenant-a/templates/my-enrollment.yaml`:

```yaml
templateId: my-enrollment
sections:
  - sectionId: applicant-info
    type: ACROFORM
    templatePath: applicant-form.pdf
    order: 1
    mappingType: DIRECT
    fieldMappings:
      firstName: applicant.firstName
      lastName: applicant.lastName
      email: applicant.email
```

### 3. Make an API Request

**With namespace** (new way):

```bash
curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "tenant-a",
    "templateId": "my-enrollment.yaml",
    "data": {
      "applicant": {
        "firstName": "John",
        "lastName": "Doe",
        "email": "john@example.com"
      }
    }
  }'
```

**Without namespace** (uses common-templates):

```bash
curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d '{
    "templateId": "base-enrollment.yaml",
    "data": { "applicant": {...} }
  }'
```

## Key Concepts

### Namespaces
- **Namespace**: A folder name that groups templates (e.g., "tenant-a", "common-templates")
- **Default**: "common-templates" (if namespace is omitted in request)
- **Path**: Each namespace has its templates in `{namespace}/templates/` folder

### Resource References
- **Local**: `"forms/applicant.pdf"` → loads from `{currentNamespace}/templates/forms/applicant.pdf`
- **Cross-namespace**: `"common:forms/header.pdf"` → loads from `common-templates/templates/forms/header.pdf`

### File Organization
```
{namespace}/templates/
├── *.yaml             # Template definitions
├── *.ftl              # FreeMarker templates
├── *.pdf              # PDF forms
└── subdirs/           # Organized by type
    ├── forms/
    ├── styles/
    └── images/
```

## Common Patterns

### Pattern 1: Tenant-Specific with Shared Base

**common-templates/templates/base-enrollment.yaml**
```yaml
templateId: base-enrollment
sections:
  - sectionId: header
    type: FREEMARKER
    templatePath: common:header.ftl
```

**tenant-a/templates/enrollment.yaml**
```yaml
baseTemplateId: common:base-enrollment.yaml
sections:
  - sectionId: custom-form
    type: ACROFORM
    templatePath: tenant-specific-form.pdf
```

Request:
```json
{
  "namespace": "tenant-a",
  "templateId": "enrollment.yaml",
  "data": { }
}
```

### Pattern 2: Multiple Tenants with Base

Create one base template in `common-templates` and reuse it in multiple tenant-specific templates.

**File**: `common-templates/templates/base.yaml`
```yaml
sections:
  - sectionId: header
    type: FREEMARKER
    templatePath: header.ftl
  - sectionId: footer
    type: FREEMARKER
    templatePath: footer.ftl
```

**Files**: 
- `tenant-a/templates/enrollment.yaml` → `baseTemplateId: common:base.yaml`
- `tenant-b/templates/enrollment.yaml` → `baseTemplateId: common:base.yaml`

### Pattern 3: Cross-Tenant Resource Access

Tenant A references both its own and common templates:

```yaml
sections:
  - sectionId: company-header
    type: FREEMARKER
    templatePath: common:headers/company-standard.ftl
    
  - sectionId: tenant-content
    type: FREEMARKER
    templatePath: custom-content.ftl  # From tenant-a/templates/
```

## Testing

### Using Provided Example Files

1. **Common templates example**:
```bash
curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d @src/main/resources/example-request.json
```

2. **Tenant-specific example**:
```bash
curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d @src/main/resources/example-request-namespace.json
```

### Test File Locations

- `src/main/resources/example-request.json` - Uses default namespace
- `src/main/resources/example-request-namespace.json` - Uses "tenant-a" namespace

## Important Notes

1. **Case-sensitive**: Namespace and templateId are case-sensitive
2. **Path separators**: Always use forward slashes `/` in paths, not backslashes
3. **File extensions**: YAML files can be `.yaml` or `.yml`
4. **Templates folder**: The `templates/` folder is added automatically; don't include it in templateId
5. **Common prefix**: Use `common:` prefix for cross-namespace references

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Template not found" | Check namespace folder exists and file path is correct |
| "No AcroForm found" | Ensure PDF is a valid form with AcroForm fields |
| 404 error | Verify template file exists in `{namespace}/templates/` |
| Wrong data appearing | Check field mappings in YAML match your data structure |

## Next Steps

1. See [NAMESPACE_SYSTEM.md](NAMESPACE_SYSTEM.md) for detailed documentation
2. Check [MAPPING_STRATEGIES_README.md](MAPPING_STRATEGIES_README.md) for field mapping options
3. Review example templates in `tenant-a/templates/` and `common-templates/templates/`

## Architecture

```
DocumentGenerationRequest
  ├── namespace: "tenant-a"
  ├── templateId: "enrollment.yaml"
  └── data: {...}
        ↓
DocumentComposer.generateDocument()
        ↓
TemplateLoader.loadTemplate(namespace, templateId)
        ↓
NamespaceResolver.resolveTemplatePath()
  "tenant-a" + "enrollment.yaml" → "tenant-a/templates/enrollment.yaml"
        ↓
RenderContext (stores namespace: "tenant-a")
        ↓
SectionRenderer.render()
  ├── FreeMarkerRenderer: Resolves "common:header.ftl" → "common-templates/templates/header.ftl"
  └── AcroFormRenderer: Resolves "forms/applicant.pdf" → "tenant-a/templates/forms/applicant.pdf"
        ↓
Generate PDF
```
