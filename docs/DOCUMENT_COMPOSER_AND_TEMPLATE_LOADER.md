# DocumentComposer and TemplateLoader — Implementation notes

This document explains the responsibilities, main flows, and debugging tips for the two central classes used during document generation: the `DocumentComposer` and the `TemplateLoader`.

Files
- Template loader: [src/main/java/com/example/demo/docgen/service/TemplateLoader.java](src/main/java/com/example/demo/docgen/service/TemplateLoader.java)
- Document composer: [src/main/java/com/example/demo/docgen/service/DocumentComposer.java](src/main/java/com/example/demo/docgen/service/DocumentComposer.java)

1) High-level responsibility

- DocumentComposer: coordinates the end-to-end generation of a document. It receives a generation request (template id, namespace/tenant, input model data), asks for template content from `TemplateLoader`, constructs a view model (via `ViewModelFactory`), applies data to the template, invokes renderers (e.g., FreeMarker, PDF renderer, AcroForm renderer), and returns or persists the resulting binary (PDF).

- TemplateLoader: single place for resolving and loading templates and their binary resources. It is responsible for locating a template in the correct namespace, resolving inheritance (base templates), loading and inlining fragments, interpolating placeholders (${...}), and fetching binary assets (images, PDFs) referenced by templates. It supports multiple source backends: classpath (embedded templates), local filesystem (dev mode), and remote config server / HTTP resource storage.

2) Typical request flow (sequence)

1. Request arrives at `DocumentComposer.generate(...)` with `namespace` and `templateId`.
2. `DocumentComposer` calls `TemplateLoader.loadTemplate(namespace, templateId)`.
3. `TemplateLoader` resolves the namespace and physical path using `NamespaceResolver` rules (e.g. `tenant` → `tenant/templates/...`, `common:` prefix → `common-templates/...`).
4. `TemplateLoader` attempts sources in order:
   - classpath resource (fast, used for built-in templates and tests)
   - local filesystem (when `docgen.templates.remote-enabled=false` and `docgen.resources.storage-basepath` configured for dev)
   - remote config server / HTTP-based resource storage (when remote enabled)
5. When a template is found, `TemplateLoader` parses YAML/FreeMarker (or the project's template model), resolves `baseTemplateId` inheritance (recursively), and merges configurations.
6. Included fragments referenced by `includedFragments` are loaded. Cross-namespace fragment references like `common:footer` are mapped to the appropriate `common-templates` location by the namespace resolver.
7. Placeholders in template metadata/strings are interpolated using the incoming request context and resolved properties.
8. Binary resources referenced by the template (e.g., PDF fragments, images) are fetched via `getResourceBytes(...)` which delegates to either filesystem or HTTP client.
9. `TemplateLoader` returns a composed TemplateModel to `DocumentComposer`.
10. `DocumentComposer` builds a view model and runs renderers to produce the final binary.

3) Key methods and responsibilities (concise)

- TemplateLoader.loadTemplate(namespace, templateId)
  - Entry point. Returns the fully-resolved template model.
  - Handles caching (annotated with `@Cacheable` in some builds) — ensure cache key evaluation parameters are present (see debugging tips).

- TemplateLoader.resolveTemplatePath(namespace, id)
  - Normalizes `common:` prefixes and maps to configured folder structure.

- TemplateLoader.loadRawTemplate(path)
  - Reads raw bytes/stream from classpath, filesystem, or HTTP.

- TemplateLoader.interpolateTemplateFields(template, context)
  - Replaces placeholders (${...}) in template metadata and small strings.

- TemplateLoader.mergeTemplates(baseTemplate, childTemplate)
  - Applies inheritance rules: child values override base; arrays/lists are merged when appropriate (fragments + placeholders).

- TemplateLoader.getResourceBytes(path)
  - Fetches binary assets. Uses `docgen.resources.storage-enabled`, `base-url`, and `storage-basepath` configuration.

- DocumentComposer.generate(request)
  - Orchestrates template retrieval, view-model creation, rendering, and response/storage.

