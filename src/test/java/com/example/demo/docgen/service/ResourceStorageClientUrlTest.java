package com.example.demo.docgen.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {ResourceStorageClient.class},
    properties = {
        "docgen.resources.storage-enabled=true",
        "docgen.resources.base-url=http://base.example.com/api/resources",
        "docgen.resources.tenant-a.url=http://tenant-a.example.com/files"
    })
@ActiveProfiles("test")
public class ResourceStorageClientUrlTest {

    @Autowired
    private ResourceStorageClient client;

    @Test
    void explicitTenantUrlIsPreferred() {
        String url = client.getResourceUrl("tenant-a", "forms/applicant-form.pdf");
        assertThat(url).isEqualTo("http://tenant-a.example.com/files/forms/applicant-form.pdf");
    }

    @Test
    void fallsBackToPattern() {
        String url = client.getResourceUrl("tenant-b", "forms/applicant-form.pdf");
        assertThat(url).isEqualTo("http://base.example.com/api/resources/tenant-b/templates/forms/applicant-form.pdf");
    }
}
