# Document Generation Flow - Complete Class Guide

## Overview

When you submit a document generation request, approximately **20+ classes** work together in a coordinated flow to transform your JSON data and PDF templates into a final PDF document. This guide explains each class and its role.

---

## High-Level Architecture

```
HTTP Request
    ↓
Controller Layer (REST endpoint)
    ↓
Service Layer (Orchestration & coordination)
    ↓
Template Loading & Resolution
    ↓
Data Transformation (ViewModels, Mapping)
    ↓
Rendering Layer (PDF generation)
    ↓
Post-Processing (Headers/Footers)
    ↓
HTTP Response (PDF binary)
```

---

## Complete Class Flow (Step by Step)

### **STEP 1: REST Request Reception**

#### Class: `DocumentController` 
**File:** `controller/DocumentController.java`  
**Role:** HTTP entry point  
**Key Method:** `generateDocument(@RequestBody DocumentGenerationRequest request)`

```java
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentComposer documentComposer;
    
    @PostMapping("/generate")
    public ResponseEntity<?> generateDocument(@RequestBody DocumentGenerationRequest request) {
        // 1. Receives JSON request
        // 2. Calls DocumentComposer
        // 3. Returns PDF response
    }
}
```

**Responsibilities:**
- Receives HTTP POST request
- Deserializes JSON into `DocumentGenerationRequest`
- Delegates to `DocumentComposer`
- Handles exceptions (returns error responses)
- Serializes PDF as HTTP response

---

### **STEP 2: Orchestration & Coordination**

#### Class: `DocumentComposer`
**File:** `service/DocumentComposer.java`  
**Role:** MAIN ORCHESTRATOR - coordinates the entire flow  
**Key Method:** `generateDocument(DocumentGenerationRequest request): byte[]`

```java
@Service
@RequiredArgsConstructor
public class DocumentComposer {
    private final List<SectionRenderer> renderers;
    private final List<FieldMappingStrategy> mappingStrategies;
    private final TemplateLoader templateLoader;
    private final HeaderFooterProcessor headerFooterProcessor;
    
    public byte[] generateDocument(DocumentGenerationRequest request) {
        // 1. Loads template
        // 2. Creates RenderContext
        // 3. Processes sections
        // 4. Merges PDFs
        // 5. Applies headers/footers
        // 6. Converts to bytes
    }
}
```

**Responsibilities:**
- **Orchestrates** the entire document generation pipeline
- Loads templates
- Creates RenderContext
- Routes sections to appropriate renderers
- Merges PDFs
- Applies post-processing
- Returns byte array

**Design Philosophy:** 
> "Intentionally thin - it coordinates but delegates heavy work to other services"

---

### **STEP 3: Template Loading & Resolution**

#### Class: `TemplateLoader`
**File:** `service/TemplateLoader.java`  
**Role:** Template acquisition and parsing  
**Key Methods:**
- `loadTemplate(String namespace, String templateId, Map data): DocumentTemplate`
- `loadTemplate(String templateId): DocumentTemplate`

```java
@Component
public class TemplateLoader {
    private final ObjectMapper yamlMapper;
    private final NamespaceResolver namespaceResolver;
    
    @Cacheable(value = "templates", key = "#templateId")
    public DocumentTemplate loadTemplate(String templateId, Map<String, Object> data) {
        // 1. Resolves file path (namespace-aware)
        // 2. Loads YAML from file/classpath/remote
        // 3. Parses YAML → DocumentTemplate
        // 4. Resolves fragments
        // 5. Handles inheritance
        // 6. Interpolates placeholders
    }
}
```

**Responsibilities:**
- Loads YAML template files from:
  - Classpath
  - Filesystem
  - Remote Spring Cloud Config Server
- Parses YAML into Java objects
- Resolves template fragments (includes)
- Handles template inheritance
- Interpolates placeholders
- Supports namespace-aware loading (multi-tenant)
- Caches templates for performance

**Output:** `DocumentTemplate` object containing all sections

---

#### Class: `DocumentTemplate` (Model)
**File:** `model/DocumentTemplate.java`  
**Role:** Data model for parsed template

```java
@Data
public class DocumentTemplate {
    private List<PageSection> sections;
    private HeaderFooterConfig headerFooterConfig;
    // ... other properties
}
```

