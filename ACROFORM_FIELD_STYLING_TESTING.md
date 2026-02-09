# AcroForm Field Styling - Testing Guide

## Testing Methods

There are three ways to test the field styling feature:

1. **Manual Testing** - Generate a PDF and verify in a viewer (easiest)
2. **Integration Testing** - Use REST API to generate documents
3. **Unit Testing** - Write Java tests for styling logic

---

## Method 1: Manual Testing (Recommended for Getting Started)

### Prerequisites
- Running application (Spring Boot)
- PDF viewer (Adobe Reader, Preview, etc.)
- Test template YAML file
- Test PDF form

### Step 1: Create a Test Template YAML

Create file: `src/main/resources/test-styling-templates/styling-demo.yaml`

```yaml
sections:
  - sectionId: styled-form
    type: ACROFORM
    templatePath: test-styling-templates/demo-form.pdf
    
    fieldMappingGroups:
      # Group 1: Required fields (red, bold)
      - mappingType: DIRECT
        fields:
          requiredField1: data.firstName
          requiredField2: data.lastName
        
        fieldStyles:
          requiredField1:
            bold: true
            textColor: 0xFF0000  # Red
            fontSize: 12
          
          requiredField2:
            bold: true
            textColor: 0xFF0000  # Red
            fontSize: 12
      
      # Group 2: Read-only calculated fields
      - mappingType: DIRECT
        defaultStyle:
          readOnly: true
          backgroundColor: 0xF0F0F0
          italic: true
        
        fields:
          calculatedTotal: data.total
          generatedId: data.id
        
        fieldStyles:
          calculatedTotal:
            fontSize: 14
            bold: true
```

### Step 2: Create a Test PDF Form

1. **Option A: Use an Existing Form**
   - Download a sample AcroForm PDF from the web
   - Place in `src/main/resources/test-styling-templates/demo-form.pdf`
   - Extract field names and use them in YAML

2. **Option B: Create a Simple Form**
   - Use Adobe Acrobat, LibreOffice, or iText to create a form PDF
   - Add text fields for testing:
     - `requiredField1`
     - `requiredField2`
     - `calculatedTotal`
     - `generatedId`

### Step 3: Test Data JSON

Create test request JSON: `test-styling-request.json`

```json
{
  "templateId": "styling-demo.yaml",
  "namespace": "common-templates",
  "data": {
    "firstName": "John",
    "lastName": "Doe",
    "total": "1,234.56",
    "id": "APP-2026-001"
  }
}
```

### Step 4: Generate PDF via API

Using curl:

```bash
curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d @test-styling-request.json \
  -o generated-document.pdf
```

Using REST client (Postman/Insomnia):
1. POST to `http://localhost:8080/api/documents/generate`
2. Body (JSON):
```json
{
  "templateId": "styling-demo.yaml",
  "namespace": "common-templates",
  "data": {
    "firstName": "John",
    "lastName": "Doe",
    "total": "1,234.56",
    "id": "APP-2026-001"
  }
}
```
3. Save response as `generated-document.pdf`

### Step 5: Verify in PDF Viewer

Open `generated-document.pdf` in Adobe Reader or Preview:

| Field | Expected | What to Look For |
|-------|----------|------------------|
| `requiredField1` | Red, bold, 12pt | Text is RED color, BOLD weight |
| `requiredField2` | Red, bold, 12pt | Text is RED color, BOLD weight |
| `calculatedTotal` | Light gray background, italic, bold, 14pt | Gray background, ITALIC text, BOLD, larger font |
| `generatedId` | Light gray background, italic, read-only | Gray background, ITALIC, can't edit |

### Troubleshooting Manual Testing

| Issue | Cause | Fix |
|-------|-------|-----|
| PDF not generated | API not running | Start app: `mvn spring-boot:run` |
| Fields not populated | Wrong template ID | Check template name matches |
| Styling not visible | Wrong field names in YAML | Extract actual field names from PDF |
| Colors not showing | Invalid hex format | Use `0xRRGGBB` format |

---

## Method 2: Integration Testing via REST API

