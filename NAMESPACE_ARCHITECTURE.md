# Multi-Tenant Namespace Architecture

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      API Request                            │
├─────────────────────────────────────────────────────────────┤
│ {                                                           │
│   "namespace": "tenant-a",                                  │
│   "templateId": "enrollment-form.yaml",                     │
│   "data": { "applicant": { ... } }                         │
│ }                                                           │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
                 ┌─────────────────────────┐
                 │ DocumentController      │
                 │  /api/documents/generate│
                 └────────┬────────────────┘
                          │
                          ▼
            ┌──────────────────────────────┐
            │   DocumentComposer           │
            │  - Use namespace from request│
            │  - Pass to TemplateLoader    │
            └──────────┬───────────────────┘
                       │
         ┌─────────────┴──────────────────┐
         │                                │
         ▼                                ▼
┌──────────────────────┐   ┌─────────────────────────┐
│  TemplateLoader      │   │  NamespaceResolver      │
│  - loadTemplate()    │   │  - resolveTemplatePath()│
│  - caching           │   │  - resolveResourcePath()│
│                      │   │  - Support common: ref  │
└──────────┬───────────┘   └─────────────────────────┘
           │                         ▲
           │                         │
           │    NamespaceResolver    │
           │    (injected)           │
           └─────────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────┐
        │   Template Loading           │
        │   "tenant-a/templates/       │
        │    enrollment-form.yaml"     │
        └──────────┬───────────────────┘
                   │
                   ▼
      ┌────────────────────────────┐
      │   DocumentTemplate         │
      │  + namespace metadata      │
      │  + sections []             │
      └────────────┬───────────────┘
                   │
                   ▼
      ┌────────────────────────────┐
      │   RenderContext            │
      │  + template                │
      │  + data                    │
      │  + namespace: "tenant-a"   │ ◄── Namespace stored here
      └────────────┬───────────────┘
                   │
         ┌─────────┴──────────────┐
         │                        │
         ▼                        ▼
  ┌─────────────┐        ┌──────────────┐
  │ Renderers   │        │ Renderers    │
  ├─────────────┤        ├──────────────┤
  │ - FTL       │        │ - AcroForm   │
  │ - PDF       │        │ - PDFBox     │
  └────┬────────┘        └──────┬───────┘
       │                        │
       │  Use RenderContext     │
       │  .getNamespace()       │
       │                        │
       ▼                        ▼
┌─────────────────────────────────────────────────┐
│  NamespaceResolver (via Renderers)              │
│  - Resolve FTL path: "header.ftl"               │
│    → "tenant-a/templates/header.ftl"            │
│                                                 │
│  - Resolve PDF path: "common:base-form.pdf"    │
│    → "common-templates/templates/base-form.pdf"│
└────────────┬───────────────────────────────────┘
             │
             ▼
    ┌─────────────────────┐
    │ File System / Cache │
    │  - Load resources   │
    │  - Render PDF       │
    └─────────┬───────────┘
              │
              ▼
      ┌──────────────┐
      │  PDF Output  │
      └──────────────┘
```

## Directory Structure

```
src/main/resources/
│
├── common-templates/                       ◄── Shared by all tenants
│   └── templates/
│       ├── base-enrollment.yaml
│       ├── base-enrollment-CT.yaml
│       ├── composite-enrollment.yaml
│       ├── header.ftl
│       ├── footer.ftl
│       ├── forms/
│       │   ├── applicant.pdf
│       │   └── plans.pdf
│       └── styles/
│           └── ...
│
├── tenant-a/                               ◄── Tenant-specific
│   └── templates/
│       ├── base-enrollment.yaml
│       ├── base-enrollment-CT.yaml
│       ├── composite-enrollment.yaml
│       ├── enrollment-specific.ftl
│       ├── forms/
│       │   └── tenant-a-form.pdf
│       └── images/
│           └── ...
│
├── tenant-b/                               ◄── Tenant-specific
│   └── templates/
│       ├── enrollment-form.yaml
│       └── forms/
│           └── tenant-b-form.pdf
│
└── (other namespaces as needed)
```

## Request Flow with Namespace Resolution

```
API Request:
{
  "namespace": "tenant-a",
  "templateId": "enrollment-form.yaml",
  "data": { ... }
}
         │
         ▼
DocumentComposer.generateDocument(request)
         │
         ├─ request.getNamespace() = "tenant-a"
         ├─ request.getTemplateId() = "enrollment-form.yaml"
         └─ request.getData() = { ... }
                │
                ▼
