package com.example.demo.docgen.service;

import freemarker.cache.TemplateLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Custom FreeMarker TemplateLoader that uses our centralized TemplateLoader (with caching and remote support)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomFreeMarkerTemplateLoader implements TemplateLoader {

    private final com.example.demo.docgen.service.TemplateLoader centralizedLoader;

    @Override
    public Object findTemplateSource(String name) throws IOException {
        // We use the centralized loader to get the bytes (which handles caching and remote fetching)
        log.info("CustomFreeMarkerTemplateLoader.findTemplateSource called with name: '{}'", name);
        try {
            return centralizedLoader.getResourceBytes(name);
        } catch (com.example.demo.docgen.exception.TemplateLoadingException e) {
            log.warn("Failed to load FreeMarker template: {} - {}", name, e.getDescription());
            return null;
        }
    }

    @Override
    public long getLastModified(Object templateSource) {
        // For simplicity in this demo, we return -1 to indicate we don't track modification times
        // FreeMarker will rely on our Spring Cache for freshness
        return -1;
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        byte[] bytes = (byte[]) templateSource;
        return new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8);
    }

    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
        // Nothing to close for byte arrays
    }
}