### Test Multiple Styling Scenarios

Create test script: `test-styling.sh`

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"
ENDPOINT="/api/documents/generate"

# Test 1: Required fields styling
echo "Test 1: Required fields styling..."
curl -X POST "$BASE_URL$ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{
    "templateId": "styling-demo.yaml",
    "namespace": "common-templates",
    "data": {
      "firstName": "John",
      "lastName": "Doe",
      "total": "1000.00",
      "id": "TEST-001"
    }
  }' \
  -o test-output-1.pdf

echo "Generated: test-output-1.pdf"

# Test 2: Different data values
echo "Test 2: Different styling test..."
curl -X POST "$BASE_URL$ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{
    "templateId": "styling-demo.yaml",
    "namespace": "common-templates",
    "data": {
      "firstName": "Jane",
      "lastName": "Smith",
      "total": "5000.00",
      "id": "TEST-002"
    }
  }' \
  -o test-output-2.pdf

echo "Generated: test-output-2.pdf"

echo "Done! Check test-output-*.pdf for styling"
```

Run tests:
```bash
chmod +x test-styling.sh
./test-styling.sh
```

---

## Method 3: Unit Testing

### Add Unit Test for Field Styling

Create test class: `src/test/java/com/example/demo/docgen/model/FieldStylingTest.java`

```java
package com.example.demo.docgen.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FieldStyling model
 */
public class FieldStylingTest {
    
    @Test
    public void testFieldStylingBuilder() {
        FieldStyling styling = FieldStyling.builder()
            .bold(true)
            .fontSize(12f)
            .textColor(0xFF0000)
            .alignment(FieldStyling.TextAlignment.CENTER)
            .build();
        
        assertTrue(styling.getBold());
        assertEquals(12f, styling.getFontSize());
        assertEquals(0xFF0000, styling.getTextColor());
        assertEquals(FieldStyling.TextAlignment.CENTER, styling.getAlignment());
    }
    
    @Test
    public void testFieldStylingDefaults() {
        FieldStyling styling = new FieldStyling();
        
        assertFalse(styling.getBold());
        assertFalse(styling.getItalic());
        assertFalse(styling.getReadOnly());
        assertFalse(styling.getHidden());
    }
    
    @Test
    public void testTextAlignmentCodes() {
        assertEquals(0, FieldStyling.TextAlignment.LEFT.code);
        assertEquals(1, FieldStyling.TextAlignment.CENTER.code);
        assertEquals(2, FieldStyling.TextAlignment.RIGHT.code);
    }
    
    @Test
    public void testColorValues() {
        // Test color hex conversion
        int red = 0xFF0000;
        int green = 0x00FF00;
        int blue = 0x0000FF;
        int white = 0xFFFFFF;
        int black = 0x000000;
        
        assertEquals(255, (red >> 16) & 0xFF);
        assertEquals(255, (green >> 8) & 0xFF);
        assertEquals(255, blue & 0xFF);
        assertEquals(0, (black >> 16) & 0xFF);
    }
}
```

Run test:
```bash
mvn test -Dtest=FieldStylingTest
```

### Add Integration Test for Field Mapping Group Styling

Create test class: `src/test/java/com/example/demo/docgen/model/FieldMappingGroupStylingTest.java`

```java
package com.example.demo.docgen.model;

import com.example.demo.docgen.mapper.MappingType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FieldMappingGroup with styling
 */
public class FieldMappingGroupStylingTest {
    
    @Test
    public void testFieldMappingGroupWithStyles() {
        FieldStyling redBold = FieldStyling.builder()
            .textColor(0xFF0000)
            .bold(true)
            .fontSize(12f)
            .build();
        
        FieldStyling readOnlyGray = FieldStyling.builder()
            .readOnly(true)
            .backgroundColor(0xF0F0F0)
            .build();
        
        Map<String, FieldStyling> fieldStyles = new HashMap<>();
        fieldStyles.put("field1", redBold);
        fieldStyles.put("field2", readOnlyGray);
        
        FieldMappingGroup group = FieldMappingGroup.builder()
            .mappingType(MappingType.DIRECT)
            .fields(Map.of(
                "field1", "data.value1",
                "field2", "data.value2"
            ))
            .fieldStyles(fieldStyles)
            .build();
        
        assertEquals(2, group.getFieldStyles().size());
        assertTrue(group.getFieldStyles().containsKey("field1"));
        assertTrue(group.getFieldStyles().get("field1").getBold());
    }
    
