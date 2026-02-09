# AcroForm Field Styling - Architecture Overview

## Feature Summary

The AcroForm Field Styling feature enables configurable visual styling of PDF form fields without requiring code changes. Styles can be applied per-field or at the group level, including font sizes, colors, alignment, borders, and field properties like read-only and hidden.

## Architecture

### Components

#### 1. FieldStyling Model
**Location**: `com.example.demo.docgen.model.FieldStyling`

Represents styling configuration for a single field:
- Font properties (size, name, bold, italic)
- Colors (text, background, border)
- Alignment
- Field flags (readOnly, hidden)

```
FieldStyling
├── fontSize: Float
├── textColor: Integer (0xRRGGBB)
├── backgroundColor: Integer (0xRRGGBB)
├── borderColor: Integer (0xRRGGBB)
├── borderWidth: Float
├── alignment: TextAlignment (LEFT/CENTER/RIGHT)
├── fontName: String
├── bold: Boolean
├── italic: Boolean
├── readOnly: Boolean
└── hidden: Boolean
```

#### 2. FieldMappingGroup Extensions
**Location**: `com.example.demo.docgen.model.FieldMappingGroup`

Extended to include:
- `fieldStyles: Map<String, FieldStyling>` - Per-field styling overrides
- `defaultStyle: FieldStyling` - Default styling for entire group

```
FieldMappingGroup
├── mappingType: MappingType
├── basePath: String
├── fields: Map<String, String>
├── repeatingGroup: RepeatingGroupConfig
├── fieldStyles: Map<String, FieldStyling>  ← NEW
└── defaultStyle: FieldStyling              ← NEW
```

#### 3. AcroFormRenderer Enhancement
**Location**: `com.example.demo.docgen.renderer.AcroFormRenderer`

Extended to apply styles:
- `fillFormFields()` - Updated to accept PageSection and collect all styles
- `applyFieldStyling()` - New method to apply styles to PDField instances

### Data Flow

```
1. Template YAML Parsing
   └─> FieldMappingGroup (with optional fieldStyles & defaultStyle)

2. Field Value Mapping
   └─> mapFieldValues(section, context)
       └─> Returns Map<String, String>

3. Field Population & Styling
   └─> fillFormFields(acroForm, fieldValues, section)
       ├─> Collect all fieldStyles from all mapping groups
       ├─> Set field value: field.setValue(value)
       └─> Apply styling: applyFieldStyling(field, styling)

4. PDF Generation
   └─> PDDocument with styled fields
```

### Styling Application Process

```
For each field:
  1. Get field value from data mapping
  2. Set field value in PDF form
  3. Look up styling:
     a. Check fieldStyles map for this specific field
     b. If not found, use defaultStyle from mapping group
     c. If neither exists, use PDFBox defaults
  4. Apply styling properties:
     - Colors (using COSArray and COSFloat)
     - Alignment (using Q field)
     - Flags (using Ff field)
```

## Implementation Details

### Style Resolution

```java
FieldStyling styleToApply = null;

// 1. Check per-field styles
if (allFieldStyles.containsKey(fieldName)) {
    styleToApply = allFieldStyles.get(fieldName);
}

// 2. Fall back to default style if available
if (styleToApply == null && defaultStyle != null) {
    styleToApply = defaultStyle;
}

// 3. Apply if we have a style
if (styleToApply != null) {
    applyFieldStyling(field, styleToApply);
}
```

### PDFBox API Integration

The implementation uses PDFBox's COS (Compact Object Structure) layer for direct PDF manipulation:

```java
// Text Color (via appearance stream)
// Stored in PDF as /DA (default appearance) field

// Background Color
COSArray bgColor = new COSArray();
bgColor.add(new COSFloat(r));
bgColor.add(new COSFloat(g));
bgColor.add(new COSFloat(b));
field.getCOSObject().setItem(COSName.getPDFName("BG"), bgColor);

// Border Color
COSArray borderColor = new COSArray();
borderColor.add(new COSFloat(r));
borderColor.add(new COSFloat(g));
borderColor.add(new COSFloat(b));
field.getCOSObject().setItem(COSName.getPDFName("BC"), borderColor);

// Text Alignment (Q field: 0=left, 1=center, 2=right)
field.getCOSObject().setItem(COSName.getPDFName("Q"), new COSFloat(alignment.code));

// Field Flags (Ff field, bit flags)
int flags = field.getCOSObject().getInt(COSName.FF, 0);
flags |= 1;  // Set bit 0 (ReadOnly)
field.getCOSObject().setItem(COSName.FF, new COSFloat(flags));
```

## Configuration Structure

### YAML Template Structure

```yaml
sections:
  - sectionId: form-id
    type: ACROFORM
    templatePath: template.pdf
    
    fieldMappingGroups:
      - mappingType: DIRECT
        
        # Optional: Default style for all fields in this group
        defaultStyle:
          fontSize: 11
          textColor: 0x000000
          borderColor: 0xCCCCCC
        
        # Field value mappings
        fields:
          fieldA: dataPath.a
          fieldB: dataPath.b
        
        # Optional: Per-field style overrides
        fieldStyles:
          fieldA:
            bold: true
            fontSize: 12
          fieldB:
            backgroundColor: 0xFFFF00
```

## Usage Patterns

### Pattern 1: Uniform Styling
Use `defaultStyle` for all fields:
```yaml
defaultStyle:
  fontSize: 11
  borderColor: 0xCCCCCC
fields:
  field1: data1
  field2: data2
  field3: data3
```

### Pattern 2: Per-Field Customization
Use `fieldStyles` for exceptions:
```yaml
defaultStyle:
  fontSize: 11
fieldStyles:
  field1:
    fontSize: 14  # Override default
  field2:
    bold: true    # Add custom style
```

