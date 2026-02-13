#!/bin/bash

echo "🔍 Testing FreeMarker field filling..."
echo ""

# Test 1: Fill the template
echo "1️⃣  Filling template..."
curl -s -X POST http://localhost:8080/api/documents/fill-excel \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "common-templates",
    "templateId": "templates/example.xlsx",
    "data": {
      "customerName": "Test Customer",
      "orderDate": "2026-02-13"
    }
  }' --output /tmp/verify-test.xlsx

if [ -f /tmp/verify-test.xlsx ]; then
    echo "✓ File created successfully"
else
    echo "✗ Failed to create file"
    exit 1
fi

# Test 2: Use verify endpoint
echo ""
echo "2️⃣  Verifying filled values..."
curl -s -X POST http://localhost:8080/api/documents/verify-excel \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "common-templates",
    "templateId": "templates/example.xlsx",
    "data": {
      "customerName": "Test Customer",
      "orderDate": "2026-02-13"
    },
    "options": {
      "verifyKeys": ["customerName", "orderDate"]
    }
  }' | python3 -m json.tool

# Test 3: Inspect with Python
echo ""
echo "3️⃣  Inspecting Excel cells..."
python3 << 'PYEOF'
from openpyxl import load_workbook

wb = load_workbook('/tmp/verify-test.xlsx')
sheet = wb.active

print(f"✓ Sheet name: {sheet.title}")
print(f"  Cell A2 (customerName): {sheet['A2'].value}")
print(f"  Cell B2 (orderDate): {sheet['B2'].value}")

if sheet['A2'].value == "Test Customer":
    print("  ✅ customerName is FILLED correctly")
else:
    print(f"  ❌ customerName is NOT filled. Found: {sheet['A2'].value}")

if sheet['B2'].value == "2026-02-13":
    print("  ✅ orderDate is FILLED correctly")
else:
    print(f"  ❌ orderDate is NOT filled. Found: {sheet['B2'].value}")
PYEOF

echo ""
echo "✓ Test complete! Check results above."