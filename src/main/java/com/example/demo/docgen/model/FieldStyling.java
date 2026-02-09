package com.example.demo.docgen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Defines styling for AcroForm PDF fields
 * All color values should be RGB as integers (e.g., 0xFF0000 for red)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldStyling {
    
    /**
     * Font size in points (e.g., 12 for 12pt)
     */
    private Float fontSize;
    
    /**
     * Text color as RGB hex value (e.g., 0xFF0000 for red, 0x000000 for black)
     * Default: 0x000000 (black)
     */
    private Integer textColor;
    
    /**
     * Background color as RGB hex value (e.g., 0xFFFFFF for white)
     * Default: 0xFFFFFF (white)
     */
    private Integer backgroundColor;
    
    /**
     * Border color as RGB hex value (e.g., 0x666666 for gray)
     * Default: 0x000000 (black)
     */
    private Integer borderColor;
    
    /**
     * Border width in points (e.g., 1 for 1pt border)
     * Default: 1
     */
    private Float borderWidth;
    
    /**
     * Text alignment: LEFT, CENTER, RIGHT
     * Default: LEFT
     */
    private TextAlignment alignment;
    
    /**
     * Font name (e.g., "Helvetica", "Times-Roman", "Courier")
     * Note: Limited to standard PDF fonts
     * Default: "Helvetica"
     */
    private String fontName;
    
    /**
     * Bold text flag
     * Default: false
     */
    @Builder.Default
    private Boolean bold = false;
    
    /**
     * Italic text flag
     * Default: false
     */
    @Builder.Default
    private Boolean italic = false;
    
    /**
     * Read-only field flag
     * Makes the field non-editable in the PDF
     * Default: false
     */
    @Builder.Default
    private Boolean readOnly = false;
    
    /**
     * Text is hidden (password field)
     * Default: false
     */
    @Builder.Default
    private Boolean hidden = false;
    
    public enum TextAlignment {
        LEFT(0),
        CENTER(1),
        RIGHT(2);
        
        public final int code;
        
        TextAlignment(int code) {
            this.code = code;
        }
    }
}