Contains: All sections with their mappings, styles, and configurations

---

#### Class: `NamespaceResolver`
**File:** `service/NamespaceResolver.java`  
**Role:** Multi-tenant namespace resolution  

Resolves paths like:
- Template: `common-templates/templates/forms/my-form.yaml`
- Resource: `tenant-a/resource/image.png`
- Fragment: `namespace/fragments/shared-section.yaml`

---

### **STEP 4: Request Context Building**

#### Class: `RenderContext`
**File:** `core/RenderContext.java`  
**Role:** Data carrier for rendering operations

```java
@Data
public class RenderContext {
    private DocumentTemplate template;
    private Map<String, Object> data;
    private String namespace;
    private String currentSectionId;
    // ... tracking properties
}
```

**Purpose:** Single source of truth for all rendering operations
- Carries template structure
- Carries request data
- Carries namespace for resource resolution
- Tracks current section being rendered

---

### **STEP 5: Section Processing & Rendering**

#### Class: `PageSection` (Model)
**File:** `model/PageSection.java`  
**Role:** Configuration for a single section

```java
@Data
public class PageSection {
    private String sectionId;
    private SectionType type;              // ACROFORM, FREEMARKER, etc.
    private String templatePath;
    private List<FieldMappingGroup> fieldMappingGroups;
    private String viewModelType;          // For ViewModel transformation
    private String condition;              // Conditional rendering
    private List<OverflowConfig> overflowConfigs;
}
```

---

#### **Interface: `SectionRenderer`**
**File:** `renderer/SectionRenderer.java`  
**Role:** Abstraction for section rendering

```java
public interface SectionRenderer {
    PDDocument render(PageSection section, RenderContext context);
    boolean supports(SectionType type);
}
```

**Multiple Implementations:**

---

#### **1. `AcroFormRenderer` (For PDF Forms)**
**File:** `renderer/AcroFormRenderer.java`  
**Handles:** `SectionType.ACROFORM`

```java
@Component
public class AcroFormRenderer implements SectionRenderer {
    private final List<FieldMappingStrategy> mappingStrategies;
    private final TemplateLoader templateLoader;
    private final ViewModelFactory viewModelFactory;
    
    @Override
    public PDDocument render(PageSection section, RenderContext context) {
        // 1. Load PDF form template
        // 2. Apply ViewModel transformation (if specified)
        // 3. Map data to fields
        // 4. Apply field styles
        // 5. Return PDDocument
    }
}
```

**Flow for AcroForm:**
```
PDF Form Template
    ↓
Load with PDFBox
    ↓
Extract AcroForm fields
    ↓
Apply ViewModel (optional)
    ↓
Map data to fields
    ↓
Apply styles (bold, color, etc.)
    ↓
Return PDDocument
```

---

#### **2. `FreeMarkerRenderer` (For Template-based Content)**
**File:** `renderer/FreeMarkerRenderer.java`  
**Handles:** `SectionType.FREEMARKER`

```java
@Component
public class FreeMarkerRenderer implements SectionRenderer {
    private final Configuration freemarkerConfig;
    private final ViewModelFactory viewModelFactory;
    
    @Override
    public PDDocument render(PageSection section, RenderContext context) {
        // 1. Apply ViewModel transformation
        // 2. Load FTL template file
        // 3. Process template with data
        // 4. Convert HTML → PDF
        // 5. Return PDDocument
    }
}
```

**Flow for FreeMarker:**
```
FreeMarker Template (.ftl)
    ↓
Apply ViewModel
    ↓
Parse FTL directives
    ↓
Merge with data
    ↓
Generate HTML
    ↓
Convert HTML → PDF
    ↓
Return PDDocument
```

---

#### **3. `PdfBoxDirectSectionRenderer` (For Direct PDFBox Components)**
**File:** `renderer/PdfBoxDirectSectionRenderer.java`  
**Handles:** `SectionType.PDFBOX_COMPONENT`

Allows registering custom PDFBox renderers for specialized content.

---

### **STEP 6: Data Transformation (ViewModel)**

#### Class: `ViewModelFactory`
**File:** `viewmodel/ViewModelFactory.java`  
**Role:** Factory for creating ViewModels

