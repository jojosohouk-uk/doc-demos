package com.example.demo.docgen.renderer;

import com.example.demo.docgen.core.RenderContext;
import com.example.demo.docgen.model.PageSection;
import com.example.demo.docgen.model.SectionType;
import com.example.demo.docgen.service.ExcelTemplateService;
import com.example.demo.docgen.service.ExcelToPdfConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Renderer for EXCEL sections. Fills Excel templates and converts to PDF when a converter
 * is available. If no converter is present, returns a simple placeholder PDF page so the
 * DocumentComposer pipeline can continue.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExcelSectionRenderer implements SectionRenderer {

    private final ExcelTemplateService excelTemplateService;
    private ExcelToPdfConverter excelToPdfConverter;

    @Autowired(required = false)
    public void setExcelToPdfConverter(ExcelToPdfConverter converter) {
        this.excelToPdfConverter = converter;
    }

    @Override
    public PDDocument render(PageSection section, RenderContext context) {
        String templatePath = section.getTemplatePath();
        String namespace = context != null && context.getNamespace() != null ? context.getNamespace() : "common-templates";

        log.info("Filling Excel template '{}' for section: {}", templatePath, section.getSectionId());

        byte[] filledBytes = excelTemplateService.fillTemplate(namespace, templatePath, context != null ? context.getData() : null);

        if (excelToPdfConverter != null) {
            try {
                log.info("Converting filled Excel to PDF using registered converter");
                return excelToPdfConverter.convert(filledBytes);
            } catch (IOException e) {
                log.warn("Excel->PDF conversion failed, falling back to placeholder PDF: {}", e.getMessage());
            }
        }

        // No converter available or conversion failed - return a placeholder PDF page
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage();
        doc.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
            cs.newLineAtOffset(50, 700);
            cs.showText("Excel template filled: " + (templatePath != null ? templatePath : "(unknown)"));
            cs.endText();
        } catch (IOException e) {
            log.warn("Failed to write placeholder text to PDF: {}", e.getMessage());
        }

        return doc;
    }

    @Override
    public boolean supports(SectionType type) {
        return type == SectionType.EXCEL;
    }
}