    @Test
    public void testDefaultStyleOverride() {
        FieldStyling defaultStyle = FieldStyling.builder()
            .fontSize(11f)
            .textColor(0x000000)
            .build();
        
        FieldStyling override = FieldStyling.builder()
            .fontSize(14f)
            .textColor(0xFF0000)
            .build();
        
        Map<String, FieldStyling> fieldStyles = new HashMap<>();
        fieldStyles.put("specialField", override);
        
        FieldMappingGroup group = FieldMappingGroup.builder()
            .mappingType(MappingType.DIRECT)
            .defaultStyle(defaultStyle)
            .fieldStyles(fieldStyles)
            .fields(Map.of("specialField", "data.value"))
            .build();
        
        assertEquals(11f, group.getDefaultStyle().getFontSize());
        assertEquals(14f, group.getFieldStyles().get("specialField").getFontSize());
    }
}
```

Run test:
```bash
mvn test -Dtest=FieldMappingGroupStylingTest
```

---

## Method 4: Full Integration Test with Real PDF

### Create End-to-End Test

Create test in: `src/test/java/com/example/demo/StylingIntegrationTest.java`

```java
package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for field styling feature
 */
@SpringBootTest
@AutoConfigureMockMvc
public class StylingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGenerateDocumentWithFieldStyling() throws Exception {
        String requestJson = """
            {
              "templateId": "styling-demo.yaml",
              "namespace": "common-templates",
              "data": {
                "firstName": "John",
                "lastName": "Doe",
                "total": "1234.56",
                "id": "APP-001"
              }
            }
            """;

        mockMvc.perform(post("/api/documents/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
        
        // PDF is generated without errors
        // Content verification would require PDF parsing
    }

    @Test
    public void testStyledFieldsAreApplied() throws Exception {
        // This would require:
        // 1. Generate PDF
        // 2. Parse PDF with PDFBox
        // 3. Extract field styling attributes
        // 4. Assert values match configuration
        
        // Example (pseudocode):
        // PDDocument doc = PDDocument.load(pdfBytes);
        // PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
        // PDField field = form.getField("requiredField1");
        // COSArray bgColor = (COSArray) field.getCOSObject().getItem(COSName.BG);
        // assert bgColor represents correct color
    }
}
```

Run test:
```bash
mvn test -Dtest=StylingIntegrationTest
```

---

## Complete Test Template

Create comprehensive test template: `src/main/resources/test-styling-templates/comprehensive-styling-test.yaml`

```yaml
sections:
  - sectionId: comprehensive-styling-demo
    type: ACROFORM
    templatePath: test-styling-templates/demo-form.pdf
    
    fieldMappingGroups:
      # Group 1: Required fields - Red, Bold
      - mappingType: DIRECT
        basePath: null
        fields:
          firstName: applicant.firstName
          lastName: applicant.lastName
        
        fieldStyles:
          firstName:
            bold: true
            textColor: 0xFF0000
            fontSize: 12
          
          lastName:
            bold: true
            textColor: 0xFF0000
            fontSize: 12
      
      # Group 2: Read-only fields - Gray background
      - mappingType: DIRECT
        defaultStyle:
          fontSize: 11
          readOnly: true
          backgroundColor: 0xF0F0F0
        
        fields:
          applicationId: applicant.id
          generatedDate: applicant.createdAt
        
        fieldStyles:
          applicationId:
            fontSize: 10
            textColor: 0x666666
          
          generatedDate:
            fontSize: 9
            italic: true
      
      # Group 3: Calculated/Derived - Yellow highlight
      - mappingType: DIRECT
        fields:
          total: summary.total
          count: summary.itemCount
        