```java
@Component
@RequiredArgsConstructor
public class ViewModelFactory {
    private final List<ViewModelBuilder<?>> builders;
    
    public Object createViewModel(String viewModelType, Map<String, Object> rawData) {
        // 1. Looks up ViewModelBuilder by name
        // 2. Calls builder.build(rawData)
        // 3. Returns transformed object
    }
}
```

**How it works:**
- Spring auto-discovers all `ViewModelBuilder` implementations
- Registers them by class name (without "Builder" suffix)
- On demand, creates ViewModel from raw data

---

#### **Interface: `ViewModelBuilder<T>`**
**File:** `viewmodel/ViewModelBuilder.java`

```java
public interface ViewModelBuilder<T> {
    T build(Map<String, Object> rawData);
}
```

**Implementations:**

---

#### **`AcroFormViewModelBuilder`**
**File:** `viewmodel/AcroFormViewModelBuilder.java`

```java
@Component
public class AcroFormViewModelBuilder implements ViewModelBuilder<AcroFormViewModel> {
    @Override
    public AcroFormViewModel build(Map<String, Object> rawData) {
        // Transforms nested data:
        // - Flattens objects
        // - Concatenates fields
        // - Computes derived values
        // - Handles nulls safely
        
        return AcroFormViewModel.builder()
            .primaryFullName(computed)
            .primaryAge(calculated)
            .isSenior(flag)
            .build();
    }
}
```

---

#### Class: `AcroFormViewModel`
**File:** `viewmodel/AcroFormViewModel.java`  
**Role:** Flattened data model for form fields

```java
@Data @Builder
public class AcroFormViewModel {
    private String primaryFullName;        // Computed
    private String primaryDateOfBirth;      // Formatted
    private Integer primaryAge;             // Calculated
    private Boolean isSenior;               // Derived flag
    private Integer dependentCount;         // Aggregated
    // ... many more fields
}
```

---

### **STEP 7: Field Mapping**

#### **Interface: `FieldMappingStrategy`**
**File:** `mapper/FieldMappingStrategy.java`

```java
public interface FieldMappingStrategy {
    Map<String, String> mapData(Map<String, Object> data, Map<String, String> mappings);
    Object evaluatePath(Map<String, Object> data, String path);
}
```

**Three Implementations:**

---

#### **1. `DirectMappingStrategy`**
Maps simple dot-notation paths:
```
data.applicant.firstName → "John"
```

---

#### **2. `JsonPathMappingStrategy`**
Maps JSONPath expressions:
```
$.applicants[0].demographic.firstName → "John"
```

---

#### **3. `JsonataMappingStrategy`**
Maps complex JSONata expressions:
```
applicants[type='PRIMARY'].demographic.firstName & ' ' & lastName
```

---

#### Class: `FieldMappingGroup` (Model)
**File:** `model/FieldMappingGroup.java`

```java
@Data
public class FieldMappingGroup {
    private MappingType mappingType;       // DIRECT, JSONPATH, JSONATA
    private Map<String, String> fields;    // PDF field → data path
    private Map<String, FieldStyling> fieldStyles;  // Styling
}
```

---

### **STEP 8: Field Styling**

#### Class: `FieldStyling` (Model)
**File:** `model/FieldStyling.java`

```java
@Data @Builder
public class FieldStyling {
    private Float fontSize;
    private Integer textColor;
    private Integer backgroundColor;
    private Integer borderColor;
    private Boolean bold;
    private Boolean italic;
    private Boolean readOnly;
    private TextAlignment alignment;
    // ... etc
}
```

---

#### In `AcroFormRenderer.fillFormFields()`

```java
private void fillFormFields(PDAcroForm acroForm, Map<String, String> fieldValues, PageSection section) {
    for (String fieldName : fieldValues.keySet()) {
        // 1. Set field value
        PDField field = acroForm.getField(fieldName);
        field.setValue(fieldValues.get(fieldName));
        
        // 2. Apply styling if configured
        if (allFieldStyles.containsKey(fieldName)) {
            applyFieldStyling(acroForm, field, allFieldStyles.get(fieldName));
        }
    }
}
```

---

### **STEP 9: PDF Assembly**

#### Class: `PDDocument` (PDFBox)
Represents a PDF in memory

Each SectionRenderer returns a `PDDocument`

