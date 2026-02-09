package com.example.demo.docgen.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "docgen.resources.storage-enabled=true",
            "docgen.resources.storage-basepath=/workspaces/test-demos/config-repo",
        "docgen.templates.remote-enabled=false",
            "spring.cloud.config.uri=",
            "spring.cloud.config.enabled=false"
    })
@ActiveProfiles("dev")
public class ResourceStorageFilesystemTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void servesPdfFromFilesystem() {
        String url = "http://localhost:" + port + "/api/resources/common-templates/templates/forms/applicant-form.pdf";
        ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        HttpHeaders headers = resp.getHeaders();
        assertThat(headers.getContentType().toString()).contains("pdf");
        byte[] body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.length).isGreaterThan(10);
        // PDF files start with %PDF
        String beginning = new String(body, 0, Math.min(4, body.length));
        assertThat(beginning).contains("%PDF");
    }
}