        fieldStyles:
          total:
            bold: true
            fontSize: 14
            backgroundColor: 0xFFFFCC
            readOnly: true
          
          count:
            fontSize: 12
            alignment: CENTER
            backgroundColor: 0xFFFFCC
      
      # Group 4: Optional/Info - Normal with hint
      - mappingType: DIRECT
        defaultStyle:
          fontSize: 11
          textColor: 0x000000
        
        fields:
          email: applicant.email
          phone: applicant.phone
          notes: applicant.notes
```

Test data:
```json
{
  "templateId": "comprehensive-styling-test.yaml",
  "namespace": "common-templates",
  "data": {
    "applicant": {
      "firstName": "Alice",
      "lastName": "Johnson",
      "id": "APP-2026-12345",
      "createdAt": "2026-02-09",
      "email": "alice@example.com",
      "phone": "555-1234"
    },
    "summary": {
      "total": "$5,678.90",
      "itemCount": 23
    }
  }
}
```

---

## Verification Checklist

After generating a PDF with styling, verify:

### Text Styling
- [ ] Required fields are **bold** and **red** (0xFF0000)
- [ ] Font sizes differ as configured (12pt for required, 11pt for others)
- [ ] Italic text appears slanted

### Visual Styling
- [ ] Read-only fields have light gray background (0xF0F0F0)
- [ ] Calculated fields have yellow background (0xFFFFCC)
- [ ] Colors are vibrant and match configuration

### Field Behavior
- [ ] Read-only fields cannot be edited
- [ ] Editable fields can be modified
- [ ] All data values are populated correctly

### Alignment
- [ ] Centered fields are centered
- [ ] Left-aligned fields are left-aligned

---

## Debugging Tips

### Enable Logging

Add to `application.properties`:
```properties
logging.level.com.example.demo.docgen.renderer.AcroFormRenderer=DEBUG
```

Look for log messages like:
```
Applied text color to field: firstName
Applied background color to field: total
Applied read-only flag to field: applicationId
```

### PDF Viewer Tips

- **Adobe Reader**: Shows all field properties in Form > Edit Fields menu
- **Preview (Mac)**: Double-click field to see properties
- **Linux (PDFTk)**: Use `pdftk document.pdf dump_data_fields` to inspect

### Extract Field Information from PDF

```bash
# Using pdftotext (requires poppler-utils)
pdftotext -layout generated-document.pdf

# Using pdftk (requires pdftk)
pdftk generated-document.pdf dump_data_fields | grep FieldName
```

---

## Quick Test Checklist

- [ ] Application is running (`mvn spring-boot:run`)
- [ ] Test template YAML created
- [ ] Test PDF form file exists with named fields
- [ ] Test data JSON created
- [ ] API endpoint is `/api/documents/generate`
- [ ] Generated PDF saved successfully
- [ ] PDF opens in viewer without errors
- [ ] Field values are populated correctly
- [ ] Field styling is visible (colors, fonts, alignment)

---

## Next Steps

1. **Start simple**: Test one field with one style property
2. **Expand gradually**: Add more fields and style properties
3. **Verify in viewer**: Use Adobe Reader or Preview
4. **Check logs**: Enable DEBUG logging to see styling application
5. **Iterate**: Adjust styles and regenerate to find best appearance

---

## Common Test Patterns

### Pattern 1: Minimal Test
```yaml
fieldMappingGroups:
  - mappingType: DIRECT
    fields:
      singleField: data.value
    
    fieldStyles:
      singleField:
        bold: true
```

### Pattern 2: Default with Override
```yaml
fieldMappingGroups:
  - mappingType: DIRECT
    defaultStyle:
      fontSize: 11
    fields:
      field1: data1
      field2: data2
    fieldStyles:
      field1:
        fontSize: 14
```

### Pattern 3: Multiple Groups
```yaml
fieldMappingGroups:
  - mappingType: DIRECT
    fields:
      required1: data1
    fieldStyles:
      required1:
        bold: true
  
  - mappingType: DIRECT
    defaultStyle:
      readOnly: true
    fields:
      readonly1: data2
```

