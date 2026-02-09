# AcroForm Field Styling Guide

## Overview

This guide explains how to apply configurable styling to AcroForm PDF fields in the document generation system. You can now control font sizes, colors, alignment, borders, and field properties for individual fields or apply default styles to entire field mapping groups.

## Styling Features

### Supported Style Properties

The `FieldStyling` model supports the following properties:

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `fontSize` | Float | Font size in points | `12` for 12pt |
| `textColor` | Integer | RGB color as hex (0xRRGGBB) | `0xFF0000` (red) |
| `backgroundColor` | Integer | Background color as hex | `0xFFFFFF` (white) |
| `borderColor` | Integer | Border color as hex | `0x666666` (gray) |
| `borderWidth` | Float | Border width in points | `1.5` |
| `alignment` | Enum | Text alignment: LEFT, CENTER, RIGHT | `CENTER` |
| `fontName` | String | Standard PDF fonts | `Helvetica`, `Times-Roman` |
| `bold` | Boolean | Bold text flag | `true` |
| `italic` | Boolean | Italic text flag | `true` |
| `readOnly` | Boolean | Make field non-editable | `true` |
| `hidden` | Boolean | Hide/mask field (password) | `true` |

### Color Reference

Colors are specified as RGB hex values in the format `0xRRGGBB`:

```
0x000000 = Black
0xFFFFFF = White
0xFF0000 = Red
0x00FF00 = Green
0x0000FF = Blue
0xFFFF00 = Yellow
0xFF00FF = Magenta
0x00FFFF = Cyan
0xCCCCCC = Light Gray
0x666666 = Gray
```

## Configuration Methods

### Method 1: Per-Field Styling (Recommended)

Apply styling to individual fields using the `fieldStyles` map within a mapping group:

```yaml
sections:
  - sectionId: enrollment-form
    type: ACROFORM
    templatePath: templates/forms/applicant-form.pdf
    fieldMappingGroups:
      - mappingType: DIRECT
        fields:
          firstName: $.applicant.firstName
          lastName: $.applicant.lastName
          email: $.applicant.email
          status: $.applicant.status
        
        fieldStyles:
          firstName:
            fontSize: 12
            bold: true
          
          lastName:
            fontSize: 12
            bold: true
          
          email:
            fontSize: 10
            textColor: 0x0066CC  # Blue
          
          status:
            backgroundColor: 0xFFFF00  # Yellow
            readOnly: true
```

### Method 2: Default Styling for Group

Apply a default style to all fields in a mapping group using `defaultStyle`:

```yaml
fieldMappingGroups:
  - mappingType: JSONATA
    defaultStyle:
      fontSize: 11
      alignment: CENTER
      borderColor: 0x666666
      borderWidth: 1
    
    fields:
      primaryName: "applicants[type='PRIMARY'].name"
      spouseName: "applicants[type='SPOUSE'].name"
      dependentCount: "$count(applicants[type='DEPENDENT'])"
```

### Method 3: Per-Field Override with Default

Combine default styling with per-field overrides:

```yaml
fieldMappingGroups:
  - mappingType: DIRECT
    defaultStyle:
      fontSize: 11
      textColor: 0x000000      # Black text for all
      borderColor: 0xCCCCCC    # Light gray border
    
    fields:
      field1: value1
      field2: value2
      field3: value3
      field4: value4
    
    fieldStyles:
      field1:
        # Uses default fontSize, textColor, borderColor
        bold: true
      
      field2:
        # Overrides textColor for this field
        textColor: 0xFF0000  # Red text
        bold: true
      
      field3:
        # Complete override
        fontSize: 14
        textColor: 0x0000FF  # Blue
        backgroundColor: 0xFFFF00  # Yellow background
        readOnly: true
```

## Real-World Examples

### Example 1: Required vs Optional Fields