TemplateLoader.loadTemplate("tenant-a", "enrollment-form.yaml", data)
                │
                ├─ NamespaceResolver.normalizeNamespace("tenant-a")
                │  → "tenant-a" (already normalized)
                │
                ├─ NamespaceResolver.resolveTemplatePath("tenant-a", "enrollment-form.yaml")
                │  → "tenant-a/templates/enrollment-form.yaml"
                │
                └─ loadRawTemplate("tenant-a/templates/enrollment-form.yaml")
                   → Loads from classpath
                   → Parses YAML
                   → Returns DocumentTemplate
                      │
                      └─ Template contains:
                         - sections[]
                         - metadata { _namespace: "tenant-a" }
                         - baseTemplateId: "common:base-enrollment.yaml"
                         - includedFragments: ["common:shared-footer.yaml"]
                                  │
                                  ▼
                         Handle inheritance/composition:
                         - baseTemplateId "common:base-enrollment.yaml"
                           → resolveTemplatePath("common-templates", "base-enrollment.yaml")
                           → "common-templates/templates/base-enrollment.yaml"
                         
                         - includedFragments "common:shared-footer.yaml"
                           → resolveTemplatePath("common-templates", "shared-footer.yaml")
                           → "common-templates/templates/shared-footer.yaml"
                                  │
                                  ▼
                         Return merged DocumentTemplate
                │
                ▼
RenderContext context = new RenderContext(template, data)
context.setNamespace("tenant-a")
                │
                ▼
For each PageSection in template.getSections():
                │
    ┌───────────├────────────────────┐
    │           │                    │
    ▼           ▼                    ▼
[FreeMarker] [AcroForm]       [PDFBox Component]
    │           │                    │
    ├─ section.getTemplatePath() = "header.ftl"
    │
    ├─ NamespaceResolver.resolveResourcePath("header.ftl", "tenant-a")
    │  → "tenant-a/templates/header.ftl"
    │
    └─ freemarkerConfig.getTemplate("tenant-a/templates/header.ftl")
       → CustomFreeMarkerTemplateLoader
       → TemplateLoader.getResourceBytes("tenant-a/templates/header.ftl")
       → Return FTL content & render
    
    Or with cross-namespace reference:
    ├─ section.getTemplatePath() = "common:shared-header.ftl"
    │
    ├─ NamespaceResolver.resolveResourcePath("common:shared-header.ftl", "tenant-a")
    │  → Detects "common:" prefix
    │  → "common-templates/templates/shared-header.ftl"
    │
    └─ freemarkerConfig.getTemplate("common-templates/templates/shared-header.ftl")
       → CustomFreeMarkerTemplateLoader
       → TemplateLoader.getResourceBytes("common-templates/templates/shared-header.ftl")
       → Return FTL content & render
                │
                ▼
Merge all section PDFs together
                │
                ▼
Apply headers/footers
                │
                ▼
Return PDF bytes
```

## Namespace Resolution Logic

### Template Path Resolution

```java
// Request: namespace="tenant-a", templateId="enrollment-form.yaml"
// Step 1: Normalize namespace
String normalized = namespaceResolver.normalizeNamespace("tenant-a");
// Result: "tenant-a"

// Step 2: Resolve path
String path = namespaceResolver.resolveTemplatePath("tenant-a", "enrollment-form.yaml");
// Result: "tenant-a/templates/enrollment-form.yaml"

// Step 3: Load template
DocumentTemplate template = loadRawTemplate(path);
```

### Resource Path Resolution

```java
// Scenario 1: Local resource (no prefix)
String resourcePath = "forms/applicant.pdf";
String resolved = namespaceResolver.resolveResourcePath(resourcePath, "tenant-a");
// Result: "tenant-a/templates/forms/applicant.pdf"

// Scenario 2: Cross-namespace reference (with prefix)
String resourcePath = "common:forms/base.pdf";
String resolved = namespaceResolver.resolveResourcePath(resourcePath, "tenant-a");
// Result: "common-templates/templates/forms/base.pdf"

// Scenario 3: Cross-namespace with subdirectories
String resourcePath = "common:shared/styles/header.ftl";
String resolved = namespaceResolver.resolveResourcePath(resourcePath, "tenant-a");
// Result: "common-templates/templates/shared/styles/header.ftl"
```

## Component Dependencies

```
DocumentComposer
    │
    ├── TemplateLoader
    │   └── NamespaceResolver
    │
    └── SectionRenderer[]
        ├── FreeMarkerRenderer
        │   └── NamespaceResolver
        │
        └── AcroFormRenderer
            └── NamespaceResolver

RenderContext
    └── Contains namespace (string)

NamespaceResolver
    ├── resolveTemplatePath()
    ├── resolveResourcePath()
    ├── normalizeNamespace()
    ├── isCrossNamespaceReference()
    └── extractResourcePath()