---

#### Class: `PDFMergerUtility` (PDFBox)
Merges multiple PDFs into one

```java
private PDDocument mergeSections(List<PDDocument> sectionDocuments) {
    if (sectionDocuments.isEmpty()) return new PDDocument();
    if (sectionDocuments.size() == 1) return sectionDocuments.get(0);
    
    PDFMergerUtility merger = new PDFMergerUtility();
    for (PDDocument doc : sectionDocuments) {
        merger.appendDocument(doc);
    }
    return merger.getDestinationDocument();
}
```

---

### **STEP 10: Post-Processing**

#### Class: `HeaderFooterProcessor`
**File:** `processor/HeaderFooterProcessor.java`  
**Role:** Adds headers/footers to all pages

```java
@Component
public class HeaderFooterProcessor {
    public void apply(PDDocument document, HeaderFooterConfig config, Map<String, Object> data) {
        // 1. Iterate through all pages
        // 2. Create content streams
        // 3. Add header content
        // 4. Add footer content
    }
}
```

**Implementations:**
- `PdfBoxHeaderFooterRenderer` - Direct PDFBox rendering
- `FreeMarkerHeaderFooterRenderer` - FreeMarker template rendering

---

### **STEP 11: Serialization**

```java
private byte[] convertToBytes(PDDocument document) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    document.save(baos);
    return baos.toByteArray();
}
```

---

### **STEP 12: HTTP Response**

```java
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_PDF);
headers.setContentDispositionFormData("attachment", "document.pdf");

return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
```

---

## Complete Class Reference

### **Controller & Models**
| Class | Package | Purpose |
|-------|---------|---------|
| `DocumentController` | `controller` | REST endpoint |
| `DocumentGenerationRequest` | `model` | HTTP request DTO |
| `DocumentTemplate` | `model` | Parsed YAML structure |
| `PageSection` | `model` | Single section config |

### **Orchestration**
| Class | Package | Purpose |
|-------|---------|---------|
| `DocumentComposer` | `service` | Main orchestrator |
| `RenderContext` | `core` | Data carrier for rendering |

### **Template Loading**
| Class | Package | Purpose |
|-------|---------|---------|
| `TemplateLoader` | `service` | Load & parse YAML |
| `NamespaceResolver` | `service` | Multi-tenant path resolution |

### **Rendering**
| Class | Package | Purpose |
|-------|---------|---------|
| `SectionRenderer` (interface) | `renderer` | Abstraction for renderers |
| `AcroFormRenderer` | `renderer` | PDF form rendering |
| `FreeMarkerRenderer` | `renderer` | FreeMarker template rendering |
| `PdfBoxDirectSectionRenderer` | `renderer` | PDFBox component rendering |

### **Data Mapping**
| Class | Package | Purpose |
|-------|---------|---------|
| `FieldMappingStrategy` (interface) | `mapper` | Abstraction for mapping |
| `DirectMappingStrategy` | `mapper` | Simple dot notation |
| `JsonPathMappingStrategy` | `mapper` | JSONPath expressions |
| `JsonataMappingStrategy` | `mapper` | JSONata expressions |
| `FieldMappingGroup` | `model` | Mapping group config |

### **ViewModels & Transformation**
| Class | Package | Purpose |
|-------|---------|---------|
| `ViewModelFactory` | `viewmodel` | Factory for builders |
| `ViewModelBuilder` (interface) | `viewmodel` | Builder abstraction |
| `AcroFormViewModelBuilder` | `viewmodel` | Transforms for forms |
| `AcroFormViewModel` | `viewmodel` | Flattened form data |

### **Field Styling**
| Class | Package | Purpose |
|-------|---------|---------|
| `FieldStyling` | `model` | Styling configuration |
| `TextAlignment` | `model` | Alignment enum |

### **Post-Processing**
| Class | Package | Purpose |
|-------|---------|---------|
| `HeaderFooterProcessor` (interface) | `processor` | Header/footer abstraction |
| `PdfBoxHeaderFooterRenderer` | `processor` | PDFBox rendering |
| `FreeMarkerHeaderFooterRenderer` | `processor` | FreeMarker rendering |

