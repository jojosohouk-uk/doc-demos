curl -X POST http://localhost:8080/api/documents/generate -H "Content-Type: application/json" -d @src/main/resources/examples/pdfbox-component-request.json --output pdfbox-component-output.pdf && ls -l pdfbox-component-output.pdf

curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d @src/main/resources/examples/pdfbox-viewmodel-request.json \
  --output pdfbox-viewmodel-output.pdf

curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d @src/main/resources/test-data/composite-dental-ny.json \
  --output composite-dental-ny-output.pdf
  

  
curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d @tech-pocs/src/main/resources/example-request-namespace.json \
  --output example-request-namespace.pdf
  

    
curl -X POST http://localhost:8080/api/documents/generate \
  -H "Content-Type: application/json" \
  -d @src/main/resources/test-data/namespace-placeholder-state.json \
  --output namespace-placeholder-state.pdf
  

