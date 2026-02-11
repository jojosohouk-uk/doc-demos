# ViewModel Package Structure Guide

## Current Architecture

The codebase follows a **layered, feature-based package structure**:

```
com.example.demo.docgen/
├── aspect/                    # Cross-cutting concerns (logging, timing)
├── config/                    # Spring configuration
├── controller/                # REST controllers
├── core/                      # Core abstractions (RenderContext)
├── exception/                 # Custom exceptions
├── mapper/                    # Field mapping strategies
├── model/                     # Data models (request/response, config)
├── processor/                 # Post-processors (headers, footers)
├── renderer/                  # Section renderers
│   └── pdfbox/               # PDFBox-specific components
├── service/                   # Business logic & orchestration
└── viewmodel/                # ✓ ViewModels & Builders
```

---

## ViewModel Package Organization

### Current Structure (Flat)

```
com.example.demo.docgen.viewmodel/
├── ViewModelBuilder.java              # Interface
├── ViewModelFactory.java               # Factory
├── AcroFormViewModel.java              # Model
├── AcroFormViewModelBuilder.java       # Builder
├── InvoiceViewModel.java               # Model
├── InvoiceViewModelBuilder.java        # Builder
├── MemberPlanViewModel.java            # Model
└── MemberPlanViewModelBuilder.java     # Builder
```

**Pros:**
- Simple, easy to navigate
- All ViewModels discoverable at a glance
- Good for small to medium projects

**Cons:**
- Can become cluttered with many ViewModels
- No logical grouping by feature/domain

---

## Recommended Package Structures

### **Option 1: Flat (Current - for simple projects)**

**Best for:** Projects with 3-5 ViewModels

```
com.example.demo.docgen.viewmodel/
├── ViewModelBuilder.java
├── ViewModelFactory.java
├── AcroFormViewModel.java
├── AcroFormViewModelBuilder.java
├── InvoiceViewModel.java
├── InvoiceViewModelBuilder.java
└── (all other ViewModels at this level)
```

---

### **Option 2: Feature-Based Hierarchy (Recommended)**

**Best for:** Projects with 5+ ViewModels organized by business domain

```
com.example.demo.docgen.viewmodel/
├── ViewModelBuilder.java               # Shared interface
├── ViewModelFactory.java                # Shared factory
│
├── acroform/                           # Namespace/domain 1
│   ├── AcroFormViewModel.java
│   └── AcroFormViewModelBuilder.java
│
├── invoice/                            # Namespace/domain 2
│   ├── InvoiceViewModel.java
│   └── InvoiceViewModelBuilder.java
│
├── memberplan/                         # Namespace/domain 3
│   ├── MemberPlanViewModel.java
│   └── MemberPlanViewModelBuilder.java
│
└── enrollment/                         # Namespace/domain 4
    ├── EnrollmentViewModel.java
    └── EnrollmentViewModelBuilder.java
```

**Package declarations:**
```java
package com.example.demo.docgen.viewmodel.acroform;
package com.example.demo.docgen.viewmodel.invoice;
package com.example.demo.docgen.viewmodel.enrollment;
```

**Pros:**
- Clear organizational structure
- Easy to navigate
- Scales well
- Groups related ViewModels together

---

### **Option 3: Type-Based Organization**

**Best for:** Projects with many shared/utility ViewModels

```
com.example.demo.docgen.viewmodel/
├── ViewModelBuilder.java
├── ViewModelFactory.java
│
├── models/                             # All ViewModel POJOs
│   ├── AcroFormViewModel.java
│   ├── InvoiceViewModel.java
│   ├── MemberPlanViewModel.java
│   └── EnrollmentViewModel.java
│
├── builders/                           # All Builders
│   ├── AcroFormViewModelBuilder.java
│   ├── InvoiceViewModelBuilder.java
│   ├── MemberPlanViewModelBuilder.java
│   └── EnrollmentViewModelBuilder.java
│
└── shared/                             # Shared utilities
    ├── DateFormatUtil.java
    └── TransformationHelper.java
```

**Package declarations:**
```java
package com.example.demo.docgen.viewmodel.models;
package com.example.demo.docgen.viewmodel.builders;
package com.example.demo.docgen.viewmodel.shared;
```

**Pros:**
- Separates models from builders
- Easy to find what you need

**Cons:**
- More nested structure
- May separate related classes

---

### **Option 4: Hybrid Approach (Best for Large Projects)**

Combines feature-based + type-based:

