package com.example.demo.docgen.exception;

/**
 * Exception thrown when a resource (PDF, FTL, XLS, etc.) cannot be loaded.
 * Contains error code and description for user-friendly error responses.
 */
public class ResourceLoadingException extends RuntimeException {
    private final String code;
    private final String description;

    public ResourceLoadingException(String code, String description) {
        super(code + ": " + description);
        this.code = code;
        this.description = description;
    }

    public ResourceLoadingException(String code, String description, Throwable cause) {
        super(code + ": " + description, cause);
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
