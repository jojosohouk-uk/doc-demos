package com.example.demo.docgen.service;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;

/**
 * Optional converter interface to transform filled Excel workbook bytes into a PDF document.
 * Implementations (e.g., using iText/POI or external tools) can be provided as beans.
 */
public interface ExcelToPdfConverter {
    /**
     * Convert an Excel workbook (xls/xlsx) represented by bytes into a PDDocument.
     *
     * @param workbookBytes Excel workbook bytes
     * @return PDDocument containing rendered PDF pages
     */
    PDDocument convert(byte[] workbookBytes) throws IOException;
}
