# AcroForm Field Styling - Quick Reference

## Quick Start

### 1. Basic Styling Setup

Add styling to your template YAML:

```yaml
fieldMappingGroups:
  - mappingType: DIRECT
    fields:
      firstName: applicant.firstName
      status: applicant.status
    
    fieldStyles:
      firstName:
        bold: true
        fontSize: 12
      status:
        backgroundColor: 0xFFFF00
        readOnly: true
```

### 2. Default Styling for All Fields

```yaml
fieldMappingGroups:
  - mappingType: DIRECT
    defaultStyle:
      fontSize: 11
      borderColor: 0xCCCCCC
    fields:
      field1: data1
      field2: data2
```

### 3. Override with Per-Field Styles

```yaml
fieldMappingGroups:
  - mappingType: DIRECT
    defaultStyle:
      fontSize: 11
    fieldStyles:
      firstName:
        fontSize: 14  # Overrides default
        bold: true
```

## Color Quick Reference

```
Common Colors:
0x000000 = Black
0xFFFFFF = White
0xFF0000 = Red
0x00FF00 = Green
0x0000FF = Blue
0xFFFF00 = Yellow
0xFF00FF = Magenta
0x00FFFF = Cyan
0xCCCCCC = Light Gray
0x666666 = Dark Gray
0xF0F0F0 = Very Light Gray (read-only backgrounds)
0xFFFFCC = Light Yellow
0xE8F4F8 = Light Blue
0xFFE6E6 = Light Red
```

## Styling Properties Cheat Sheet

### Text Styling
```yaml
bold: true                    # Make text bold
italic: true                  # Make text italic
fontSize: 12                  # Font size in points
fontName: "Helvetica"         # Font name
textColor: 0xFF0000          # Text color (RGB hex)
alignment: CENTER            # LEFT, CENTER, RIGHT
```

### Visual Styling
```yaml
backgroundColor: 0xFFFFFF    # Background color
borderColor: 0x666666        # Border color
borderWidth: 1.5             # Border width in points
```

### Field Behavior
```yaml
readOnly: true               # Make field non-editable
hidden: true                 # Hide/mask field (password)
```

## Common Scenarios

### Required Fields (Red, Bold)
```yaml
requiredField:
  bold: true
  textColor: 0xFF0000
```

### Read-Only Calculated Field
```yaml
totalField:
  readOnly: true
  backgroundColor: 0xF0F0F0
  italic: true
```

### Highlighted Section
```yaml
importantField:
  backgroundColor: 0xFFFF00
  bold: true
```

### Disabled/Informational Field
```yaml
disabledField:
  readOnly: true
  backgroundColor: 0xCCCCCC
  textColor: 0x666666
```

### Password/Hidden Field
```yaml
passwordField:
  hidden: true
  fontSize: 14
```

## YAML Syntax Examples

### Single Field with Styling
```yaml
fieldStyles:
  firstName:
    bold: true
    fontSize: 12
    textColor: 0xFF0000
```

### Multiple Fields with Styling
```yaml
fieldStyles:
  firstName:
    bold: true
    fontSize: 12
  lastName:
    bold: true
    fontSize: 12
  email:
    textColor: 0x0066CC
    italic: true
```

### Default + Overrides
```yaml
defaultStyle:
  fontSize: 11
  borderColor: 0xCCCCCC

fieldStyles:
  firstName:
    bold: true          # Adds to default
  status:
    backgroundColor: 0xFFFF00  # Overrides + adds to default
```

## Alignment Values

```yaml
alignment: LEFT     # Left-aligned (default)
alignment: CENTER   # Center-aligned
alignment: RIGHT    # Right-aligned
```

## Boolean Properties

```yaml
bold: true          # or false (default)
italic: true        # or false (default)
readOnly: true      # or false (default)
hidden: true        # or false (default)
```

## Numeric Properties

```yaml
fontSize: 11        # Points (8-14 recommended)
borderWidth: 1.5    # Points
textColor: 0xFF0000 # RGB hex (0xRRGGBB)
```

## Complete Example

```yaml
sections:
  - sectionId: styled-form
    type: ACROFORM
    templatePath: templates/form.pdf
    
    fieldMappingGroups:
      - mappingType: DIRECT
        
        defaultStyle:
          fontSize: 11
          borderColor: 0xCCCCCC
        
        fields:
          firstName: contact.firstName
          lastName: contact.lastName
          phone: contact.phone
          email: contact.email
          total: calculations.total
        
        fieldStyles:
          firstName:
            bold: true
            textColor: 0xFF0000
          lastName:
            bold: true
            textColor: 0xFF0000
          phone:
            alignment: CENTER
          total:
            readOnly: true
            backgroundColor: 0xFFFFCC
            fontSize: 12
            bold: true
```

## Validation Checklist

- [ ] Field names in `fieldStyles` match field names in `fields` map
- [ ] Color values are in format `0xRRGGBB` (hex)
- [ ] Font sizes are between 8-14pt (recommended)
- [ ] Boolean properties don't have values (just `true` or `false`)
- [ ] Indentation is correct (2 spaces per level)
- [ ] Field names are spelled correctly

## Troubleshooting Quick Guide

| Issue | Cause | Solution |
|-------|-------|----------|
| Styling not applied | Field name mismatch | Check spelling in `fieldStyles` vs `fields` |
| Colors look wrong | Invalid hex format | Use `0xRRGGBB` format (e.g., `0xFF0000`) |
| Read-only not working | Flag not set correctly | Ensure `readOnly: true` (no quotes) |
| Field not visible | Wrong background color | Use `0xFFFFFF` for white or light colors |
| YAML error | Syntax error | Check indentation and colons |

## Integration Points

The styling feature integrates with:
- ✅ Field mapping strategies (DIRECT, JSONPATH, JSONATA)
- ✅ Multiple mapping groups
- ✅ Repeating groups
- ✅ Namespace support
- ✅ Conditional sections
- ✅ ViewModel pre-processing

## PDFBox Minimum Version

- **Required**: PDFBox 2.0.0 or higher
- **Tested**: PDFBox 2.0.30

## Documentation

- **User Guide**: [ACROFORM_FIELD_STYLING_GUIDE.md](ACROFORM_FIELD_STYLING_GUIDE.md)
- **Examples**: [ACROFORM_FIELD_STYLING_EXAMPLES.md](ACROFORM_FIELD_STYLING_EXAMPLES.md)
- **Architecture**: [ACROFORM_FIELD_STYLING_ARCHITECTURE.md](ACROFORM_FIELD_STYLING_ARCHITECTURE.md)

## Performance Notes

- Styling adds minimal overhead (O(1) per field)
- No impact on form loading or rendering speed
- All configurations are static (YAML-based)
- No database queries or network calls

## Next Steps

1. **Get Started**: Copy an example from the Examples guide
2. **Customize**: Modify colors and styles for your use case
3. **Test**: Generate a PDF and verify in Adobe Reader
4. **Deploy**: Use in production templates
5. **Maintain**: Keep styling consistent across related forms

