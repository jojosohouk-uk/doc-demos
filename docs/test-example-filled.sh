#!/bin/bash

echo "📋 Testing Excel template filling (using existing example.xlsx)..."
echo ""

# The example.xlsx already exists in config-repo and src/main/resources
# It has named ranges: customerName, orderDate, items

echo "1️⃣  Filling template with data..."
curl -X POST http://localhost:8080/api/documents/fill-excel \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "common-templates",
    "templateId": "templates/example.xlsx",
    "data": {
      "customerName": "John Doe",
      "orderDate": "2026-02-13",
      "items": [
        ["Widget A", 5, 29.99],
        ["Widget B", 10, 15.50],
        ["Widget C", 3, 99.99]
      ]
    }
  }' \
  --output test-example-filled.xlsx

if [ $? -eq 0 ]; then
    echo "✓ File created: test-example-filled.xlsx"
else
    echo "✗ Failed to fill template"
    exit 1
fi

echo ""
echo "2️⃣  Verifying filled values..."
curl -s -X POST http://localhost:8080/api/documents/verify-excel \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "common-templates",
    "templateId": "templates/example.xlsx",
    "data": {
      "customerName": "John Doe",
      "orderDate": "2026-02-13",
      "items": [
        ["Widget A", 5, 29.99],
        ["Widget B", 10, 15.50],
        ["Widget C", 3, 99.99]
      ]
    },
    "options": {
      "verifyKeys": ["customerName", "orderDate", "items"]
    }
  }' | jq .

echo ""
echo "✓ Done! Open the file to verify:"
echo "  open test-example-filled.xlsx  # macOS"
echo "  libreoffice test-example-filled.xlsx  # Linux"