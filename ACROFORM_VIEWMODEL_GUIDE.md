# AcroForm ViewModel Development Guide

## Overview

This guide explains how to use **ViewModels with AcroForm PDF field rendering** to transform complex nested data into clean, structured data suitable for form fields.

### What Problem Does This Solve?

Before ViewModels, developers had to write complex JSONATA expressions to extract and transform data:

```yaml
# ❌ Hard to read, maintain, and debug
fields:
  fullName: "$.applicants[type='PRIMARY'].demographic.firstName & ' ' & $.applicants[type='PRIMARY'].demographic.middleName & ' ' & $.applicants[type='PRIMARY'].demographic.lastName"
  familySize: "$.applicants | $count($)"
  senior: "$applicants[type='PRIMARY'].demographic.dateOfBirth > '1960-02-11'"
```

**With ViewModels:**

```yaml
# ✅ Clean, simple, and maintainable
viewModelType: AcroForm
fieldMappingGroups:
  - mappingType: DIRECT
    fields:
      fullName: "primaryFullName"        # Computed in Java
      familySize: "familySize"           # Calculated in Java
      senior: "isSenior"                 # Derived flag in Java
```

---

## Architecture

### Component Flow

```
1. Raw JSON Request
        ↓
2. AcroFormRenderer.render()
        ↓
3. Check if viewModelType is specified
        ↓
4. ViewModelFactory.createViewModel()
        ↓
5. AcroFormViewModelBuilder.build()
        ↓
6. Flattened AcroFormViewModel
        ↓
7. Field Mapping (DIRECT/JSONPATH/JSONATA)
        ↓
8. PDF Form Population & Styling
        ↓
9. Output PDF
```

### Key Components

| Component | File | Purpose |
|-----------|------|---------|
| **ViewModelFactory** | `viewmodel/ViewModelFactory.java` | Locates and instantiates ViewModelBuilder |
| **ViewModelBuilder** | `viewmodel/ViewModelBuilder.java` | Interface for all builders |
| **AcroFormViewModelBuilder** | `viewmodel/AcroFormViewModelBuilder.java` | Transforms data for AcroForm |
| **AcroFormViewModel** | `viewmodel/AcroFormViewModel.java` | POJO holding transformed data |
| **AcroFormRenderer** | `renderer/AcroFormRenderer.java` | Uses ViewModel before field mapping |

---

## Real-World Scenarios

### Scenario 1: Enrollment Form - Simple Flattening

**Problem:** Nested demographic data must be flattened for form fields

**Data Structure:**
```json
{
  "applicants": [{
    "type": "PRIMARY",
    "demographic": {
      "firstName": "John",
      "lastName": "Doe",
      "dateOfBirth": "1985-03-15",
      "ssn": "123-45-6789"
    },
    "addresses": [{
      "street": "123 Main St",
      "city": "Boston",
      "state": "MA",
      "zipCode": "02101"
    }]
  }]
}
```

**Form Fields Expected:**
- Field "applicantName": `John Doe`
- Field "applicantDOB": `03/15/1985`
- Field "applicantAddress": `123 Main St, Boston, MA 02101`

**YAML Template:**
```yaml
sections:
  - sectionId: personal-info
    type: ACROFORM
    templatePath: forms/enrollment.pdf
    viewModelType: AcroForm  # ← Uses AcroFormViewModelBuilder
    
    fieldMappingGroups:
      - mappingType: DIRECT
        fields:
          applicantName: "primaryFullName"        # John Doe
          applicantDOB: "primaryDateOfBirth"      # 03/15/1985
          applicantAddress: "primaryAddress"      # 123 Main St, Boston, MA 02101
        
        fieldStyles:
          applicantName:
            bold: true
            fontSize: 12
            textColor: 0x000000
```

**What AcroFormViewModelBuilder Does:**
```java
// Extract nested data
String firstName = demographic.get("firstName")        // "John"
String lastName = demographic.get("lastName")          // "Doe"
String dob = demographic.get("dateOfBirth")            // "1985-03-15"

// Flatten & transform
String primaryFullName = firstName + " " + lastName     // "John Doe"
String primaryDateOfBirth = formatDate(dob)            // "03/15/1985"
String primaryAddress = street + ", " + city + ", " 
    + state + " " + zipCode                            // "123 Main St, Boston, MA 02101"
```

