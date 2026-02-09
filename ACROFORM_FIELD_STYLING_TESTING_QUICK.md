# AcroForm Field Styling - Getting Started with Testing

## Quick Start (5 minutes)

### 1. Start the Application

```bash
# Terminal 1: Start the Spring Boot application
cd /workspaces/demos
mvn spring-boot:run
```

Wait for output showing:
```
Started DemoApplication in X.XXX seconds
```

### 2. Make a Test Request via curl

**Terminal 2:** Create test request and send it

```bash
# Create test request
cat > /tmp/styling-test.json << 'EOF'
{
  "templateId": "enrollment-application.yaml",
  "namespace": "common-templates",
  "data": {
    "applicationId": "APP-2026-001",
    "applicationDate": "2026-02-09",
    "status": "PENDING",
    "applicants": [
      {
        "type": "PRIMARY",
        "demographic": {
          "firstName": "John",
          "lastName": "Doe",
          "middleName": "Robert",
          "dateOfBirth": "1990-01-15",
          "gender": "M",
          "ssn": "123-45-6789",
          "email": "john.doe@example.com",
          "phone": "555-123-4567"
        },
        "addresses": [
          {
            "type": "HOME",
            "street": "123 Main St",
            "city": "Springfield",
            "state": "IL",
            "zipCode": "62701"
          }
        ]
      }
    ]
  }
}
EOF

# Send request
curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d @/tmp/styling-test.json \
  -o /tmp/test-output.pdf

# Check if file was created
ls -lh /tmp/test-output.pdf
```

### 3. Open the Generated PDF

```bash
# macOS
open /tmp/test-output.pdf

# Linux
xdg-open /tmp/test-output.pdf

# Windows
start /tmp/test-output.pdf
```

---

## Detailed Testing Walkthrough

### Test 1: Verify Basic Document Generation

**Goal**: Ensure the API works without styling

```bash
curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d '{
    "templateId": "enrollment-application.yaml",
    "namespace": "common-templates",
    "data": {
      "applicationId": "TEST-001",
      "applicationDate": "2026-02-09",
      "status": "ACTIVE",
      "applicants": [
        {"type": "PRIMARY", "demographic": {"firstName": "Test", "lastName": "User"}}
      ]
    }
  }' \
  -o test-basic.pdf

echo "✅ Generated test-basic.pdf"
echo "   Check that document was created without errors"
```

### Test 2: Create a Styled Template

**Create file**: `src/main/resources/common-templates/templates/styling-test.yaml`

```yaml
sections:
  - sectionId: styled-section
    type: ACROFORM
    templatePath: templates/forms/applicant-form.pdf
    
    fieldMappingGroups:
      # Group 1: Required fields - Red and bold
      - mappingType: JSONATA
        fields:
          applicantName: "applicants[type='PRIMARY'].demographic.firstName & ' ' & applicants[type='PRIMARY'].demographic.lastName"
          email: "applicants[type='PRIMARY'].demographic.email"
        
        fieldStyles:
          applicantName:
            bold: true
            textColor: 0xFF0000
            fontSize: 12
          
          email:
            textColor: 0xFF0000
            fontSize: 11
      
      # Group 2: Read-only fields with gray background
      - mappingType: DIRECT
        defaultStyle:
          readOnly: true
          backgroundColor: 0xF0F0F0
          italic: true
        
        fields:
          applicationId: applicationId
          submissionDate: applicationDate
        
        fieldStyles:
          applicationId:
            fontSize: 10
          submissionDate:
            fontSize: 10
```

**Send request to use styled template:**

```bash
curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d '{
    "templateId": "styling-test.yaml",
    "namespace": "common-templates",
    "data": {
      "applicationId": "APP-STYLED-001",
      "applicationDate": "2026-02-09",
      "applicants": [
        {
          "type": "PRIMARY",
          "demographic": {
            "firstName": "Alice",
            "lastName": "Johnson",
            "email": "alice@example.com"
          }
        }
      ]
    }
  }' \
  -o test-styled.pdf

echo "Generated test-styled.pdf"
```