- DocumentComposer.applyViewModel(template, viewModel)
  - Binds the model to the template engine (FreeMarker/other) and returns intermediate content for renderers.

- DocumentComposer.renderPdf(intermediate)
  - Calls the PDF renderer (PDFBox or a PDF pipeline) and merges any AcroForm fields.

4) Failure modes & debugging checklist

- ApplicationContext fails to start during tests with ConfigClient fail-fast
  - Cause: Spring Cloud Config client attempts to contact a config server during test bootstrap and `fail-fast` is enabled.
  - Quick fix: disable config client in tests using `spring.cloud.config.enabled=false` or set `spring.cloud.config.uri=` (empty) in the test properties. See `src/test/java/.../ResourceStorageIntegrationTest.java` changes done earlier.

- Missing templates in integration tests
  - Cause: test property `docgen.resources.storage-basepath` pointed to the wrong workspace path.
  - Fix: update tests to point to `/workspaces/test-demos/config-repo` or run a dev HTTP server that serves files under the expected path.

- `@Cacheable` null-key errors
  - Cause: method-level cache keys using parameter names require the code to be compiled with method parameter metadata (`-parameters` javac flag) or explicit `key` expressions that don't rely on parameter names.
  - Fix: enable `-parameters` in `pom.xml` or change cache key to use a computed non-null key (e.g., concatenation of namespace + templateId).

- Fragment/Inheritance resolution surprises (templatePath missing)
  - Symptom: child template expects to inherit `templatePath` from base when base is missing it.
  - Fix: Template merging logic should allow child-supplied `templatePath` when base omits it. Debug by enabling `TemplateLoader` debug log lines which show which path was loaded and merged.

- Binary PDFs or images corrupted / 404
  - Cause: resource fetch uses text-based transfer (config server) rather than binary-safe direct fetch, or the storage backend URL/path mapping is wrong.
  - Debug steps:
    1. Inspect `docgen.resources` config (base-url, path-pattern).
    2. Start a local HTTP file server for development: `python3 -m http.server 9090 --directory /workspaces/test-demos/config-repo` and point `docgen.resources.base-url=http://localhost:9090`.
    3. Use logs from `TemplateLoader` lines like "Fetching raw resource: ..." and "Loaded template from classpath resource: ..." to confirm the source.

5) Logging & quick-turn debug knobs

- Enable DEBUG for these packages to get detailed traces:
  - `com.example.demo.docgen.service.TemplateLoader`
  - `com.example.demo.docgen.service.DocumentComposer`
  - `com.example.demo.docgen.service.ResourceStorageClient`

- Test toggles (useful in `application-dev.properties` or test properties):
  - `docgen.templates.remote-enabled=false` (force classpath/filesystem)
  - `docgen.resources.storage-enabled=true/false`
  - `docgen.resources.storage-basepath=/workspaces/test-demos/config-repo` (dev-only)
  - `spring.cloud.config.enabled=false` (disable remote config during tests)

6) Suggested quick read/walkthrough steps

- Open the loader to follow the path resolution code: [src/main/java/com/example/demo/docgen/service/TemplateLoader.java](src/main/java/com/example/demo/docgen/service/TemplateLoader.java)
- Walk the composer orchestration: [src/main/java/com/example/demo/docgen/service/DocumentComposer.java](src/main/java/com/example/demo/docgen/service/DocumentComposer.java)
- Run a focused test while watching logs (example):

```bash
mvn -Dtest=ResourceStorageFilesystemTest test -DskipITs=false -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG
```

7) Common extension points

- Add fallback resolution for missing `templatePath` in `mergeTemplates`.
- Add a `ResourceStorageClient` mock/provider for unit tests to avoid depending on a running HTTP file server.
- Make cache keys explicit and safe (e.g., `key = "#namespace + ':' + #templateId"`).

---

If you'd like, I can also:
- Annotate the two source files with in-line comments explaining each method (in-place edits), or
- Generate a small UML sequence diagram for the request -> TemplateLoader -> DocumentComposer -> renderer flow.

Which of those follow-ups would you like me to do next?