**Result:** Form fields populated with clean, formatted data without complex expressions.

---

### Scenario 2: Family Application - Computed Flags

**Problem:** Need to conditionally show/style form sections based on data

**Data Structure:**
```json
{
  "applicants": [
    {"type": "PRIMARY", "demographic": {"firstName": "John"}},
    {"type": "SPOUSE", "demographic": {"firstName": "Jane"}},
    {"type": "DEPENDENT", "demographic": {"firstName": "Alice"}},
    {"type": "DEPENDENT", "demographic": {"firstName": "Bob"}}
  ]
}
```

**Form Requirements:**
- Show spouse section only if spouse exists
- Display number of dependents
- Red text if family size > 4
- Flag as "LARGE_FAMILY" if dependents > 3

**YAML Template:**
```yaml
sections:
  - sectionId: family-info
    type: ACROFORM
    templatePath: forms/family.pdf
    viewModelType: AcroForm
    
    fieldMappingGroups:
      - mappingType: DIRECT
        fields:
          spouseName: "spouseFullName"
          dependentCount: "dependentCount"
          familySize: "familySize"
          largeFamily: "isLargeFamily"              # Computed flag
        
        fieldStyles:
          spouseName:
            readOnly: "$.spouseFullName == null"    # Hide if no spouse
          dependentCount:
            textColor: "$.isLargeFamily ? 0xFF0000 : 0x000000"  # Red if large family
```

**What AcroFormViewModelBuilder Does:**
```java
// Count applicants by type
List<Map> dependents = applicants.stream()
    .filter(a -> "DEPENDENT".equals(a.getType()))
    .collect(toList());

int dependentCount = dependents.size();      // 2
int familySize = applicants.size();          // 4
boolean isLargeFamily = familySize > 4;      // false (but would be true with 5+)
boolean hasSpouse = applicants.stream()
    .anyMatch(a -> "SPOUSE".equals(a.getType()));  // true
```

**Result:** 
- Clean, type-safe computed flags
- No complex JSONATA conditionals
- Easy to test and maintain

---

### Scenario 3: Senior Applicant Recognition - Age Calculation

**Problem:** Need to calculate age from DOB and apply special handling for seniors

**Data:**
```json
{
  "applicants": [{
    "demographic": {
      "firstName": "Robert",
      "dateOfBirth": "1955-02-11"
    }
  }]
}
```

**Form Requirements:**
- Display "Age: 71"
- Mark as senior (age ≥ 65)
- Apply different styling for seniors
- Require additional documentation for seniors

**YAML Template:**
```yaml
sections:
  - sectionId: applicant-details
    type: ACROFORM
    templatePath: forms/applicant.pdf
    viewModelType: AcroForm
    
    fieldMappingGroups:
      - mappingType: DIRECT
        fields:
          applyingAs: "primaryFirstName"
          age: "primaryAge"                    # Calculated
          seniorStatus: "isSenior"             # Flag based on age
        
        fieldStyles:
          age:
            fontSize: 14
            bold: true
          seniorStatus:
            textColor: 0xFF0000              # Red for seniors
            bold: true
            borderColor: 0xFF0000            # Red border
```

**What AcroFormViewModelBuilder Does:**
```java
// Parse DOB
LocalDate dob = LocalDate.parse("1955-02-11");

// Calculate age
long primaryAge = ChronoUnit.YEARS.between(dob, LocalDate.now());  // 71

// Set senior flag
boolean isSenior = primaryAge >= SENIOR_AGE_THRESHOLD;             // true

// Format for display
String displayAge = "Age: " + primaryAge;
```

**Result:**
- Age automatically calculated (no manual entry)
- Senior flag computed reliably
- Styling applied based on computed value
- Future-proof: age updates automatically each year

---

### Scenario 4: Multi-Applicant Form - Flattening With Defaults

**Problem:** Need to handle optional spouse/secondary applicants with safe defaults

**Data:**
```json
{
  "applicants": [
    {
      "type": "PRIMARY",
      "demographic": {
        "firstName": "John",
        "lastName": "Doe",
        "email": "john@example.com"
      }
    }
    // No spouse - optional!
  ]
}
```

**Form Requirements:**
- Always fill primary applicant fields
- Spouse fields should be empty/readonly if no spouse
- Default values for missing fields