```
com.example.demo.docgen.viewmodel/
├── ViewModelBuilder.java               # Shared interface
├── ViewModelFactory.java                # Shared factory
├── shared/                             # Shared utilities
│   ├── DateFormatUtil.java
│   └── ValidationHelper.java
│
├── acroform/                           # Feature 1
│   ├── AcroFormViewModel.java
│   └── AcroFormViewModelBuilder.java
│
├── enrollment/                         # Feature 2
│   ├── EnrollmentViewModel.java
│   ├── EnrollmentViewModelBuilder.java
│   └── EnrollmentTransformHelper.java
│
├── invoice/                            # Feature 3
│   ├── InvoiceViewModel.java
│   ├── InvoiceViewModelBuilder.java
│   └── InvoiceCalculator.java
│
└── memberplan/                         # Feature 4
    ├── MemberPlanViewModel.java
    ├── MemberPlanViewModelBuilder.java
    └── shared/                         # Feature-specific shared code
        └── PlanAggregator.java
```

---

## Naming Conventions

### **ViewModel Classes**

**Pattern:** `{Feature}ViewModel`

✅ **Correct:**
```java
class AcroFormViewModel { }
class InvoiceViewModel { }
class EnrollmentApplicationViewModel { }
class MemberPlanViewModel { }
```

❌ **Incorrect:**
```java
class AcroForm { }                      // Too short, ambiguous
class ViewModel { }                     // Too generic
class AcroFormView { }                  // "View" suggests UI view
class AcroFormData { }                  // "Data" too generic
```

---

### **Builder Classes**

**Pattern:** `{Feature}ViewModelBuilder`

✅ **Correct:**
```java
class AcroFormViewModelBuilder implements ViewModelBuilder<AcroFormViewModel> { }
class InvoiceViewModelBuilder implements ViewModelBuilder<InvoiceViewModel> { }
class EnrollmentApplicationViewModelBuilder implements ViewModelBuilder<EnrollmentApplicationViewModel> { }
```

❌ **Incorrect:**
```java
class AcroFormBuilder { }               // Missing "ViewModel"
class Builder { }                       // Too generic
class AcroFormViewBuilder { }           // Inconsistent naming
```

---

### **Helper/Utility Classes**

**Pattern:** `{Feature}{Purpose}Helper` or `{Feature}{Purpose}Util`

✅ **Correct:**
```java
class InvoiceCalculationHelper { }
class EnrollmentDateFormatUtil { }
class MemberPlanAggregatorHelper { }
```

❌ **Incorrect:**
```java
class Helper { }                        // Too generic
class Util { }                          // Too generic
class Tools { }                         // Wrong name
```

---

## Complete Example: Feature-Based Structure

For an enrollment system with multiple ViewModels:

### Directory Layout
```
src/main/java/com/example/demo/docgen/viewmodel/
├── ViewModelBuilder.java
├── ViewModelFactory.java
│
├── shared/
│   ├── DateFormatter.java
│   ├── AddressFlattener.java
│   └── NullSafeHelper.java
│
├── acroform/
│   ├── AcroFormViewModel.java
│   └── AcroFormViewModelBuilder.java
│
├── enrollment/
│   ├── EnrollmentViewModel.java
│   ├── EnrollmentViewModelBuilder.java
│   └── EnrollmentValidator.java
│
├── enrollment/document/
│   ├── EnrollmentDocumentViewModel.java
│   └── EnrollmentDocumentViewModelBuilder.java
│
├── member/
│   ├── MemberViewModel.java
│   ├── MemberViewModelBuilder.java
│   └── MemberDataTransformer.java
│
└── plan/
    ├── PlanViewModel.java
    ├── PlanViewModelBuilder.java
    ├── shared/
    │   └── PlanCalculator.java
    └── premium/
        ├── PremiumViewModel.java
        └── PremiumViewModelBuilder.java
```

### Java Code Structure

**Shared Interface & Factory:**
```java
package com.example.demo.docgen.viewmodel;

public interface ViewModelBuilder<T> {
    T build(Map<String, Object> rawData);
}

@Component
public class ViewModelFactory { ... }
```

**Shared Utilities:**
```java
package com.example.demo.docgen.viewmodel.shared;

@Component
public class DateFormatter {
    public String format(LocalDate date) { ... }
}

@Component
public class AddressFlattener {
    public String flatten(Map<String, Object> address) { ... }
}
```

**Feature 1: AcroForm**
```java
package com.example.demo.docgen.viewmodel.acroform;

@Data @Builder
public class AcroFormViewModel { ... }

@Component
public class AcroFormViewModelBuilder implements ViewModelBuilder<AcroFormViewModel> { ... }
```

