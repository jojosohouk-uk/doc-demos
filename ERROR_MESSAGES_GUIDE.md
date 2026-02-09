# Error Messages Guide: Template Definition and Resource Loading

## Overview
This document explains where error messages are generated when a template definition YAML file or referenced resources (FreeMarker files, PDF files) are not found or cannot be loaded.

---

## Main Entry Point: TemplateLoader.java

The primary component responsible for template loading and error generation is **[TemplateLoader.java](src/main/java/com/example/demo/docgen/service/TemplateLoader.java)**.

---

## Error Scenarios and Message Generation

### 1. **TEMPLATE_NOT_FOUND** - Template Definition File Not Found

#### Scenario A: Config Server Enabled (Remote Loading)
**Location:** [TemplateLoader.java, lines 450-456](src/main/java/com/example/demo/docgen/service/TemplateLoader.java#L450-L456)

```java
// Not found on config-server
throw new com.example.demo.docgen.exception.TemplateLoadingException(
    "TEMPLATE_NOT_FOUND",
    "Template not found for id '" + templateId + "' on config-server at " + configServerUri
);
```

**Triggered when:**
- `docgen.templates.remote-enabled` is `true`
- Config Server URL is configured in `spring.cloud.config.uri`
- Template definition file is not found at the remote Config Server location

#### Scenario B: Local Classpath (Remote Disabled)
**Location:** [TemplateLoader.java, lines 477-481](src/main/java/com/example/demo/docgen/service/TemplateLoader.java#L477-L481)

```java
// Not found locally
throw new com.example.demo.docgen.exception.TemplateLoadingException(
    "TEMPLATE_NOT_FOUND",
    "Template not found for id '" + templateId + "'. Check src/main/resources/templates or enable remote templates."
);
```

**Triggered when:**
- `docgen.templates.remote-enabled` is `false`
- Template definition file is not found in classpath (`src/main/resources/`)

---

### 2. **UNRESOLVED_PLACEHOLDER** - Variable Substitution Failed

**Location:** [TemplateLoader.java, lines 300-305](src/main/java/com/example/demo/docgen/service/TemplateLoader.java#L300-L305)

```java
private String resolvePlaceholders(String templateId, Map<String, Object> variables) {
    if (templateId == null) return null;
    Matcher m = PLACEHOLDER.matcher(templateId);
    StringBuffer sb = new StringBuffer();

    boolean found = false;
    while (m.find()) {
        found = true;
        String path = m.group(1);
        Object val = getValueFromPath(variables, path);
        if (val == null) {
            throw new com.example.demo.docgen.exception.TemplateLoadingException(
                "UNRESOLVED_PLACEHOLDER",
                "Unresolved placeholder '" + path + "' in template id: " + templateId
            );
        }
        // ...
    }
}
```

**Triggered when:**
- Template ID contains `${...}` placeholders (e.g., `templates/${tenant}/form.yaml`)
- The variable path inside `${...}` does not exist in the provided variables map
- Example: `${data.missing}` when `data` object or `missing` field is not in variables

---

### 3. **PDF/FreeMarker Resource Not Found** - Referenced Resources in Template Definition

**Location:** [TemplateLoader.java, lines 630-675](src/main/java/com/example/demo/docgen/service/TemplateLoader.java#L630-L675)

```java
private InputStream getInputStream(String path) throws TemplateLoadingException {
    // If remote fetching is enabled, use Config Server as the single source of truth
    if (remoteEnabled && configServerUri != null && !configServerUri.isEmpty()) {
        try {
            InputStream remoteStream = getFromConfigServer(path);
            if (remoteStream != null) {
                log.info("Loaded template from Config Server (HTTP): {}", path);
                return remoteStream;
            }
        } catch (IOException e) {
            // Remote is the source of truth - don't fall back to local sources
            log.error("Failed to fetch template from Config Server for path '{}': {}", path, e.getMessage());
            throw new TemplateLoadingException(
                "CONFIG_SERVER_ERROR",
                "Template not found on Config Server at " + configServerUri,
                e
            );
        }
    }

    // Load from classpath only (single source of truth)
    try {
        ClassPathResource resource = new ClassPathResource(path);
        if (resource.exists()) {
            return resource.getInputStream();
        }
    } catch (Exception e) {
        log.debug("Template not found in classpath: {}", path);
    }

    throw new TemplateLoadingException(
        "TEMPLATE_NOT_FOUND",
        buildDetailedErrorMessage(path)
    );
}
```

**Triggered when:**
- Template definition YAML references a PDF file, FreeMarker template, or signature image
- The referenced resource path (specified in `templatePath` field within YAML) is invalid or file doesn't exist
- Examples: 
  - `header.pdf` referenced in YAML but not found in `src/main/resources/tenant-a/templates/`
  - `signature.ftl` referenced but missing

#### Detailed Error Message
**Location:** [TemplateLoader.java, lines 676-687](src/main/java/com/example/demo/docgen/service/TemplateLoader.java#L676-L687)

```java
private String buildDetailedErrorMessage(String path) {
    StringBuilder message = new StringBuilder();
    message.append("PDF template resource not found: ").append(path).append("\n\n");
    message.append("Checked the following location:\n");
    message.append("  • Classpath/Resources: src/main/resources/").append(path).append("\n\n");
    message.append("If this is a tenant-specific resource, verify that the namespace and file path are correct.\n");
    message.append("Expected structure: src/main/resources/{namespace}/templates/{resourcePath}");
    
    return message.toString();
}
```

---

### 4. **CONFIG_SERVER_ERROR** - Config Server Unreachable

**Location:** [TemplateLoader.java, lines 440-448](src/main/java/com/example/demo/docgen/service/TemplateLoader.java#L440-L448)

```java
if (e instanceof java.net.ConnectException || e instanceof java.net.SocketTimeoutException
        || (e.getCause() != null && (e.getCause() instanceof java.net.ConnectException || e.getCause() instanceof java.net.SocketTimeoutException))) {
    throw new com.example.demo.docgen.exception.TemplateLoadingException(
            "CONFIG_SERVER_ERROR",
            "Failed to contact Config Server at " + configServerUri,
            e
    );
}
```

**Triggered when:**
- Config Server is enabled (`docgen.templates.remote-enabled=true`)
- Network connectivity issue prevents reaching the server at `spring.cloud.config.uri`
- Timeout connecting to Config Server

---

## HTTP Response Mapping

### DocumentController.java (Primary Handler)
**Location:** [DocumentController.java, lines 42-68](src/main/java/com/example/demo/docgen/controller/DocumentController.java#L42-L68)

The controller catches `TemplateLoadingException` and maps error codes to HTTP status codes:

```java
catch (com.example.demo.docgen.exception.TemplateLoadingException tle) {
    String code = tle.getCode();
    String description = tle.getDescription();

    java.util.Map<String, String> body = new java.util.HashMap<>();
    body.put("code", code);
    body.put("description", description);

    if ("TEMPLATE_NOT_FOUND".equals(code)) {
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);        // 404
    } else if ("UNRESOLVED_PLACEHOLDER".equals(code)) {
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);     // 400
    }

    return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR); // 500
}
```

### GlobalExceptionHandler.java (Fallback Handler)
**Location:** [GlobalExceptionHandler.java, lines 55-70](src/main/java/com/example/demo/docgen/controller/GlobalExceptionHandler.java#L55-L70)

```java
private HttpStatus determineHttpStatus(String errorCode) {
    return switch (errorCode) {
        case "TEMPLATE_NOT_FOUND", "RESOURCE_NOT_FOUND" -> HttpStatus.NOT_FOUND;              // 404
        case "UNSUPPORTED_TEMPLATE_FORMAT" -> HttpStatus.BAD_REQUEST;                        // 400
        case "INVALID_REQUEST" -> HttpStatus.BAD_REQUEST;                                    // 400
        case "TEMPLATE_PARSE_ERROR" -> HttpStatus.BAD_REQUEST;                               // 400
        case "RESOURCE_READ_ERROR" -> HttpStatus.INTERNAL_SERVER_ERROR;                      // 500
        case "CONFIG_SERVER_ERROR" -> HttpStatus.SERVICE_UNAVAILABLE;                        // 503
        default -> HttpStatus.INTERNAL_SERVER_ERROR;                                         // 500
    };
}
```

---

## Error Response JSON Format

All errors are returned as JSON in the following format:

```json
{
  "code": "ERROR_CODE",
  "description": "Human-readable error description"
}
```

### Example Responses:

**Template Not Found (404):**
```json
{
  "code": "TEMPLATE_NOT_FOUND",
  "description": "Template not found for id 'enrollment-form.yaml'. Check src/main/resources/templates or enable remote templates."
}
```

**Unresolved Placeholder (400):**
```json
{
  "code": "UNRESOLVED_PLACEHOLDER",
  "description": "Unresolved placeholder 'data.tenantId' in template id: templates/${data.tenantId}/form.yaml"
}
```

**Resource Not Found (404):**
```json
{
  "code": "TEMPLATE_NOT_FOUND",
  "description": "PDF template resource not found: tenant-a/templates/header.pdf\n\nChecked the following location:\n  • Classpath/Resources: src/main/resources/tenant-a/templates/header.pdf\n\nIf this is a tenant-specific resource, verify that the namespace and file path are correct.\nExpected structure: src/main/resources/{namespace}/templates/{resourcePath}"
}
```

**Config Server Unreachable (503):**
```json
{
  "code": "CONFIG_SERVER_ERROR",
  "description": "Failed to contact Config Server at http://localhost:8888"
}
```

---

## Summary Table

| Error Code | HTTP Status | Triggered When | Location |
|-----------|---------|---------|----------|
| `TEMPLATE_NOT_FOUND` | 404 | Template definition YAML file not found (local or remote) | TemplateLoader.java:450, 477 |
| `UNRESOLVED_PLACEHOLDER` | 400 | Variable placeholder in template ID cannot be resolved | TemplateLoader.java:303 |
| `CONFIG_SERVER_ERROR` | 503 | Cannot connect to remote Config Server | TemplateLoader.java:443 |
| `TEMPLATE_NOT_FOUND` | 404 | Referenced PDF/FreeMarker resource in YAML not found | TemplateLoader.java:670 |
| `UNSUPPORTED_TEMPLATE_FORMAT` | 400 | Template file has unsupported extension (not .yaml, .yml, or .json) | TemplateLoader.java:472 |

---

## Key Configuration Properties

- **`spring.cloud.config.uri`** - Config Server location (e.g., `http://localhost:8888`)
- **`docgen.templates.remote-enabled`** - Enable/disable remote template loading (default: `true`)
- **`spring.profiles.active`** - Active Spring profile for Config Server lookups
- **`spring.cloud.config.label`** - Git branch/label for Config Server (default: `main`)
