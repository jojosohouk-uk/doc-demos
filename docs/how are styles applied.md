## Implementation Overview

Here are the key components involved in the styling feature:

---

## 1. **Data Models** (Define styling structure)

### FieldStyling.java
The core class that holds all styling properties:
- `fontSize`, `textColor`, `backgroundColor`, `borderColor`, `borderWidth`
- `fontName` (Helvetica, Times-Roman, Courier)
- `bold`, `italic`, `readOnly`, `hidden`
- `alignment` (LEFT, CENTER, RIGHT)

### FieldMappingGroup.java
Contains field mappings AND their associated styles:
```java
@Builder.Default
private Map<String, FieldStyling> fieldStyles = new HashMap<>();

private FieldStyling defaultStyle;  // Optional default styling for all fields in group
```

### PageSection.java
Holds one or more field mapping groups:
```java
private List<FieldMappingGroup> fieldMappingGroups;
```

---

## 2. **Configuration Loading** (Parse YAML)

### TemplateLoader.java
Parses YAML templates using Jackson with YAMLFactory:
```java
private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
// Deserializes YAML → DocumentTemplate → PageSection → FieldMappingGroup → FieldStyling
```

**Flow:**
```
styling-test.yaml
    ↓
Jackson YAML Parser
    ↓
DocumentTemplate object
    ↓
PageSection (with FieldMappingGroups)
    ↓
FieldMappingGroup (with fieldStyles Map)
    ↓
FieldStyling objects populated from YAML
```

---

## 3. **Field Value Mapping**

### AcroFormRenderer.java - `mapFieldValues()` method
Maps data from your JSON request to PDF form field names using:
- **DIRECT**: Simple dot notation (e.g., "applicant.firstName")
- **JSONPATH**: JSONPath expressions (e.g., "$.applicant.firstName")
- **JSONATA**: Complex expressions (e.g., "applicants[type='PRIMARY'].demographic.firstName")

**Example mapping:**
```yaml
fields:
  firstName: applicants[type='PRIMARY'].demographic.firstName
  email: applicants[0].demographic.email

# Result: {firstName: "John", email: "john@example.com"}
```

---

## 4. **Style Application** (Core Logic)

### AcroFormRenderer.java - Main methods:

#### `render(PageSection section, RenderContext context)` - Lines 64-94
```java
public PDDocument render(PageSection section, RenderContext context) {
    // 1. Load PDF template
    PDDocument document = loadTemplate(section.getTemplatePath(), context);
    
    // 2. Get AcroForm from PDF
    PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
    
    // 3. Map field values from request data
    Map<String, String> fieldValues = mapFieldValues(section, context);
    
    // 4. Fill fields AND apply styles
    fillFormFields(acroForm, fieldValues, section);
    
    return document;
}
```

#### `fillFormFields(PDAcroForm acroForm, Map<String, String> fieldValues, PageSection section)` - Lines 330-365
```java
private void fillFormFields(PDAcroForm acroForm, Map<String, String> fieldValues, PageSection section) {
    // 1. Collect ALL styles from all mapping groups
    Map<String, FieldStyling> allFieldStyles = new HashMap<>();
    for (FieldMappingGroup group : section.getFieldMappingGroups()) {
        allFieldStyles.putAll(group.getFieldStyles());
    }
    
    // 2. For each field value to set
    for (Map.Entry<String, String> entry : fieldValues.entrySet()) {
        PDField field = acroForm.getField(fieldName);
        
        // 3. Set the value
        field.setValue(value);
        
        // 4. If styles exist for this field, apply them
        if (allFieldStyles.containsKey(fieldName)) {
            applyFieldStyling(acroForm, field, allFieldStyles.get(fieldName));
        }
    }
}
```

#### `applyFieldStyling(PDAcroForm acroForm, PDField field, FieldStyling styling)` - Lines 368-478
This is where the magic happens! It:

**Step 1: Select Font** (Lines 384-393)
```java
String fontName = styling.getFontName() != null ? styling.getFontName().toLowerCase() : "helvetica";
PDFont pdfFont;

if (fontName.contains("times")) {
    pdfFont = bold ? PDType1Font.TIMES_BOLD : PDType1Font.TIMES_ROMAN;
} else if (fontName.contains("courier")) {
    pdfFont = bold ? PDType1Font.COURIER_BOLD : PDType1Font.COURIER;
} else {
    pdfFont = bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
}
```

