package com.example.demo.docgen.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Error response model for REST API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String description;
}