**Feature 2: Enrollment**
```java
package com.example.demo.docgen.viewmodel.enrollment;

@Data @Builder
public class EnrollmentViewModel { ... }

@Component
public class EnrollmentViewModelBuilder implements ViewModelBuilder<EnrollmentViewModel> { 
    @Autowired private DateFormatter dateFormatter;
    @Autowired private EnrollmentValidator validator;
}

@Component
public class EnrollmentValidator { 
    public boolean isValid(Map<String, Object> data) { ... }
}
```

**Feature 2b: Enrollment Document (Sub-feature)**
```java
package com.example.demo.docgen.viewmodel.enrollment.document;

@Data @Builder
public class EnrollmentDocumentViewModel { ... }

@Component
public class EnrollmentDocumentViewModelBuilder 
    implements ViewModelBuilder<EnrollmentDocumentViewModel> { 
    @Autowired private EnrollmentValidator validator;
}
```

**Feature 3: Plan with Sub-features**
```java
package com.example.demo.docgen.viewmodel.plan;

@Data @Builder
public class PlanViewModel { ... }

@Component
public class PlanViewModelBuilder implements ViewModelBuilder<PlanViewModel> { 
    @Autowired private PlanCalculator calculator;
}

@Component
public class PlanCalculator { 
    public Double calculatePremium(...) { ... }
}
```

```java
package com.example.demo.docgen.viewmodel.plan.premium;

@Data @Builder
public class PremiumViewModel { ... }

@Component
public class PremiumViewModelBuilder implements ViewModelBuilder<PremiumViewModel> { 
    @Autowired private PlanCalculator calculator;
}
```

---

## YAML Template Configuration

When using ViewModels in templates, the `viewModelType` must match the class name (without "Builder"):

### With Flat Structure
```yaml
sections:
  - viewModelType: AcroForm           # Matches AcroFormViewModelBuilder
  - viewModelType: Invoice            # Matches InvoiceViewModelBuilder
```

### With Feature-Based Structure
```yaml
sections:
  - viewModelType: AcroForm           # Matches acroform/AcroFormViewModelBuilder
  - viewModelType: Enrollment         # Matches enrollment/EnrollmentViewModelBuilder
  - viewModelType: EnrollmentDocument # Matches enrollment/document/EnrollmentDocumentViewModelBuilder
  - viewModelType: PlanPremium        # Matches plan/premium/PremiumViewModelBuilder
```

**How ViewModelFactory Resolves:**
```java
// Takes class name, removes "Builder" suffix
// Looks up in registry regardless of package
String name = builder.getClass().getSimpleName();  // "AcroFormViewModelBuilder"
if (name.endsWith("Builder")) {
    name = name.substring(0, name.length() - 7);   // "AcroForm"
}
builderMap.put(name, builder);                      // Registry: "AcroForm" → builder
```

**Note:** ViewModelFactory discovers all builders via Spring's component scanning, regardless of package location!

---

## Best Practices

### 1. **Keep ViewModels Simple**
Each ViewModel should be a single, focused POJO.

✅ **Good:**
```java
@Data @Builder
public class EnrollmentViewModel {
    private String applicantName;       // One concern
    private Integer age;
    private Boolean isActive;
}
```

❌ **Bad:**
```java
@Data @Builder
public class EnrollmentViewModel {
    private String applicantName;
    private Integer age;
    private List<String> policyNumbers;
    private Map<String, Object> rawData;  // Over-complicated
    private Document xmlDocument;
}
```

### 2. **Separate Concerns**

Put business logic in builders or helpers, not ViewModels:

✅ **Good:**
```java
// ViewModel - pure data
@Data @Builder
public class InvoiceViewModel {
    private Double totalAmount;         // Result, not calculation
}

// Builder - calculation
public class InvoiceViewModelBuilder implements ViewModelBuilder<InvoiceViewModel> {
    public InvoiceViewModel build(Map<String, Object> rawData) {
        double total = items.stream()
            .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
            .sum();
        return InvoiceViewModel.builder().totalAmount(total).build();
    }
}
```

❌ **Bad:**
```java
// ViewModel doing calculation
@Data @Builder
public class InvoiceViewModel {
    private List<InvoiceItem> items;
    
    public Double getTotalAmount() {    // Logic in ViewModel!
        return items.stream()
            .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
            .sum();
    }
}
```

### 3. **Use Packages Hierarchically for Related Features**

Group related ViewModels:

```
enrollment/
├── EnrollmentViewModel.java
├── EnrollmentViewModelBuilder.java
└── spouse/                           # Related sub-feature
    ├── SpouseViewModel.java
    └── SpouseViewModelBuilder.java
```

