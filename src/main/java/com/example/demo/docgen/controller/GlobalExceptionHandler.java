package com.example.demo.docgen.controller;

import com.example.demo.docgen.exception.TemplateLoadingException;
import com.example.demo.docgen.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler for REST API endpoints.
 * Converts exceptions into user-friendly JSON error responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Handle TemplateLoadingException - template resource not found or loading failed
     */
    @ExceptionHandler(TemplateLoadingException.class)
    public ResponseEntity<ErrorResponse> handleTemplateLoadingException(
            TemplateLoadingException ex,
            WebRequest request) {
        
        log.warn("Template loading error [{}]: {}", ex.getCode(), ex.getDescription());
        
        ErrorResponse error = new ErrorResponse(
            ex.getCode(),
            ex.getDescription()
        );
        
        HttpStatus status = determineHttpStatus(ex.getCode());
        return ResponseEntity
            .status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(error);
    }
    
    /**
     * Handle generic exceptions - internal server errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            WebRequest request) {
        
        log.error("Unexpected error occurred", ex);
        
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred while processing your request"
        );
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(error);
    }
    
    /**
     * Determine HTTP status code based on error code
     */
    private HttpStatus determineHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "TEMPLATE_NOT_FOUND", "RESOURCE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "UNSUPPORTED_TEMPLATE_FORMAT" -> HttpStatus.BAD_REQUEST;
            case "INVALID_REQUEST" -> HttpStatus.BAD_REQUEST;
            case "TEMPLATE_PARSE_ERROR" -> HttpStatus.BAD_REQUEST;
            case "RESOURCE_READ_ERROR" -> HttpStatus.INTERNAL_SERVER_ERROR;
            case "CONFIG_SERVER_ERROR" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
