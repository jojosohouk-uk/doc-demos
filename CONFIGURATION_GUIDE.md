# Developer Configuration Guide: Template & Resource Storage

This guide explains how to configure the document generation service to load templates and resources from different sources: **local classpath**, **Spring Cloud Config Server**, and **external file storage services**.

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Configuration Modes](#configuration-modes)
3. [Local Classpath Loading](#local-classpath-loading)
4. [Spring Cloud Config Server](#spring-cloud-config-server)
5. [External File Storage Service](#external-file-storage-service)
6. [Combining Multiple Sources](#combining-multiple-sources)
7. [Caching and Performance](#caching-and-performance)
8. [Quick Start Examples](#quick-start-examples)

---

## Architecture Overview

The application has a **layered resource loading strategy**:

```
┌─────────────────────────────────────────────────────────┐
│         Document Generation Request                      │
│         (POST /api/documents/generate)                   │
└───────────────────┬─────────────────────────────────────┘
                    │
                    ▼
        ┌─────────────────────────┐
        │   TemplateLoader        │
        │  (resolves + caches)    │
        └───────────┬─────────────┘
                    │
        ┌───────────┴─────────────┐
        │                         │
        ▼                         ▼
  ┌──────────────┐      ┌──────────────────┐
  │  Template    │      │   Resources      │
  │  Definition  │      │  (PDF/FTL/etc)   │
  │  (YAML/JSON) │      │                  │
  └──────┬───────┘      └────────┬─────────┘
         │                       │
    ┌────┴──────────────┬────────┴─────────────┐
    │                   │                      │
    ▼                   ▼                      ▼
┌────────────┐    ┌──────────────┐     ┌─────────────────┐
│  Classpath │    │ Config Server│     │ External Storage│
│    /       │    │              │     │  (HTTP/S3/GCS)  │
│ Filesystem │    │  (Remote)    │     │                 │
└────────────┘    └──────────────┘     └─────────────────┘
```

---

## Configuration Modes

### Mode 1: Local Classpath Only (Development)
- **Use case:** Local development, quick prototyping
- **Templates stored in:** `src/main/resources/templates/` or `src/main/resources/{namespace}/templates/`
- **Resources:** Classpath only
- **Configuration:**
  ```yaml
  docgen:
    templates:
      remote-enabled: false
      cache-enabled: false
  ```

### Mode 2: Config Server Only (Production-Ready)
- **Use case:** Centralized template management, multi-environment
- **Templates stored in:** Git repository served by Spring Cloud Config Server
- **Resources:** External storage service
- **Configuration:**
  ```yaml
  spring:
    cloud:
      config:
        uri: http://config-server:8888
        label: main
  docgen:
    templates:
      remote-enabled: true
    resources:
      storage-enabled: true
      base-url: http://storage-service:9090
  ```

### Mode 3: Hybrid (Mixed)
- **Use case:** Fallback mechanism, gradual migration
- **Templates stored in:** Config Server (primary), classpath (fallback)
- **Resources:** External storage
- **Configuration:**
  ```yaml
  spring:
    cloud:
      config:
        uri: http://config-server:8888
  docgen:
    templates:
      remote-enabled: true
    resources:
      storage-enabled: true
  ```

---

## Local Classpath Loading

### Configuration

**File:** `src/main/resources/application.yml`

```yaml
docgen:
  templates:
    remote-enabled: false        # Disable remote loading
    cache-enabled: false         # Optional: set to true for production
```

### Directory Structure

```
src/main/resources/
├── templates/
│   ├── form.yaml              # Default/common templates
│   ├── enrollment.json
│   └── forms/
│       ├── header.pdf
│       └── signature.ftl
├── common-templates/
│   └── templates/
│       ├── base-enrollment.yaml
│       └── forms/
│           ├── header.pdf
│           └── footer.ftl
├── tenant-a/
│   └── templates/
│       ├── enrollment.yaml
│       └── forms/
│           ├── applicant-form.pdf
│           └── signature.ftl
└── tenant-b/
    └── templates/
        ├── enrollment.yaml
        └── forms/
            └── header.pdf
```

### Loading Templates

```java
// Load from default namespace
DocumentTemplate template = templateLoader.loadTemplate("enrollment-form.yaml");

// Load from specific namespace/tenant
DocumentTemplate template = templateLoader.loadTemplate("tenant-a", "enrollment.yaml");

// Load with variable substitution
Map<String, Object> variables = new HashMap<>();
variables.put("tenantId", "tenant-a");
DocumentTemplate template = templateLoader.loadTemplate(
    "templates/${tenantId}/form.yaml",
    variables
);
```

### Example: YAML Template Definition

**File:** `src/main/resources/tenant-a/templates/enrollment.yaml`

```yaml
baseTemplate: common-templates:base-enrollment    # Reference from another namespace
namespace: tenant-a

sections:
  - id: header
    type: PDF_RENDERER
    templatePath: forms/header.pdf
    
  - id: form
    type: ACROFORM_RENDERER
    templatePath: forms/applicant-form.pdf
    
  - id: signature
    type: FREEMARKER_RENDERER
    templatePath: freemarker/signature.ftl
    variables:
      - name: companyName
        jspath: $.company
      - name: signatureDate
        jspath: $.date
```

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| `TEMPLATE_NOT_FOUND` | File missing from classpath | Verify file path in `src/main/resources/{namespace}/templates/` |
| Template loads but resource is missing | Resource path wrong in YAML | Check `templatePath` fields reference existing files |
| Namespace not found | Namespace folder missing | Create `src/main/resources/{namespace}/` directory |

---

## Spring Cloud Config Server

### Prerequisites

1. **Config Server running** at configured `spring.cloud.config.uri`
2. **Git repository** with template files
3. **Proper structure** in git repo:
   ```
   config-repo/
   ├── doc-gen-service-dev.yml
   ├── doc-gen-service-prod.yml
   ├── common-templates/
   │   └── templates/
   │       ├── base-form.yaml
   │       └── forms/
   ├── tenant-a/
   │   └── templates/
   │       ├── enrollment.yaml
   │       └── forms/
   └── tenant-b/
       └── templates/
       └── forms/
   ```

### Configuration

**File:** `src/main/resources/application-dev.yml`

```yaml
spring:
  application:
    name: doc-gen-service              # Application name for config server
  config:
    import: optional:configserver:${spring.cloud.config.uri}
  cloud:
    config:
      uri: http://localhost:8888       # Config server URL
      label: main                       # Git branch
      enabled: true

docgen:
  templates:
    remote-enabled: true               # Enable remote loading
    cache-enabled: true                # Recommended for production
```

### How It Works

1. **Application startup:** Spring boots and reads `application.yml`
2. **Config server import:** Fetches configuration from config server using `spring.config.import`
3. **Template loading:** When a template is requested:
   - Check if `docgen.templates.remote-enabled` is true
   - If yes, construct URL: `http://config-server:8888/{application}/{profile}/{label}/{namespace}/null/null/templates/{template-id}.yaml.yaml`
   - If found, return cached result (with TTL)
   - If 404 or timeout, throw `TemplateLoadingException`

### Config Server URL Construction

**URL Pattern:**
```
{configServerUri}/{application}/{profile}/{label}/{namespace}/templates/{templateId}
```

**Example:**
```
http://localhost:8888/doc-gen-service/dev/main/tenant-a/templates/enrollment.yaml
```

### Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `spring.cloud.config.uri` | (empty) | Config server base URL |
| `spring.cloud.config.label` | `main` | Git branch/label |
| `spring.profiles.active` | `default` | Active profile for config selection |
| `docgen.templates.remote-enabled` | `true` | Enable remote template loading |

### Example Setup Script

```bash
# Start local config server
cd config-server
mvn spring-boot:run

# Git repo with templates already in /workspaces/demos/config-repo
# Config server configured to point to this git repo in:
# config-server/src/main/resources/application.yml:
#   spring.cloud.config.server.git.uri: file:///workspaces/demos/config-repo

# Application connects and loads templates from config server
# Start application in dev profile:
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Error Handling

| Error | Status | Cause | Resolution |
|-------|--------|-------|-----------|
| `CONFIG_SERVER_ERROR` | 503 | Connection refused | Ensure config server is running, check URI |
| `TEMPLATE_NOT_FOUND` | 404 | Template not in repo | Verify file exists in git repo with correct path |
| Timeout | 503 | Network latency | Increase timeout or optimize network |

### Caching Strategy

When using Config Server with caching enabled:

```yaml
docgen:
  templates:
    cache-enabled: true
  cache:
    document-templates:
      ttl-hours: 24           # Cache for 24 hours
      max-size: 500           # Keep up to 500 templates in cache
    raw-resources:
      ttl-hours: 12           # Resources cached for 12 hours
      max-size: 200
```

---

## External File Storage Service

### Overview

External storage is used for **resources** (PDFs, FreeMarker files, images) referenced in template YAML definitions, NOT for the template definitions themselves.

### Architecture

```
┌─────────────────────────────────────────┐
│  Template Definition (YAML/JSON)        │
│  - Loaded from Classpath or Config      │
└──────────────────┬──────────────────────┘
                   │
        ┌──────────▼────────────┐
        │  Load Resource from   │
        │  Referenced in YAML   │
        │  (templatePath field) │
        └──────────┬────────────┘
                   │
        ┌──────────┴───────────────────┐
        │                              │
        ▼                              ▼
  ┌──────────────┐          ┌──────────────────┐
  │  Classpath   │          │ External Storage │
  │  /Files      │          │  (HTTP Service)  │
  └──────────────┘          └──────────────────┘
```

### Configuration

**File:** `src/main/resources/application-dev.yml`

```yaml
docgen:
  resources:
    # Enable external resource storage
    storage-enabled: true
    
    # Base URL for resource service — point this at an independent
    # external storage service (S3/GCS/http-file-server).
    # Production example (external service):
    base-url: https://storage-service.example.com/api/resources

    # URL pattern for resource paths
    # Available placeholders: {namespace}, {resource}
    path-pattern: "{namespace}/templates/{resource}"

    # Optional: Filesystem path for local resources (dev mode only)
    # NOTE: `storage-basepath` is a developer convenience intended for
    # local testing and is NOT a production storage endpoint.
    storage-basepath: /workspaces/demos/config-repo

    # Optional: Per-namespace explicit URLs (examples)
    common-templates:
      url: https://shared-storage.example.com/files
    tenant-a:
      url: https://tenant-a-storage.example.com/v1/files
    tenant-b:
      url: https://tenant-b-storage.example.com/v1/files
```

### Configuration Options

#### Option 1: Pattern-Based URLs (Recommended)

Uses a pattern and namespace placeholders:

```yaml
docgen:
  resources:
    storage-enabled: true
    base-url: http://storage-service.example.com
    path-pattern: "{namespace}/templates/{resource}"
    # Resolves to:
    # http://storage-service.example.com/tenant-a/templates/forms/header.pdf
```

#### Option 2: Explicit Per-Namespace URLs

Different URLs for different tenants:

```yaml
docgen:
  resources:
    storage-enabled: true
    tenant-a:
      url: http://tenant-a-storage.example.com/v1/files
    tenant-b:
      url: http://tenant-b-storage.example.com/v1/files
    common-templates:
      url: http://shared-storage.example.com/files
    # Resolves to:
    # http://tenant-a-storage.example.com/v1/files/forms/header.pdf
```

#### Option 3: Filesystem Fallback (Dev Only)

Uses filesystem to serve resources locally:

```yaml
docgen:
  resources:
    storage-enabled: true
    storage-basepath: /workspaces/demos/config-repo
    # Falls back to filesystem if not found at (dev-only):
    # /workspaces/demos/config-repo/tenant-a/templates/forms/header.pdf
```

### Template Definition with Resources

**File:** `src/main/resources/tenant-a/templates/enrollment.yaml`

```yaml
namespace: tenant-a

sections:
  - id: header
    type: PDF_RENDERER
    templatePath: forms/header.pdf           # Fetched from external storage service
    # Final URL (example): {base-url}/tenant-a/templates/forms/header.pdf
    
  - id: form
    type: ACROFORM_RENDERER
    templatePath: forms/enrollment-form.pdf
    
  - id: signature
    type: FREEMARKER_RENDERER
    templatePath: freemarker/signature.ftl   # FreeMarker template resource
```

### Resource Storage API

External storage services typically expose a resource endpoint similar to:

```
GET /api/resources/{namespace}/{resource}
```

#### Production examples

Production examples should point to an independent storage host:

```bash
# Fetch PDF form (production/external storage)
curl https://storage-service.example.com/api/resources/tenant-a/forms/enrollment-form.pdf

# Fetch FreeMarker template (production/external storage)
curl https://storage-service.example.com/api/resources/tenant-a/freemarker/signature.ftl

# Fetch signature image (production/external storage)
curl https://storage-service.example.com/api/resources/common-templates/images/company-logo.png
```

#### Dev examples (convenience)

For local development you may run a local file server or use the application's
optional proxy endpoint. These are conveniences for development and testing
only — they are not a substitute for external production storage.

```bash
# Dev-only: fetch via local app proxy (convenience)
curl http://localhost:8080/api/resources/tenant-a/forms/enrollment-form.pdf

# Dev-only: fetch directly from a local file server
curl http://localhost:9090/tenant-a/templates/forms/enrollment-form.pdf
```

Dev note: the application may optionally provide a local proxy endpoint at
`/api/resources/...` to serve files from `storage-basepath` or a local file
server. Using the application's own `/api/resources` endpoint is a development
convenience only and should NOT be used as the production storage backend.

### Directory Structure for External Storage

**Filesystem (for dev mode with storage-basepath):**

```
config-repo/
├── common-templates/
│   └── templates/
│       ├── base-enrollment.yaml        # Template definition (Config Server)
│       ├── forms/
│       │   ├── header.pdf              # Resource
│       │   └── footer.ftl              # Resource
│       └── images/
│           └── logo.png
├── tenant-a/
│   └── templates/
│       ├── enrollment.yaml             # Template definition
│       ├── forms/
│       │   ├── applicant-form.pdf
│       │   └── header.pdf
│       └── freemarker/
│           └── signature.ftl
└── tenant-b/
    └── templates/
        ├── enrollment.yaml
        └── forms/
            ├── header.pdf
            └── footer.ftl
```

### Production Setup with Real Storage

Replace local filesystem with cloud storage:

```yaml
docgen:
  resources:
    storage-enabled: true
    # Example: AWS S3 with pre-signed URLs
    base-url: https://my-bucket.s3.amazonaws.com
    path-pattern: "docgen/{namespace}/templates/{resource}"
    # Results in: https://my-bucket.s3.amazonaws.com/docgen/tenant-a/templates/forms/header.pdf
```

Or use explicit per-namespace URLs for complete control:

```yaml
docgen:
  resources:
    storage-enabled: true
    tenant-a:
      url: https://tenant-a-bucket.s3.amazonaws.com
    tenant-b:
      url: https://tenant-b-bucket.s3.us-west-2.amazonaws.com
```

### Error Handling

| Error | Cause | Resolution |
|-------|-------|-----------|
| `TEMPLATE_NOT_FOUND` | Resource URL returns 404 | Verify resource exists in storage service |
| Connection timeout | Network unreachable | Check storage service URL and firewall |
| 401/403 | Authentication failure | Verify credentials/API key for storage service |

---

## Combining Multiple Sources

### Scenario 1: Templates from Config Server, Resources from Classpath

**Use case:** Zero-downtime template updates without infrastructure

```yaml
spring:
  cloud:
    config:
      uri: http://config-server:8888

docgen:
  templates:
    remote-enabled: true           # Load templates from Config Server
  resources:
    storage-enabled: false         # Use classpath resources (default)
```

### Scenario 2: Templates from Classpath, Resources from External Storage

**Use case:** Scalable resource serving without changing templates

```yaml
docgen:
  templates:
    remote-enabled: false          # Use classpath templates
  resources:
    storage-enabled: true
    base-url: http://s3-proxy.example.com
```

### Scenario 3: Everything Remote

**Use case:** Full centralization, enterprise setup

```yaml
spring:
  cloud:
    config:
      uri: http://config-server:8888

docgen:
  templates:
    remote-enabled: true           # Config Server for templates
    cache-enabled: true            # Cache for performance
  resources:
    storage-enabled: true
    base-url: http://storage-service:9090
```

### Scenario 4: Fallback Chain (Recommended)

**Use case:** Reliable with graceful degradation

```yaml
# Primary: Config Server templates + External storage resources
spring:
  cloud:
    config:
      uri: http://config-server:8888    # Primary source

docgen:
  templates:
    remote-enabled: true                # Try Config Server first
    cache-enabled: true                 # Fall back to cache if online
  resources:
    storage-enabled: true               # External storage preferred
    storage-basepath: /local/fallback    # Fall back to local filesystem
```

---

## Caching and Performance

### Caching Layers

```
┌─────────────────────────────────┐
│  Request for Template            │
└──────────────┬──────────────────┘
               │
        ┌──────▼────────┐
        │  In-Memory    │
        │  Cache        │──── HIT ──→ Return
        │  (Caffeine)   │
        └──────┼────────┘
               │ MISS
        ┌──────▼──────────────┐
        │  Config Server      │
        │  OR                 │
        │  Classpath          │
        └──────┬──────────────┘
               │
        ┌──────▼────────┐
        │  Cache + Ret. │
        └────────────────
```

### Cache Configuration

**File:** `src/main/resources/application.yml`

```yaml
docgen:
  templates:
    cache-enabled: true
  
  cache:
    # Document template definitions (YAML/JSON)
    document-templates:
      ttl-hours: 24          # Time to live
      max-size: 500          # Max templates in cache
    
    # Raw resources (PDF/FTL/images)
    raw-resources:
      ttl-hours: 12          # Resources cached shorter
      max-size: 200
```

### Cache Behavior

| Setting | Effect | Use Case |
|---------|--------|----------|
| `ttl-hours: 24` | Cache for 1 day | Production (low change rate) |
| `ttl-hours: 1` | Cache for 1 hour | Development (frequent changes) |
| `ttl-hours: 0` | Disable cache | Testing (always fresh) |
| `max-size: 500` | Keep 500 items | High-traffic services |
| `max-size: 50` | Keep 50 items | Low-memory environments |

### Cache Invalidation

To force fresh load without restart:

**Method 1: Restart application**
```bash
mvn spring-boot:run
```

**Method 2: Use shorter TTL in development**
```yaml
docgen:
  cache:
    document-templates:
      ttl-hours: 0   # No caching
```

**Method 3: Clear cache via application context (custom endpoint)**

```java
@PostMapping("/admin/cache/clear")
public ResponseEntity<String> clearCache(
        @Autowired CacheManager cacheManager) {
    cacheManager.getCache("documentTemplates").clear();
    cacheManager.getCache("rawResources").clear();
    return ResponseEntity.ok("Cache cleared");
}
```

---

## Quick Start Examples

### Example 1: Development with Local Templates

```yaml
# application.yml
spring:
  profiles:
    active: default

docgen:
  templates:
    remote-enabled: false
    cache-enabled: false
```

**Directory structure:**
```
src/main/resources/
  ├── tenant-a/
  │   └── templates/
  │       ├── enrollment.yaml
  │       └── forms/header.pdf
```

**Load template:**
```java
DocumentTemplate template = templateLoader.loadTemplate("tenant-a", "enrollment.yaml");
```

---

### Example 2: Production with Config Server

```yaml
# application-prod.yml
spring:
  cloud:
    config:
      uri: http://config-server.prod.company.com:8888
      label: release-1.0

docgen:
  templates:
    remote-enabled: true
    cache-enabled: true
  resources:
    storage-enabled: true
    base-url: http://s3-proxy.prod.company.com
```

**Git repo structure:**
```
config-repo (git repository)
  ├── doc-gen-service-prod.yml
  ├── tenant-a/
  │   └── templates/
  │       ├── enrollment.yaml
  │       └── forms/header.pdf
```

**No code changes needed!** Same API call:
```java
DocumentTemplate template = templateLoader.loadTemplate("tenant-a", "enrollment.yaml");
```

---

### Example 3: Multi-Tenant with Per-Namespace URLs

```yaml
docgen:
  resources:
    storage-enabled: true
    tenant-a:
      url: http://storage-us-west.company.com
    tenant-b:
      url: http://storage-eu.company.com
    common-templates:
      url: http://shared-storage.company.com
```

**Requests resolve to:**
- Tenant A resources → `http://storage-us-west.company.com/forms/header.pdf`
- Tenant B resources → `http://storage-eu.company.com/forms/header.pdf`
- Common resources → `http://shared-storage.company.com/forms/header.pdf`

---

### Example 4: Template Preloading (Performance)

```yaml
docgen:
  templates:
    cache-enabled: true
    # Preload frequently used templates at startup
    preload:
      tenant-a:
        - base-enrollment
        - composite-enrollment
      common-templates:
        - base-form
        - default-footer
```

**Benefits:**
- Eliminates cold start latency
- Ensures templates available before first request
- Validates configuration at startup

---

## Troubleshooting Checklist

### Config Server Issues

- [ ] Config server running at configured URI?
  ```bash
  curl http://localhost:8888/doc-gen-service/dev/main/status
  ```
- [ ] Git repository accessible to config server?
  ```bash
  cd config-repo && git log --oneline | head -5
  ```
- [ ] Correct `spring.profiles.active` in application?
  ```bash
  # Check via actuator
  curl http://localhost:8080/actuator/env | grep active
  ```
- [ ] Template file exists in git repo with correct path?

### Resource Storage Issues

- [ ] Storage service running and accessible?
  ```bash
  curl http://storage-service:9090/api/health
  ```
- [ ] Resource path correct in template YAML?
  ```yaml
  templatePath: forms/header.pdf  # ✓ Correct
  # vs
  templatePath: /common-templates/templates/forms/header.pdf  # ✗ Don't include namespace/templates
  ```
- [ ] File exists in storage?
  ```bash
  curl http://storage-service:9090/api/resources/tenant-a/forms/header.pdf
  ```

### Caching Issues

- [ ] Cache TTL too long for development?
  ```yaml
  cache:
    document-templates:
      ttl-hours: 0  # Disable caching in dev
  ```
- [ ] Memory issues due to large cache?
  ```yaml
  cache:
    document-templates:
      max-size: 100  # Reduce from 500
  ```

---

## Summary Table

| Feature | Classpath | Config Server | External Storage |
|---------|-----------|---------------|------------------|
| **Use Case** | Development | Production | Scalable resources |
| **Data Source** | Filesystem (src/main/resources) | Git repo | HTTP service |
| **Configuration** | `remote-enabled: false` | `remote-enabled: true` | `storage-enabled: true` |
| **Performance** | Medium | Good (with cache) | Depends on network |
| **Scalability** | Low | High | High |
| **Multi-tenant** | Yes | Yes | Yes |
| **Hot reload** | Requires restart | Via config server | Not supported |
| **Complexity** | Low | Medium | Medium-High |

---

## Additional Resources

- [Spring Cloud Config Reference](https://spring.io/projects/spring-cloud-config)
- [Caffeine Cache Documentation](https://github.com/ben-manes/caffeine/wiki)
- [ERROR_MESSAGES_GUIDE.md](./ERROR_MESSAGES_GUIDE.md) - Error handling details
- [NAMESPACE_QUICKSTART.md](./NAMESPACE_QUICKSTART.md) - Multi-tenant setup
