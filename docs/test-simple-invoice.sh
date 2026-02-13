#!/bin/bash

echo "1️⃣  Creating template..."
python3 << 'PYEOF'
import os
from openpyxl import Workbook
from openpyxl.workbook.defined_name import DefinedName
from openpyxl.styles import Font, PatternFill

wb = Workbook()
sheet = wb.active
sheet.title = "Invoice"
sheet['A1'] = "INVOICE"
sheet['A1'].font = Font(bold=True, size=14)

sheet['A3'] = "Company"
sheet['B3'] = "Invoice #"
sheet['C3'] = "Date"

for col in ['A', 'B', 'C']:
    sheet[col + '4'] = ""

sheet['A6'] = "Description"
sheet['B6'] = "Qty"
for col in ['A', 'B']:
    sheet[col + '6'].font = Font(bold=True)

for row in range(7, 10):
    for col in ['A', 'B']:
        sheet[col + str(row)] = ""

wb.defined_names["companyName"] = DefinedName("companyName", "$'Invoice'!$A$4")
wb.defined_names["invoiceNumber"] = DefinedName("invoiceNumber", "$'Invoice'!$B$4")
wb.defined_names["invoiceDate"] = DefinedName("invoiceDate", "$'Invoice'!$C$4")
wb.defined_names["items"] = DefinedName("items", "$'Invoice'!$A$7:$B$9")

os.makedirs("src/main/resources/common-templates/templates", exist_ok=True)
wb.save("src/main/resources/common-templates/templates/test-invoice.xlsx")
print("✓ Template created")
PYEOF

echo ""
echo "2️⃣  Filling template..."
curl -X POST http://localhost:8080/api/documents/fill-excel \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "common-templates",
    "templateId": "templates/test-invoice.xlsx",
    "data": {
      "companyName": "TechCorp Inc",
      "invoiceNumber": "INV-2026-042",
      "invoiceDate": "2026-02-13",
      "items": [
        ["Consulting", 10],
        ["Development", 40]
      ]
    }
  }' \
  --output test-invoice-filled.xlsx

echo ""
echo "3️⃣  Verifying results..."
curl -X POST http://localhost:8080/api/documents/verify-excel \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "common-templates",
    "templateId": "templates/test-invoice.xlsx",
    "data": {
      "companyName": "TechCorp Inc",
      "invoiceNumber": "INV-2026-042",
      "invoiceDate": "2026-02-13",
      "items": [
        ["Consulting", 10],
        ["Development", 40]
      ]
    },
    "options": {
      "verifyKeys": ["companyName", "invoiceNumber", "invoiceDate", "items"]
    }
  }' | jq .

echo ""
echo "✓ Done! Check: test-invoice-filled.xlsx"