### Test 3: Verify Styling in PDF Viewer

**In Adobe Reader**, check:

1. **Required Fields** (should be RED and BOLD):
   - applicantName
   - email
   
2. **Read-Only Fields** (should have GRAY background):
   - applicationId
   - submissionDate
   - Try to edit: fields should be non-editable

3. **Font Sizes**:
   - applicantName: 12pt (larger)
   - email: 11pt
   - Others: 10pt (smaller)

### Test 4: Batch Testing

**Create script**: `test-batch.sh`

```bash
#!/bin/bash

for i in {1..3}; do
  echo "Generating test document $i..."
  
  curl -s -X POST http://localhost:8080/api/documents/generate \
    -H "Content-Type: application/json" \
    -d "{
      \"templateId\": \"styling-test.yaml\",
      \"namespace\": \"common-templates\",
      \"data\": {
        \"applicationId\": \"BATCH-$i\",
        \"applicationDate\": \"2026-02-09\",
        \"applicants\": [
          {
            \"type\": \"PRIMARY\",
            \"demographic\": {
              \"firstName\": \"User$i\",
              \"lastName\": \"Test\",
              \"email\": \"user$i@test.com\"
            }
          }
        ]
      }
    }" \
    -o "test-output-$i.pdf"
  
  echo "✅ Generated test-output-$i.pdf"
done

echo "All tests completed. Open PDFs to verify styling."
```

Run it:
```bash
chmod +x test-batch.sh
./test-batch.sh
```

---

## Verification Using PDF Inspection Tools

### Option 1: Using PDFBox Command Line (Java)

Create test class to inspect PDF:

**File**: `src/test/java/com/example/demo/PdfInspectionTest.java`

```java
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.junit.jupiter.api.Test;

import java.io.File;

public class PdfInspectionTest {
    
    @Test
    public void inspectGeneratedPdf() throws Exception {
        // Open the generated PDF
        PDDocument document = PDDocument.load(new File("test-styled.pdf"));
        PDAcroForm form = document.getDocumentCatalog().getAcroForm();
        
        if (form != null) {
            System.out.println("PDF Form Fields:");
            for (PDField field : form.getFields()) {
                System.out.println("  Field: " + field.getFullyQualifiedName());
                System.out.println("    Value: " + field.getValue());
                // Check for styling attributes
                System.out.println("    BG (background): " + 
                    field.getCOSObject().getItem("BG"));
                System.out.println("    Q (alignment): " + 
                    field.getCOSObject().getItem("Q"));
                System.out.println("    Ff (flags): " + 
                    field.getCOSObject().getInt("Ff", 0));
            }
        }
        
        document.close();
    }
}
```

Run it:
```bash
mvn test -Dtest=PdfInspectionTest -e
```

### Option 2: Using PDF Text Tools

```bash
# Extract field information from PDF
pdftotext -layout test-styled.pdf - | head -20

# Or using pdftk (if installed)
pdftk test-styled.pdf dump_data_fields
```

---

## Automated Testing Using RestTemplate