```

## Path Resolution Rules Summary

| Scenario | Input | Output | Notes |
|----------|-------|--------|-------|
| Template in current namespace | "enrollment.yaml" | "{namespace}/templates/enrollment.yaml" | Normal case |
| Template in common-templates | (namespace=null) | "common-templates/templates/X.yaml" | Default namespace |
| FTL in current namespace | "header.ftl" | "{namespace}/templates/header.ftl" | Renderer uses context |
| FTL in common-templates | "common:header.ftl" | "common-templates/templates/header.ftl" | Explicit cross-ref |
| PDF in subdirectory | "forms/applicant.pdf" | "{namespace}/templates/forms/applicant.pdf" | Supports nesting |
| Base template inheritance | "common:base.yaml" | "common-templates/templates/base.yaml" | Template level |
| Fragment inclusion | "common:shared-section.yaml" | "common-templates/templates/shared-section.yaml" | Composition |

## Caching Strategy

```
TemplateLoader caching:
    @Cacheable(value = "documentTemplates", key = "{ #namespace, #templateId }")
    public DocumentTemplate loadTemplate(String namespace, String templateId)

TemplateLoader.getResourceBytes() caching:
    @Cacheable(value = "rawResources", key = "#path")
    public byte[] getResourceBytes(String path)

Custom FreeMarker TemplateLoader:
    - Delegates to TemplateLoader.getResourceBytes()
    - Inherits caching benefits
```

## Error Handling

```
Exception Scenarios:

1. Template Not Found
   Input: namespace="invalid-ns", templateId="unknown.yaml"
   → TemplateLoadingException: TEMPLATE_NOT_FOUND
   → Message includes both namespace and templateId

2. Cross-Namespace Resource Not Found
   Input: templatePath="common:nonexistent.ftl"
   → TemplateLoadingException: TEMPLATE_NOT_FOUND
   → Resolved to: "common-templates/templates/nonexistent.ftl"

3. Invalid PDF Form
   Input: templatePath="broken-form.pdf"
   → IOException caught in AcroFormRenderer
   → Wrapped with section context

4. Namespace Normalization
   Input: namespace="  TENANT-A  "
   → Normalized to: "TENANT-A"
   → Still case-sensitive
```

## Sequence Diagram: Complete Request Flow

```
Client
  │
  └─ POST /api/documents/generate
     {namespace: "tenant-a", templateId: "enrollment.yaml", data: {...}}
        │
        ▼
     DocumentController
        │
        └─ requestBody → DocumentGenerationRequest
           (namespace="tenant-a", templateId="enrollment.yaml")
              │
              ▼
           DocumentComposer.generateDocument()
              │
              ├─ TemplateLoader.loadTemplate("tenant-a", "enrollment.yaml", request.getData())
              │  │
              │  ├─ NamespaceResolver: normalize "tenant-a" → "tenant-a"
              │  │
              │  ├─ NamespaceResolver: resolve path → "tenant-a/templates/enrollment.yaml"
              │  │
              │  └─ Load YAML, parse, handle inheritance/composition
              │     │
              │     └─ For baseTemplateId: "common:base.yaml"
              │        │
              │        ├─ NamespaceResolver: resolve → "common-templates/templates/base.yaml"
              │        │
              │        └─ Merge templates
              │
              ├─ RenderContext(template, data)
              │  └─ context.setNamespace("tenant-a")
              │
              └─ For each section in template.sections:
                 │
                 ├─ Condition check
                 │
                 ├─ Select Renderer based on section.type
                 │  │
                 │  ├─ FreeMarkerRenderer.render()
                 │  │  │
              │  │  ├─ section.templatePath = "tenant-specific.ftl"
                 │  │  │
                 │  │  ├─ NamespaceResolver.resolveResourcePath("tenant-specific.ftl", "tenant-a")
                 │  │  │  → "tenant-a/templates/tenant-specific.ftl"
                 │  │  │
                 │  │  ├─ freemarkerConfig.getTemplate("tenant-a/templates/tenant-specific.ftl")
                 │  │  │  → CustomFreeMarkerTemplateLoader
                 │  │  │  → TemplateLoader.getResourceBytes("tenant-a/templates/tenant-specific.ftl")
                 │  │  │
                 │  │  ├─ Process FTL with ViewModel
                 │  │  │
                 │  │  └─ Return HTML → PDF
                 │  │
                 │  └─ AcroFormRenderer.render()
                 │     │
                 │     ├─ section.templatePath = "common:base-form.pdf"
                 │     │
                 │     ├─ NamespaceResolver.resolveResourcePath("common:base-form.pdf", "tenant-a")
                 │     │  → "common-templates/templates/base-form.pdf"
                 │     │
                 │     ├─ TemplateLoader.getResourceBytes("common-templates/templates/base-form.pdf")
                 │     │
                 │     ├─ PDDocument.load(pdfBytes)
                 │     │
                 │     ├─ Map field values using strategies
                 │     │
                 │     └─ Return PDF with filled fields
                 │
                 └─ Collect all section PDFs
                    │
                    ├─ Merge PDFs
                    │
                    ├─ Apply headers/footers
                    │
                    └─ Convert to bytes
                       │
                       ▼
                    Return PDF to Client
```