**YAML Template:**
```yaml
sections:
  - sectionId: applicants
    type: ACROFORM
    templatePath: forms/multi-applicant.pdf
    viewModelType: AcroForm
    
    fieldMappingGroups:
      - mappingType: DIRECT
        fields:
          primaryName: "primaryFullName"
          primaryEmail: "primaryEmail"
          spouseName: "spouseFullName"              # Empty if no spouse
          spouseEmail: "spouseEmail"                # Empty if no spouse
        
        fieldStyles:
          spouseName:
            readOnly: true                          # Always readonly
            backgroundColor: 0xEEEEEE               # Light gray
          spouseEmail:
            readOnly: true
            backgroundColor: 0xEEEEEE
```

**What AcroFormViewModelBuilder Does:**
```java
// Extract primary
String primaryFullName = "John Doe";
String primaryEmail = "john@example.com";

// Extract spouse (with null safety)
String spouseFullName = null;              // No spouse
String spouseEmail = null;                 // No spouse

// Or with defaults:
@Builder.Default
private String spouseFullName = "";        // Empty string instead of null
```

**Result:**
- No null pointer exceptions
- Safe handling of optional data
- Clear in YAML which fields are optional

---

## Implementation Steps

### Step 1: Understand Your Data Structure

Analyze the incoming JSON to identify:
- Nested objects that need flattening
- Arrays that need aggregation
- Transformations needed (date format, concatenation, calculations)
- Conditional flags needed

**Example Analysis:**
```
Raw Data:
├── applicants[]
│   ├── demographic
│   │   ├── firstName ──┐
│   │   ├── lastName ───┼──> Concatenate → primaryFullName
│   │   ├── middleName ─┘
│   │   ├── dateOfBirth ───> Format & Calculate → primaryAge, isSenior
│   │   └── ssn
│   └── addresses[]
│       ├── street ──┐
│       ├── city ────┼──> Flatten → primaryAddress
│       ├── state ───┤
│       └── zipCode ─┘

Transformations Needed:
✓ Flatten nested objects
✓ Concatenate fields
✓ Format dates
✓ Calculate age
✓ Count dependents
✓ Create boolean flags
```

### Step 2: Examine Existing ViewModel

Check if [AcroFormViewModel.java](src/main/java/com/example/demo/docgen/viewmodel/AcroFormViewModel.java) covers your use case.

**Does it have fields for your requirements?**
```java
✓ primaryFullName
✓ primaryDateOfBirth (formatted)
✓ primaryAddress (flattened)
✓ spouseFullName
✓ dependentCount
✓ isSenior
✓ hasSpouse
✓ isFamilyApplication
✓ (+ many more demographic fields)
```

**If yes:** Use as-is, skip to Step 4

**If no:** Create a custom ViewModel (Step 3)

### Step 3: Create Custom ViewModel (If Needed)

**Case 1: Extend AcroFormViewModel**
```java
@Data
@Builder
public class CustomAcroFormViewModel extends AcroFormViewModel {
    private String customField1;
    private String customField2;
    private Boolean customFlag;
}
```

**Case 2: Create New ViewModel**
```java
@Data
@Builder
public class SpecializedFormViewModel {
    private String flattenedField;
    private Integer computedMetric;
    private Boolean derivedFlag;
}
```

### Step 4: Review AcroFormViewModelBuilder

Check [AcroFormViewModelBuilder.java](src/main/java/com/example/demo/docgen/viewmodel/AcroFormViewModelBuilder.java) to understand existing transformations:

```java
// Date formatting
String primaryDateOfBirth = formatDate(dob);         // "MM/dd/yyyy"

// Concatenation
String primaryFullName = firstName + " " + lastName;

// Age calculation
long primaryAge = ChronoUnit.YEARS.between(dob, now);

// Filtering
Map primary = applicants.stream()
    .filter(a -> isPrimaryApplicant(a))
    .findFirst()
    .orElse(null);

// Counting
int dependentCount = dependents.size();

// Conditional flags
boolean isSenior = age >= SENIOR_AGE_THRESHOLD;
```

**Does the builder handle your transformations?** → Likely yes!

### Step 5: Create Your YAML Template

Use the transformed ViewModel fields in your template:

