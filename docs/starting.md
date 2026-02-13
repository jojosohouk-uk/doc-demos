# Starting the Document Generation Service

## Option 1: Local Development (Recommended)

Uses local `config-repo/` templates. Config Server is disabled.

```bash
cd /workspaces/doc-demos
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

## Option 2: With Config Server

Requires a running Spring Cloud Config Server (http://localhost:8888).

```bash
# Terminal 1: Start the Config Server
cd /workspaces/doc-demos/config-server
mvn spring-boot:run

# Terminal 2: Start the main app
cd /workspaces/doc-demos
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

---

## Test Endpoints

### Generate PDF (AcroForm, FreeMarker, etc.)

```bash
curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "tenant-a",
    "templateId": "composite-enrollment",
    "data": {
      "applicant": {
        "firstName": "John",
        "lastName": "Doe"
      }
    }
  }' --output document.pdf
```

### Fill Excel Template

```bash
curl -X POST http://localhost:8080/api/documents/fill-excel \
  -H "Content-Type: application/json" \
  -d @docs/request-excel.json \
  --output filled.xlsx
```

### Verify Filled Excel (JSON Summary)

```bash
curl -X POST http://localhost:8080/api/documents/verify-excel \
  -H "Content-Type: application/json" \
  -d @docs/request-excel.json | jq .
```

### Health Check

```bash
curl http://localhost:8080/api/documents/health
```