```yaml
sections:
  - sectionId: applicant-form
    type: ACROFORM
    templatePath: templates/forms/applicant.pdf
    
    fieldMappingGroups:
      - mappingType: DIRECT
        defaultStyle:
          fontSize: 11
          textColor: 0x000000
        
        fields:
          firstName: applicant.firstName
          lastName: applicant.lastName
          middleName: applicant.middleName
          ssn: applicant.ssn
          dateOfBirth: applicant.dateOfBirth
          email: applicant.email
        
        fieldStyles:
          # Required fields - red text, bold
          firstName:
            textColor: 0xFF0000
            bold: true
          
          lastName:
            textColor: 0xFF0000
            bold: true
          
          ssn:
            textColor: 0xFF0000
            bold: true
            hidden: true  # Mask SSN input
          
          # Optional fields - normal styling
          middleName:
            textColor: 0x666666  # Gray text
```

### Example 2: Read-Only and Calculated Fields

```yaml
fieldMappingGroups:
  - mappingType: JSONATA
    
    fields:
      # User input fields
      quantity: items[0].quantity
      price: items[0].unitPrice
      
      # Calculated field - read-only
      total: items[0].quantity * items[0].unitPrice
      
      # System fields
      generatedId: $uuid()
      timestamp: $now()
    
    fieldStyles:
      # Calculated total: read-only, light gray background
      total:
        readOnly: true
        backgroundColor: 0xEEEEEE
        italic: true
        textColor: 0x333333
      
      # System fields: very light background, smaller font
      generatedId:
        readOnly: true
        backgroundColor: 0xF5F5F5
        fontSize: 9
      
      timestamp:
        readOnly: true
        backgroundColor: 0xF5F5F5
        fontSize: 9
```

### Example 3: Color-Coded Status Fields

```yaml
fieldMappingGroups:
  - mappingType: JSONATA
    
    fields:
      status: applicant.status
      enrollmentType: applicant.enrollmentType
      riskLevel: applicant.riskLevel
    
    fieldStyles:
      # Status field styling
      status:
        bold: true
        backgroundColor: 0xE3F2FD  # Light blue background
        readOnly: true
      
      # Enrollment type - centered
      enrollmentType:
        alignment: CENTER
        bold: true
        fontSize: 12
      
      # Risk level - color based on value (applied in code if data-driven)
      riskLevel:
        backgroundColor: 0xFFF3E0  # Light orange
        bold: true
        readOnly: true
```

## Implementation Details

### How Styling is Applied

1. **Field Mapping**: Fields are mapped from request data to PDF form field names
2. **Value Population**: Field values are set in the PDF form
3. **Style Application**: Styles are retrieved from `fieldStyles` map and applied to each field

### Style Resolution Order

1. First check if field has specific style in `fieldStyles` map
2. If not found, apply `defaultStyle` from the mapping group
3. If neither exists, use PDFBox defaults

### Styling Scope

- **Per-Field**: Only affects the specified field
- **Per-Group**: Affects all fields in a `FieldMappingGroup`
- **Multiple Groups**: Each group's styling is independent

```
Section
├── Group 1
│   ├── defaultStyle (applies to all fields in Group 1)
│   └── fieldStyles (overrides defaultStyle for specific fields)
├── Group 2
│   ├── defaultStyle (independent from Group 1)
│   └── fieldStyles (independent from Group 1)
└── Group 3
    └── ... (no defaultStyle or fieldStyles)
```

## Advanced Use Cases

### Dynamic Styling Based on Data

While the YAML configuration is static, you can achieve dynamic styling by:

1. **Pre-processing Data**: Use a ViewModel to calculate styling values
2. **Multiple Sections**: Create multiple sections with different styling for different data scenarios
3. **Conditional Sections**: Use `condition` field to render sections based on data values

Example structure:
```yaml
sections:
  # Section 1: For high-priority applicants
  - sectionId: priority-form
    type: ACROFORM
    condition: "$.applicant.priority == 'HIGH'"
    templatePath: templates/priority-applicant.pdf
    fieldMappingGroups:
      - mappingType: DIRECT
        defaultStyle:
          backgroundColor: 0xFFFF00  # Yellow highlight
  
  # Section 2: For regular applicants
  - sectionId: standard-form
    type: ACROFORM
    condition: "$.applicant.priority != 'HIGH'"
    templatePath: templates/standard-applicant.pdf
    fieldMappingGroups:
      - mappingType: DIRECT
        defaultStyle:
          backgroundColor: 0xFFFFFF  # White/default
```

## API Reference

### FieldStyling Class

Location: `com.example.demo.docgen.model.FieldStyling`