### Pattern 3: Mixed Configuration
Use both for maximum flexibility:
```yaml
defaultStyle:
  backgroundColor: 0xF0F0F0
  readOnly: true
fieldStyles:
  importantField:
    backgroundColor: 0xFFFF99  # Override default
    textColor: 0xFF0000        # Add new style
```

## PDF Field Properties

### Standard PDF Field Properties

| PDF Property | Model Field | Type | PDFBox Code |
|-------------|------------|------|------------|
| Background | backgroundColor | Color (RGB) | BG entry |
| Border Color | borderColor | Color (RGB) | BC entry |
| Border Width | borderWidth | Float | BS dictionary |
| Text Alignment | alignment | Enum | Q field (0/1/2) |
| Field Flags | readOnly, hidden | Boolean flags | Ff field bits |
| Text Color | textColor | Color (RGB) | DA (appearance) |
| Font Size | fontSize | Float | DA (appearance) |

### Field Flags (Ff)
- Bit 0: ReadOnly (1)
- Bit 1: Required (2)
- Bit 12: Password (4096)

## Limitations

### Current Limitations
1. **Font Embedding**: Custom fonts not supported; limited to standard PDF fonts
2. **Appearance Streams**: Direct appearance stream modification not implemented
3. **Conditional Styling**: Static configuration; no expression-based styling
4. **Text Color**: Limited control due to PDFBox's appearance stream handling

### Workarounds
1. Use conditional sections with different templates for dynamic styling
2. Pre-process data with ViewModels to enable logic-based styling decisions
3. Use standard colors for maximum compatibility

## Performance Considerations

### Processing Overhead
- **Negligible**: Styling is applied after field population
- **Per-Field**: O(1) lookup and application per field
- **Memory**: Minimal - only stores styling configurations

### Optimization Tips
1. Use `defaultStyle` to avoid repetitive per-field configurations
2. Group similarly-styled fields in same mapping group
3. Use static YAML templates (no runtime generation)

## Testing

### Unit Test Coverage
- `AcroFormRendererTest`: Validates field population
- Manual testing: PDF viewer verification

### Integration Testing
1. Generate PDF with styled fields
2. Open in Adobe Reader or similar
3. Verify:
   - Colors are displayed correctly
   - Alignment is applied
   - Read-only fields are protected
   - Font sizes are correct

## Future Enhancements

### Planned Features
1. **Font Embedding**: Support custom TrueType/OpenType fonts
2. **Advanced Appearance**: Gradient backgrounds, patterns
3. **Conditional Styling**: Expression-based style selection
4. **Style Themes**: Reusable style configurations
5. **CSS-like Styling**: Inherit styles from parent groups
6. **Responsive Styling**: Different styles based on document size/layout

### Architecture for Enhancement
```
Future Design:
├── Theme System
│   ├── predefined-themes/light.yaml
│   ├── predefined-themes/dark.yaml
│   └── custom-themes/
│
├── Style Expressions
│   └── conditional-styling.yaml (style if field value matches pattern)
│
└── Advanced Appearance
    └── appearance-streams/custom-fonts.yaml
```

## Integration with Other Features

### Compatible With
- ✅ Field Mapping Strategies (DIRECT, JSONPATH, JSONATA)
- ✅ Repeating Groups
- ✅ Multiple Mapping Groups
- ✅ Base Path Optimization
- ✅ Conditional Sections
- ✅ Namespace Support
- ✅ ViewModels

### Not Compatible
- ❌ FreeMarker Sections (needs different renderer)
- ❌ Template Inheritance (if inheritor changes mapping)

## Code Examples

### Using Field Styling in Java (Programmatic)

```java
// Create styling configuration
FieldStyling redBold = FieldStyling.builder()
    .textColor(0xFF0000)
    .bold(true)
    .fontSize(12f)
    .build();

FieldStyling readOnlyGray = FieldStyling.builder()
    .readOnly(true)
    .backgroundColor(0xF0F0F0)
    .textColor(0x666666)
    .build();

// Create mapping group with styling
FieldMappingGroup group = FieldMappingGroup.builder()
    .mappingType(MappingType.DIRECT)
    .defaultStyle(FieldStyling.builder()
        .fontSize(11f)
        .build())
    .fields(Map.of(
        "firstName", "data.firstName",
        "lastName", "data.lastName",
        "total", "data.total"
    ))
    .fieldStyles(Map.of(
        "firstName", redBold,
        "lastName", redBold,
        "total", readOnlyGray
    ))
    .build();
```

### Color Helper Functions

```java
// Convert RGB to hex
int rgbToHex(int r, int g, int b) {
    return (r << 16) | (g << 8) | b;
}

// Convert hex to RGB components
int getRed(int hexColor) {
    return (hexColor >> 16) & 0xFF;
}

int getGreen(int hexColor) {
    return (hexColor >> 8) & 0xFF;
}

int getBlue(int hexColor) {
    return hexColor & 0xFF;
}
```

## References

### PDFBox Documentation
- [AcroForm Handling](https://pdfbox.apache.org/docs/2.0.x/index.html)
- [PDF Specification - Interactive Forms](https://www.adobe.io/content/dam/udp/assets/open/pdf/spec/PDF32000_2008.pdf)

### Related Documents
- [ACROFORM_FIELD_STYLING_GUIDE.md](ACROFORM_FIELD_STYLING_GUIDE.md) - User guide
- [ACROFORM_FIELD_STYLING_EXAMPLES.md](ACROFORM_FIELD_STYLING_EXAMPLES.md) - Practical examples
- [CONFIGURATION_GUIDE.md](CONFIGURATION_GUIDE.md) - Template configuration

