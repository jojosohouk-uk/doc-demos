# AcroForm Field Styling - Practical Examples

This file provides ready-to-use YAML template examples with field styling configured for common scenarios.

## Example 1: Enrollment Application with Required/Optional Fields

Save as: `src/main/resources/common-templates/templates/styled-enrollment.yaml`

```yaml
sections:
  - sectionId: enrollment-with-styling
    type: ACROFORM
    templatePath: templates/forms/enrollment.pdf
    
    fieldMappingGroups:
      # Group 1: Personal Information
      - mappingType: DIRECT
        defaultStyle:
          fontSize: 11
          textColor: 0x000000
          borderColor: 0xCCCCCC
        
        fields:
          firstName: applicant.firstName
          lastName: applicant.lastName
          middleName: applicant.middleName
          dateOfBirth: applicant.dateOfBirth
          gender: applicant.gender
        
        fieldStyles:
          # Required fields - bold, red star indicator via styling
          firstName:
            bold: true
            textColor: 0xFF0000
          
          lastName:
            bold: true
            textColor: 0xFF0000
          
          dateOfBirth:
            bold: true
            textColor: 0xFF0000
          
          # Optional fields - normal style
          middleName:
            textColor: 0x666666  # Gray
          
          gender:
            alignment: CENTER
      
      # Group 2: Contact Information
      - mappingType: JSONATA
        defaultStyle:
          fontSize: 10
          borderColor: 0xDDDDDD
        
        fields:
          phoneNumber: applicant.contactInfo.phone
          emailAddress: applicant.contactInfo.email
          website: applicant.contactInfo.website
        
        fieldStyles:
          phoneNumber:
            bold: true
            fontSize: 11
          
          emailAddress:
            textColor: 0x0066CC  # Blue
          
          website:
            textColor: 0x008000  # Green
            italic: true
      
      # Group 3: Status and System Fields (Read-only)
      - mappingType: DIRECT
        defaultStyle:
          fontSize: 10
          backgroundColor: 0xF5F5F5
          textColor: 0x666666
          readOnly: true
        
        fields:
          applicationId: applicant.id
          applicationDate: applicant.createdDate
          status: applicant.status
          processedBy: applicant.processedBy
```

## Example 2: Invoice/Payment Form with Totals

Save as: `src/main/resources/common-templates/templates/styled-invoice.yaml`

```yaml
sections:
  - sectionId: invoice-form
    type: ACROFORM
    templatePath: templates/forms/invoice.pdf
    
    fieldMappingGroups:
      # Line items group
      - mappingType: JSONATA
        basePath: items[0]
        defaultStyle:
          fontSize: 11
          alignment: RIGHT
        
        fields:
          itemDescription: description
          itemQuantity: quantity
          itemUnitPrice: unitPrice
          itemSubtotal: "$round(quantity * unitPrice, 2)"
      
      # Totals group - read-only with highlighting
      - mappingType: JSONATA
        defaultStyle:
          readOnly: true
          fontSize: 12
          bold: true
          backgroundColor: 0xFFFF99  # Light yellow
        
        fields:
          subtotal: "$sum(items.(quantity * unitPrice))"
          taxAmount: "$sum(items.(quantity * unitPrice)) * 0.08"
          totalAmount: "$sum(items.(quantity * unitPrice)) * 1.08"
        
        fieldStyles:
          subtotal:
            backgroundColor: 0xFFFFCC  # Lighter yellow
          
          totalAmount:
            fontSize: 14
            textColor: 0xFF0000  # Red for emphasis
            backgroundColor: 0xFFFF99
```

## Example 3: Employee Information Form with Departments

```yaml
sections:
  - sectionId: employee-form
    type: ACROFORM
    templatePath: templates/forms/employee.pdf
    
    fieldMappingGroups:
      # Personal Information
      - mappingType: DIRECT
        defaultStyle:
          fontSize: 11
          borderWidth: 1
          borderColor: 0x333333
        
        fields:
          fullName: employee.name
          employeeId: employee.id
          hireDate: employee.hireDate
          dateOfBirth: employee.dateOfBirth
        
        fieldStyles:
          fullName:
            bold: true
            fontSize: 12
          
          employeeId:
            readOnly: true
            backgroundColor: 0xF0F0F0
      
      # Department and Position
      - mappingType: JSONATA
        defaultStyle:
          fontSize: 11
          backgroundColor: 0xE8F4F8
        
        fields:
          department: "departments[name=employee.department].code"
          position: employee.position
          manager: "employees[id=employee.managerId].name"
          level: employee.level
        
        fieldStyles:
          department:
            alignment: CENTER
            bold: true
          
          position:
            bold: true
          
          manager:
            readOnly: true
            textColor: 0x0066CC
      
      # Compensation (Read-only)
      - mappingType: DIRECT
        defaultStyle:
          readOnly: true
          fontSize: 11
          backgroundColor: 0xFFF4E6
          textColor: 0x333333
        
        fields:
          salary: employee.compensation.salary
          bonus: employee.compensation.bonus
          startDate: employee.compensation.startDate
        
        fieldStyles:
          salary:
            bold: true
            fontSize: 12
            alignment: RIGHT
```