```yaml
sections:
  - sectionId: my-section
    type: ACROFORM
    templatePath: forms/my-form.pdf
    viewModelType: AcroForm          # ← Key: Enables ViewModel transformation
    
    fieldMappingGroups:
      - mappingType: DIRECT          # ← Now use DIRECT (simple) instead of JSONATA (complex)
        fields:
          # Map form field → ViewModel property
          formFieldName1: "primaryFullName"
          formFieldName2: "primaryDateOfBirth"
          formFieldName3: "dependentCount"
          
        fieldStyles:
          formFieldName1:
            bold: true
            textColor: 0x000000
```

### Step 6: Test Your Template

**Create Test Request:**
```json
{
  "templateId": "my-template.yaml",
  "namespace": "common-templates",
  "data": {
    "applicationId": "TEST-001",
    "applicationDate": "2026-02-11",
    "applicants": [
      {
        "type": "PRIMARY",
        "demographic": {
          "firstName": "John",
          "lastName": "Doe",
          "dateOfBirth": "1980-05-15"
        }
      }
    ]
  }
}
```

**Generate PDF:**
```bash
curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d @test-request.json \
  -o output.pdf
```

**Verify:**
- Form fields are populated correctly
- Computed values match expected results
- Styling is applied properly

---

## Best Practices

### 1. **Keep ViewModels Simple**
Only include fields needed for templates. Don't add every possible transformation.

```java
// ✅ Good: Only calculated fields needed for form
@Data @Builder
public class MyViewModel {
    private String flattenedName;      // Form field
    private Integer dependentCount;    // Form field
    private Boolean isActive;          // Form field
}

// ❌ Bad: Too many unused fields
@Data @Builder
public class MyViewModel {
    private String field1, field2, field3, field4, field5;  // Unrelated
    private List<Object> rawData;                          // Not needed
    private Map<String, Object> metadata;                  // Extra
}
```

### 2. **Use Descriptive Property Names**
Name fields to match form context.

```java
// ✅ Good
private String declarationOfResidencyCity;
private String declarationOfResidencyState;

// ❌ Bad - ambiguous
private String city1;
private String state1;
```

### 3. **Handle Null Safely**
Always provide defaults or check for null.

```java
// ✅ Good
private String spouseEmailAddress = "";     // Default empty
if (spouse != null) {
    this.spouseLastName = spouse.getLastName();
}

// ❌ Bad - NPE risk
private String spouseEmailAddress;          // Could be null
this.spouseLastName = spouse.getLastName(); // NPE if spouse is null
```

### 4. **Provide Computed Flags**
Make rendering decisions explicit.

```java
// ✅ Good
private Boolean isSenior = age >= 65;
private Boolean hasSpouse = spouse != null;
private Boolean isLargeFamily = dependents.size() > 3;

// Template can use directly:
readOnly: "$.isSenior"  // Much clearer
```

### 5. **Format Data for Display**
Format dates, numbers, etc. in the ViewModel, not in expressions.

```java
// ✅ Good - formatted in ViewModel
LocalDate born = LocalDate.parse("1980-05-15");
DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd/yyyy");
this.dateOfBirth = born.format(fmt);        // "05/15/1980"

// Template uses directly:
fields:
  dob: "dateOfBirth"                        // Simple!

// ❌ Bad - format in expression
fields:
  dob: "$.demographic.dateOfBirth | $substring(5,7) & '/' & $substring(8,10) & '/' & $substring(0,4)"
```

### 6. **Document Your ViewModel**
Add JavaDoc explaining what fields are and where they come from.

```java
/**
 * Formatted applicant address: "123 Main St, Boston, MA 02101"
 * Flattened from applicant.addresses[type='HOME']
 * or empty string if no home address found
 */
private String primaryAddress = "";
```

### 7. **Use Builder Pattern**
Use Lombok `@Builder` for clean construction.

```java
// ✅ Good - fluent, readable
AcroFormViewModel vm = AcroFormViewModel.builder()
    .primaryFullName("John Doe")
    .primaryAge(45)
    .isSenior(false)
    .build();

// ❌ Bad - verbose
AcroFormViewModel vm = new AcroFormViewModel();
vm.setPrimaryFullName("John Doe");
vm.setPrimaryAge(45);
```

---

## Troubleshooting

### Issue: Fields not populating

**Check:**
1. Does PageSection have `viewModelType: AcroForm`?
2. Are field names correct? (Case-sensitive)
3. Run in debug mode to verify ViewModel created:
   ```
   [INFO] Applied ViewModel type 'AcroForm' to AcroForm section
   ```