**Create test**: `src/test/java/com/example/demo/StylingApiTest.java`

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class StylingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testStyledDocumentGeneration() throws Exception {
        String requestJson = """
            {
              "templateId": "styling-test.yaml",
              "namespace": "common-templates",
              "data": {
                "applicationId": "TEST-001",
                "applicationDate": "2026-02-09",
                "applicants": [{
                  "type": "PRIMARY",
                  "demographic": {
                    "firstName": "Test",
                    "lastName": "User",
                    "email": "test@example.com"
                  }
                }]
              }
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/documents/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andReturn();

        byte[] pdfContent = result.getResponse().getContentAsByteArray();
        System.out.println("Generated PDF: " + pdfContent.length + " bytes");
        
        // Save for manual inspection
        java.nio.file.Files.write(
            java.nio.file.Paths.get("/tmp/test-from-api.pdf"),
            pdfContent
        );
    }
}
```

Run it:
```bash
mvn test -Dtest=StylingApiTest
```

---

## Testing Checklist

### Before Testing
- [ ] Application is running (`mvn spring-boot:run`)
- [ ] Test template YAML file exists
- [ ] PDF form file exists with correct field names
- [ ] Logs are configured (optional, for DEBUG output)

### During Testing
- [ ] API request succeeds (HTTP 200)
- [ ] PDF file is generated
- [ ] File size is reasonable (> 1KB)

### After Generating PDF
- [ ] PDF opens in viewer without errors
- [ ] All data fields are populated with values
- [ ] Required fields appear in RED and BOLD
- [ ] Read-only fields have GRAY background
- [ ] Fonts sizes match configuration
- [ ] Alignment is correct (centered, left)
- [ ] Try to edit fields:
  - Editable fields: can type new text
  - Read-only fields: cannot type (locked)

### Styling Verification
- [ ] **Colors**: RGB hex values display as expected
  - RED (0xFF0000): Bright red
  - GRAY (0xF0F0F0): Light gray background
  - Others: Match the hex values configured
  
- [ ] **Text Properties**:
  - Bold text: Appears in heavier weight
  - Italic text: Appears slanted
  - Font sizes: Visually different between fields
  
- [ ] **Alignment**:
  - CENTER: Text centered in field
  - LEFT: Text left-aligned
  - RIGHT: Text right-aligned
  
- [ ] **Field Behavior**:
  - Read-only: Cannot select or edit
  - Editable: Can click and type
  - Hidden: Text masked (if configured)

---

## Common Test Scenarios

### Scenario 1: Simple Single-Field Styling

```bash
curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d '{
    "templateId": "simple.yaml",
    "namespace": "common-templates",
    "data": {"name": "Test User"}
  }' -o simple-test.pdf
```

### Scenario 2: Multiple Styles in One Template

```bash
curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d '{
    "templateId": "multi-style.yaml",
    "namespace": "common-templates",
    "data": {
      "required1": "Required Value",
      "required2": "Another Required",
      "readOnly1": "Read-Only Value",
      "readOnly2": "Calculated Total"
    }
  }' -o multi-style-test.pdf
```

### Scenario 3: Conditional Styling via Multiple Templates

```bash
# Template 1: For priority applications
curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d '{
    "templateId": "priority.yaml",
    "namespace": "common-templates",
    "data": {"isPriority": true, "name": "Priority Application"}
  }' -o priority-test.pdf

# Template 2: For regular applications
curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d '{
    "templateId": "standard.yaml",
    "namespace": "common-templates",
    "data": {"isPriority": false, "name": "Standard Application"}
  }' -o standard-test.pdf
```

---

## Debugging

### Enable Debug Logging

Add to `src/main/resources/application.properties`:
```properties
logging.level.com.example.demo.docgen.renderer.AcroFormRenderer=DEBUG
logging.level.com.example.demo.docgen.model=DEBUG
```

Then check console output for messages like:
```
Applied text color to field: firstName
Applied background color to field: total
Applied read-only flag to field: id
```

### Check Field Names in PDF

If styling isn't applied, verify field names match:

```bash
# Extract field names from PDF template
pdftotext test-styled.pdf | grep -i "field\|name"

# Or use PDFBox:
java -jar pdfbox-app.jar ExtractText test-styled.pdf -
```

---

## Next Steps After Testing

1. **Review Generated PDFs**: Open in viewer and visually verify
2. **Modify Styling**: Change colors, fonts, try different combinations
3. **Create Production Templates**: Build actual templates for your forms
4. **Automate Testing**: Create test suite for regression testing
5. **Document Styles**: Add comments to YAML explaining color choices

---

## Troubleshooting Test Failures

| Problem | Solution |
|---------|----------|
| "Server not running" | Start app with `mvn spring-boot:run` |
| "Template not found" | Verify template file path and namespace |
| "PDF not generated" | Check application logs for errors |
| "Styling not visible" | Verify field names match in YAML |
| "Colors look wrong" | Check hex color format (0xRRGGBB) |
| "Read-only not working" | Open in different PDF viewer (Adobe Reader recommended) |