## Example 4: Medical Form with Highlighted Sections

```yaml
sections:
  - sectionId: medical-form
    type: ACROFORM
    templatePath: templates/forms/medical.pdf
    
    fieldMappingGroups:
      # Patient Demographics
      - mappingType: DIRECT
        defaultStyle:
          fontSize: 10
        
        fields:
          patientName: patient.name
          dateOfBirth: patient.dob
          patientId: patient.mrn
          phone: patient.phone
          email: patient.email
        
        fieldStyles:
          patientId:
            readOnly: true
            backgroundColor: 0xF5F5F5
      
      # Allergies (Important - highlighted)
      - mappingType: JSONATA
        defaultStyle:
          backgroundColor: 0xFFE6E6  # Light red
          bold: true
          fontSize: 11
        
        fields:
          allergies: "$join(patient.allergies, ', ')"
          criticalAllergies: patient.criticalAlergies
        
        fieldStyles:
          criticalAllergies:
            textColor: 0xFF0000  # Red text
            bold: true
            fontSize: 12
      
      # Medications
      - mappingType: DIRECT
        defaultStyle:
          fontSize: 10
        
        fields:
          currentMedications: patient.medications
          dosage: patient.dosageInfo
          prescribedBy: patient.doctor
      
      # Diagnosis (Read-only)
      - mappingType: DIRECT
        defaultStyle:
          readOnly: true
          backgroundColor: 0xF0F0F0
          fontSize: 10
        
        fields:
          diagnosis: patient.diagnosis
          diagnosisDate: patient.diagnosisDate
          icd10Code: patient.icd10
```

## Example 5: Tax Return Form with Multiple Sections

```yaml
sections:
  - sectionId: tax-form-1040
    type: ACROFORM
    templatePath: templates/forms/tax-1040.pdf
    
    fieldMappingGroups:
      # Header/Filing Information
      - mappingType: DIRECT
        defaultStyle:
          fontSize: 11
          bold: true
        
        fields:
          taxYear: "2024"
          filingStatus: taxpayer.filingStatus
          name: taxpayer.name
          ssn: taxpayer.ssn
        
        fieldStyles:
          ssn:
            hidden: true  # Mask SSN input
      
      # Income Section
      - mappingType: DIRECT
        defaultStyle:
          fontSize: 10
          alignment: RIGHT
        
        fields:
          wages: income.w2Wages
          interestIncome: income.interestIncome
          dividendIncome: income.dividendIncome
          capitalGains: income.capitalGains
      
      # Deductions (Read-only calculations)
      - mappingType: JSONATA
        defaultStyle:
          readOnly: true
          backgroundColor: 0xE6F2E6  # Light green
          fontSize: 10
          alignment: RIGHT
        
        fields:
          totalIncome: "$sum(income.*)"
          standardDeduction: "taxpayer.standardDeduction"
          taxableIncome: "($sum(income.*) - taxpayer.standardDeduction)"
        
        fieldStyles:
          taxableIncome:
            backgroundColor: 0xCCE6CC
            bold: true
      
      # Tax Calculation (Highlighted)
      - mappingType: JSONATA
        defaultStyle:
          readOnly: true
          fontSize: 11
          bold: true
          backgroundColor: 0xFFFFCC  # Light yellow
          alignment: RIGHT
        
        fields:
          estimatedTax: "($sum(income.*) - taxpayer.standardDeduction) * 0.22"
          taxWithheld: income.taxWithheld
          refundOrOwe: "income.taxWithheld - (($sum(income.*) - taxpayer.standardDeduction) * 0.22)"
        
        fieldStyles:
          estimatedTax:
            backgroundColor: 0xFFFFE6
          
          taxWithheld:
            backgroundColor: 0xFFFFE6
          
          refundOrOwe:
            backgroundColor: 0xFFFF99
            textColor: 0x003300  # Dark green for positive
```

## Usage Instructions

1. **Choose an Example**: Select the template that best matches your use case
2. **Customize Field Names**: Update field names to match your PDF form
3. **Adjust Colors**: Modify color values (0xRRGGBB format) as needed
4. **Update Data Paths**: Change data paths (first.last, jsonpath, jsonata) to match your data structure
5. **Test**: Generate a document and verify styling in PDF viewer

## Styling Patterns

### Pattern 1: Required Fields
```yaml
fieldStyles:
  requiredField:
    bold: true
    textColor: 0xFF0000  # Red
```

### Pattern 2: Read-Only Fields
```yaml
fieldStyles:
  readOnlyField:
    readOnly: true
    backgroundColor: 0xF0F0F0  # Light gray
    textColor: 0x666666  # Dark gray text
```

### Pattern 3: Highlighted Sections
```yaml
defaultStyle:
  backgroundColor: 0xFFFF99  # Light yellow
  bold: true
```

### Pattern 4: Important/Alert Fields
```yaml
fieldStyles:
  alertField:
    backgroundColor: 0xFFE6E6  # Light red
    textColor: 0xFF0000  # Red
    bold: true
    fontSize: 12
```

### Pattern 5: Calculation/System Fields
```yaml
fieldStyles:
  calculatedField:
    readOnly: true
    backgroundColor: 0xE8F4F8  # Light blue
    italic: true
```