### Issue: Null values in form fields

**Check:**
1. Does raw data have the nested objects?
2. Is null-handling in builder correct?
   ```java
   if (applicant != null) {
       // Safe access
   }
   ```
3. Provide defaults:
   ```java
   private String field = "";  // Instead of null
   ```

### Issue: Styling not applied

**Check:**
1. Field names in `fieldStyles` match PDF form field names
2. YAML indentation is correct
3. Styling is on `fieldStyles` level, not `fields` level:
   ```yaml
   fieldMappingGroups:
     - fields:
         name: "fullName"
       fieldStyles:                    # ← Correct level
         name:
           bold: true
   ```

### Issue: Need to create custom ViewModel

**Steps:**
1. Create new class implementing `ViewModelBuilder<YourViewModel>`
2. Annotate with `@Component` for Spring discovery
3. Add to viewModelType in YAML
4. ViewModelFactory automatically finds it!

---

## Comparison: Before vs After ViewModel

### Before (Complex JSONATA)
```yaml
sections:
  - sectionId: demographics
    type: ACROFORM
    templatePath: forms/demo.pdf
    
    fieldMappingGroups:
      - mappingType: JSONATA
        fields:
          fullName: "applicants[type='PRIMARY'].demographic.(firstName & ' ' & (middleName != null ? middleName & ' ' : '') & lastName)"
          age: "applicants[type='PRIMARY'].demographic.dateOfBirth | $substring(0,4) | $number($) | $abs($millis() / (365.25 * 24 * 60 * 60 * 1000) - $)"
          address: "applicants[type='PRIMARY'].addresses[type='HOME'].(street & ', ' & city & ', ' & state & ' ' & zipCode)"
          dependents: "applicants[type!='PRIMARY'] | $count($)"
          senior: "applicants[type='PRIMARY'].demographic.dateOfBirth | application/x-www-form-urlencoded < '1960-02-11'"
```

**Problems:**
- ❌ Very hard to read and understand
- ❌ Difficult to test
- ❌ No IDE support or autocomplete
- ❌ Silent failures if syntax wrong
- ❌ Hard to maintain or modify
- ❌ Easy to introduce bugs

### After (Clean ViewModel)
```yaml
sections:
  - sectionId: demographics
    type: ACROFORM
    templatePath: forms/demo.pdf
    viewModelType: AcroForm
    
    fieldMappingGroups:
      - mappingType: DIRECT
        fields:
          fullName: "primaryFullName"
          age: "primaryAge"
          address: "primaryAddress"
          dependents: "dependentCount"
          senior: "isSenior"
        
        fieldStyles:
          fullName:
            bold: true
          senior:
            textColor: 0xFF0000
```

**Benefits:**
- ✅ Clear and concise
- ✅ Easy to test in isolation
- ✅ Full IDE support
- ✅ Explicit error handling
- ✅ Easy to maintain
- ✅ Prevents bugs through type safety

---

## Summary

**When to use AcroForm ViewModel:**
1. Data is nested and needs flattening
2. Multiple fields need concatenation or formatting
3. You need to compute flags or derived values
4. You want to avoid complex JSONATA expressions
5. Team is more comfortable with Java than JSONATA

**How it works:**
1. Specify `viewModelType: AcroForm` in YAML
2. ViewModelFactory finds and runs AcroFormViewModelBuilder
3. Raw data is transformed to flat, simple properties
4. Use DIRECT mapping to populate form fields
5. Much cleaner, more maintainable than JSONATA

**Key advantage:**
Write standard Java code instead of learning domain-specific expression language!

---

## Additional Resources

- [AcroFormRenderer.java](src/main/java/com/example/demo/docgen/renderer/AcroFormRenderer.java) - Main renderer
- [AcroFormViewModel.java](src/main/java/com/example/demo/docgen/viewmodel/AcroFormViewModel.java) - Data model
- [AcroFormViewModelBuilder.java](src/main/java/com/example/demo/docgen/viewmodel/AcroFormViewModelBuilder.java) - Transformation logic
- [AcroFormViewModelBuilderTest.java](src/test/java/com/example/demo/docgen/viewmodel/AcroFormViewModelBuilderTest.java) - Test examples
- [ACROFORM_FIELD_STYLING_GUIDE.md](ACROFORM_FIELD_STYLING_GUIDE.md) - Field styling reference
