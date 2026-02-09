package com.example.demo.docgen.model;

import lombok.Data;

/**
 * Helper class for PDF border styling
 * Represents the BS (Border Style) dictionary in PDF
 */
@Data
public class BorderStyle {
    private Float width;
    
    public BorderStyle(Float width) {
        this.width = width != null ? width : 1.0f;
    }
}
