cd /workspaces/demos
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

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
  }'