### 4. **Extract Common Transformation Logic**

Don't repeat logic in multiple builders:

✅ **Good:**
```java
package com.example.demo.docgen.viewmodel.shared;

@Component
public class DateFormatter {
    public String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
    }
}

// Use in multiple builders
@Component
public class AcroFormViewModelBuilder implements ViewModelBuilder<AcroFormViewModel> {
    @Autowired private DateFormatter dateFormatter;
    
    public AcroFormViewModel build(Map<String, Object> rawData) {
        return AcroFormViewModel.builder()
            .dateOfBirth(dateFormatter.formatDate(...))
            .build();
    }
}
```

❌ **Bad:**
```java
// Duplicate formatting in each builder
public class AcroFormViewModelBuilder implements ViewModelBuilder<AcroFormViewModel> {
    public AcroFormViewModel build(Map<String, Object> rawData) {
        return AcroFormViewModel.builder()
            .dateOfBirth(date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
            .build();
    }
}

public class EnrollmentViewModelBuilder implements ViewModelBuilder<EnrollmentViewModel> {
    public EnrollmentViewModel build(Map<String, Object> rawData) {
        return EnrollmentViewModel.builder()
            .dateOfBirth(date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))  // Repeated!
            .build();
    }
}
```

### 5. **Document Your ViewModels**

Add JavaDoc explaining origin of data:

```java
/**
 * ViewModel for AcroForm PDF field population.
 * 
 * Created by transforming nested applicant data into flat, form-ready fields.
 * Date formatting and field concatenation handled during transformation.
 * 
 * Fields are designed to map 1:1 with PDF form field names.
 * 
 * @see AcroFormViewModelBuilder for transformation logic
 */
@Data @Builder
public class AcroFormViewModel {
    
    /**
     * Full name: concatenated from firstName + middleName (if present) + lastName
     * Example: "John Robert Doe"
     * Source: applicants[type='PRIMARY'].demographic
     */
    private String primaryFullName;
    
    /**
     * Formatted date of birth (MM/dd/yyyy)
     * Example: "05/15/1980"
     * Source: applicants[type='PRIMARY'].demographic.dateOfBirth
     */
    private String primaryDateOfBirth;
}
```

### 6. **Use Spring Component Scanning**

Ensure all builders are annotated correctly:

```java
@Component                                          // ✓ Required
public class MyViewModelBuilder implements ViewModelBuilder<MyViewModel> {
    @Autowired private SomeDependency dependency;  // ✓ Auto-injected
}
```

---

## Migration Guide: Flat → Feature-Based

If starting with flat package and need to migrate:

### Step 1: Create Sub-packages
```bash
mkdir -p src/main/java/com/example/demo/docgen/viewmodel/acroform
mkdir -p src/main/java/com/example/demo/docgen/viewmodel/enrollment
mkdir -p src/main/java/com/example/demo/docgen/viewmodel/shared
```

### Step 2: Move Files
```bash
mv AcroFormViewModel.java acroform/
mv AcroFormViewModelBuilder.java acroform/
# ... repeat for other ViewModels
```

### Step 3: Update Package Declarations
```java
// FROM:
package com.example.demo.docgen.viewmodel;

// TO:
package com.example.demo.docgen.viewmodel.acroform;
```

### Step 4: Update Imports
IDE refactoring tools (Eclipse/IntelliJ) can do this automatically.

### Step 5: Test
ViewModelFactory will still discover all builders automatically through component scanning.

```bash
mvn test
```

---

## Summary: Choosing Your Structure

| Project Size | ViewModels | Recommended Structure |
|--------------|-----------|----------------------|
| Small | 1-3 | Flat (Option 1) |
| Medium | 4-8 | Feature-Based (Option 2) |
| Large | 9+ | Hybrid (Option 4) |
| Very Complex | 10+ with sub-features | Feature-Based with sub-packages |

---

## Quick Checklist

When adding a new ViewModel:

- [ ] Create ViewModel POJO with `@Data @Builder`
- [ ] Create Builder implementing `ViewModelBuilder<YourViewModel>`
- [ ] Annotate builder with `@Component`
- [ ] Use correct naming: `{Feature}ViewModel` and `{Feature}ViewModelBuilder`
- [ ] Place in appropriate package (flat, feature-based, or hybrid)
- [ ] Add JavaDoc with data source and transformations
- [ ] Extract common logic to shared utilities if repeated
- [ ] Update template YAML with correct `viewModelType`
- [ ] Add unit tests in `src/test/java` with same package structure
- [ ] Run `mvn test` to verify discovery and no errors