### **PDF Operations (PDFBox)**
| Class | Library | Purpose |
|-------|---------|---------|
| `PDDocument` | PDFBox | In-memory PDF document |
| `PDAcroForm` | PDFBox | PDF form handling |
| `PDField` | PDFBox | Individual form field |
| `PDFMergerUtility` | PDFBox | PDF merging |

---

## Class Interaction Summary

```
Request Flow:
DocumentController
    ↓ delegates to
DocumentComposer
    ├─ calls TemplateLoader        → DocumentTemplate
    ├─ creates RenderContext
    ├─ for each PageSection:
    │  ├─ selects SectionRenderer
    │  ├─ if viewModelType set:
    │  │  ├─ calls ViewModelFactory
    │  │  └─ gets transformed data
    │  ├─ calls FieldMappingStrategy  → field values
    │  └─ calls renderer.render()    → PDDocument
    ├─ merges PDDocuments
    ├─ calls HeaderFooterProcessor
    └─ converts to bytes
        ↓ returns to
DocumentController
    ↓
HTTP Response
```

---

## Key Design Patterns Used

### **1. Strategy Pattern**
- `SectionRenderer` interface with multiple implementations
- `FieldMappingStrategy` interface with DIRECT/JSONPATH/JSONATA

### **2. Factory Pattern**
- `ViewModelFactory` creates ViewModels
- Spring's implicit factory for `SectionRenderer` beans

### **3. Decorator Pattern**
- `RenderContext` decorates request data with template
- `HeaderFooterProcessor` decorates merged PDF

### **4. Template Method Pattern**
- `DocumentComposer.generateDocument()` orchestrates steps
- Each renderer implements same `render()` contract

### **5. Builder Pattern**
- `FieldStyling.builder()` for fluent configuration
- `DocumentTemplate.builder()`, `AcroFormViewModel.builder()`

---

## Data Flow Types

### **Type 1: Simple Mapping**
```
Raw JSON → FieldMappingStrategy → String values → PDDocument
```

### **Type 2: With ViewModel**
```
Raw JSON → ViewModelFactory → ViewModel → FieldMappingStrategy → String values → PDDocument
```

### **Type 3: With Styling**
```
FieldStyling → FieldStylingApplier → PDF field properties
```

---

## Exception Handling

| Exception | Thrown by | Caught by |
|-----------|-----------|-----------|
| `TemplateLoadingException` | TemplateLoader | DocumentComposer → DocumentController |
| `ResourceLoadingException` | TemplateLoader | DocumentComposer → DocumentController |
| `IOException` | PDFBox operations | DocumentComposer |

---

## Performance Considerations

**Caching:**
- `TemplateLoader` caches parsed templates
- `HeaderFooterProcessor` caches FreeMarker templates

**Memory:**
- `PDDocument` held in memory (stream-based for large PDFs supported)
- Multiple sections kept in-memory during merge

**Lazy Loading:**
- ViewModels created only if `viewModelType` specified
- Sections skipped if condition evaluates to false

---

## Extension Points

Where you can add custom functionality:

1. **Custom Section Renderer**
   - Implement `SectionRenderer`
   - Return your custom `PDDocument`

2. **Custom ViewModel**
   - Implement `ViewModelBuilder<YourViewModel>`
   - Annotate with `@Component`

3. **Custom Field Mapping Strategy**
   - Implement `FieldMappingStrategy`
   - Auto-discovered by DocumentComposer

4. **Custom Header Footer Processor**
   - Implement `HeaderFooterProcessor` interface
   - Configure which one to use

---

## Summary

**Major Components:**
- **Controller:** Receives HTTP requests
- **DocumentComposer:** Orchestrates entire flow
- **TemplateLoader:** Loads & parses YAML
- **SectionRenderer(s):** Render each section type
- **ViewModelFactory:** Transforms data
- **FieldMappingStrategy:** Maps data to fields
- **HeaderFooterProcessor:** Post-processes PDF
- **PDFBox:** Underlying PDF library

**4 Main Phases:**
1. **Load:** Template loaded and parsed
2. **Transform:** Data transformed via ViewModels
3. **Render:** Each section rendered to PDF
4. **Assemble:** PDFs merged and post-processed

**Key Insight:** Each class has a single, focused responsibility. The orchestrator (DocumentComposer) coordinates them, but delegates actual work to specialized components.