**Step 2: Build Default Appearance String** (Lines 397-416)
The "DA" (Default Appearance) string is PDF's way of storing font, size, and color:
```java
da.append('/').append(fontResName.getName()).append(' ');
da.append(styling.getFontSize()).append(" Tf ");  // Font + size

// Add text color in RGB format
if (styling.getTextColor() != null) {
    float r = ((styling.getTextColor() >> 16) & 0xFF) / 255.0f;
    float g = ((styling.getTextColor() >> 8) & 0xFF) / 255.0f;
    float b = (styling.getTextColor() & 0xFF) / 255.0f;
    da.append(String.format("%s %s %s rg", r, g, b));  // Red, Green, Blue
}

field.getCOSObject().setString(COSName.DA, da.toString());
```

**Step 3: Apply Background Color** (Lines 419-427)
```java
if (styling.getBackgroundColor() != null) {
    // Convert hex (0xRRGGBB) to RGB floats
    // Build COSArray [r, g, b]
    // Set on field's "BG" property
    field.getCOSObject().setItem(COSName.getPDFName("BG"), bgColor);
}
```

**Step 4: Apply Border Color** (Lines 431-439)
```java
if (styling.getBorderColor() != null) {
    // Similar to background, set "BC" property
    field.getCOSObject().setItem(COSName.getPDFName("BC"), borderColor);
}
```

**Step 5: Apply Text Alignment** (Lines 451-453)
```java
if (styling.getAlignment() != null) {
    // Set "Q" property: 0=LEFT, 1=CENTER, 2=RIGHT
    field.getCOSObject().setItem(COSName.getPDFName("Q"), 
        new COSFloat(styling.getAlignment().code));
}
```

**Step 6: Apply Field Flags** (Lines 460-470)
```java
int currentFlags = field.getCOSObject().getInt(COSName.FF, 0);

if (styling.getReadOnly() != null && styling.getReadOnly()) {
    currentFlags |= 1;  // Set bit 0 (ReadOnly flag)
}

if (styling.getHidden() != null && styling.getHidden()) {
    currentFlags |= (1 << 12);  // Set bit 12 (Password/Hidden flag)
}

field.getCOSObject().setItem(COSName.FF, new COSFloat(currentFlags));
```

---

## 5. **PDF Properties Being Set**

When you configure styling, this code modifies these PDF field properties:

| Property | Code Name | What It Does | Example |
|----------|-----------|--------------|---------|
| DA (Default Appearance) | COSName.DA | Font, size, text color | `/F1 12 Tf 1 0 0 rg` |
| BG (Background) | `/BG` | Background color | `[0.95 0.95 0.95]` (light gray) |
| BC (Border Color) | `/BC` | Border color | `[0.5 0.5 0.5]` |
| Q (Quadding) | `/Q` | Text alignment | `0` (left), `1` (center), `2` (right) |
| FF (Field Flags) | COSName.FF | Read-only, hidden, etc. | Bit flags |

These are standard PDF AcroForm properties understood by Adobe Reader and other PDF viewers.

---

## 6. **Complete Data Flow Example**

```yaml
# YAML Template
fieldMappingGroups:
  - mappingType: JSONATA
    fields:
      firstName: "applicants[type='PRIMARY'].demographic.firstName"
    fieldStyles:
      firstName:
        bold: true
        textColor: 0xFF0000      # Red
        fontSize: 12
```

**Request:**
```json
{
  "applicants": [
    {
      "type": "PRIMARY",
      "demographic": {
        "firstName": "John"
      }
    }
  ]
}
```

**Processing:**
1. **TemplateLoader** parses YAML → Creates `FieldMappingGroup` with `fieldStyles` map
2. **FieldMapper** evaluates expression → `{firstName: "John"}`
3. **fillFormFields** finds PDF field "firstName"
4. **PDField.setValue("John")** fills the field with text
5. **applyFieldStyling** applies the `FieldStyling` object:
   - Selects `PDType1Font.HELVETICA_BOLD` (because bold=true)
   - Builds DA string: `/F1 12 Tf 1 0 0 rg` (font, size, red color)
   - Sets field properties in PDF

**Result:** PDF field "firstName" displays "John" in **bold red text at 12pt**

---

## 7. **Key Classes Summary**

| Class | Location | Purpose |
|-------|----------|---------|
| **FieldStyling** | model/FieldStyling.java | Defines all styling properties |
| **FieldMappingGroup** | model/FieldMappingGroup.java | Groups fields with mapping strategy + styles |
| **PageSection** | model/PageSection.java | Section config with mapping groups |
| **TemplateLoader** | service/TemplateLoader.java | Parses YAML → Java objects |
| **AcroFormRenderer** | renderer/AcroFormRenderer.java | Main renderer: maps data + applies styles |

---

## 8. **Testing**

The feature is tested in:
- AcroFormRendererTest.java - Style application tests
- Tests verify all styling properties are correctly set on PDF fields
- ✅ 106 tests passing (no regressions)