```java
@Data
@Builder
public class FieldStyling {
    private Float fontSize;                    // Font size in points
    private Integer textColor;                 // RGB hex (0xRRGGBB)
    private Integer backgroundColor;           // RGB hex (0xRRGGBB)
    private Integer borderColor;               // RGB hex (0xRRGGBB)
    private Float borderWidth;                 // Border width in points
    private TextAlignment alignment;           // LEFT, CENTER, RIGHT
    private String fontName;                   // Font name
    private Boolean bold;                      // Bold flag
    private Boolean italic;                    // Italic flag
    private Boolean readOnly;                  // Read-only flag
    private Boolean hidden;                    // Hidden/masked flag
    
    public enum TextAlignment {
        LEFT(0),
        CENTER(1),
        RIGHT(2);
    }
}
```

### FieldMappingGroup Extensions

```java
@Data
public class FieldMappingGroup {
    // ... existing fields ...
    
    // New styling fields:
    private Map<String, FieldStyling> fieldStyles;      // Per-field styles
    private FieldStyling defaultStyle;                  // Default group style
}
```

## Testing and Validation

### Test Styling Configuration

Create a test template to verify styling is applied correctly:

```yaml
sections:
  - sectionId: styling-test
    type: ACROFORM
    templatePath: templates/test-form.pdf
    
    fieldMappingGroups:
      - mappingType: DIRECT
        defaultStyle:
          fontSize: 12
          textColor: 0x000000
        
        fields:
          testField1: value1
          testField2: value2
          testField3: value3
        
        fieldStyles:
          testField1:
            bold: true
            textColor: 0xFF0000
          
          testField2:
            backgroundColor: 0xFFFF00
            readOnly: true
          
          testField3:
            alignment: CENTER
            fontSize: 14
```

### Verify in Generated PDF

1. Generate a document with the test template
2. Open the PDF in Adobe Reader or similar
3. Verify:
   - Font sizes are applied correctly
   - Colors are displayed as configured
   - Read-only fields cannot be edited
   - Alignment is correct
   - Bold/italic flags are visible

## Troubleshooting

### Styling Not Applied

1. **Check Field Names**: Ensure field names in `fieldStyles` exactly match field names in `fields` map
2. **Check YAML Syntax**: Verify proper indentation and types (integers for colors, booleans for flags)
3. **Check PDF Template**: Verify the PDF form has the named fields
4. **Check Logs**: Look for warning messages about missing fields

### Colors Not Displaying

1. **Verify Hex Format**: Ensure colors are in format `0xRRGGBB`
2. **Check PDF Viewer**: Some viewers may render colors differently
3. **Use Standard Colors**: Start with well-known colors like `0x000000`, `0xFFFFFF`, `0xFF0000`

### Read-Only Not Working

1. **Verify Flag**: Check that `readOnly: true` is set
2. **Check PDF Viewer**: Some viewers may not respect read-only flags
3. **Check Flattening**: If form is flattened, fields become read-only by default

## Best Practices

1. **Use Default Styling**: Define `defaultStyle` for consistent appearance across a group
2. **Minimal Overrides**: Only override specific fields that need different styling
3. **Color Accessibility**: Use sufficient contrast between text and background colors
4. **Consistent Font Sizes**: Keep font sizes between 8-14pt for readability
5. **Group Related Styling**: Group similar styled fields together in the same mapping group
6. **Document Colors**: Add comments explaining color choices for maintainability

```yaml
fieldStyles:
  # Required field - stands out in red
  firstName:
    textColor: 0xFF0000
    bold: true
  
  # Read-only calculation - light gray background
  totalAmount:
    readOnly: true
    backgroundColor: 0xF0F0F0
    textColor: 0x555555
```

## Limitations and Notes

### Current Limitations

1. **Font Control**: Limited to standard PDF fonts (Helvetica, Times-Roman, Courier). Custom fonts require modification to PDFBox appearance streams.
2. **Advanced Styling**: Complex appearance streams (gradients, patterns) not directly supported
3. **Conditional Styling**: Static configuration; dynamic styling based on field values requires multiple sections

### Future Enhancements

- Font embedding for custom fonts
- Advanced appearance stream customization
- Expression-based styling (apply style if field value matches pattern)
- Pre-defined style themes
- CSS-like style inheritance

