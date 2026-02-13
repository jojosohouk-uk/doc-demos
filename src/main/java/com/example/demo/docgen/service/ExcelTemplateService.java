package com.example.demo.docgen.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelTemplateService {
    private final TemplateLoader templateLoader;

    /**
     * Fill an Excel template identified by namespace + templatePath with provided data.
     *
     * Behavior (simple, pragmatic):
     * - For each top-level key in `data`:
     *   - If the workbook contains a named range with that name:
     *     - If the value is a List: treat it as a table and populate starting at the named area's first cell.
     *     - Otherwise: set the single cell referenced by the named range to the scalar value.
     * - Writes the modified workbook to bytes and returns them.
     */
    public byte[] fillTemplate(String namespace, String templatePath, Map<String, Object> data) {
        String ns = namespace != null ? namespace : "common-templates";
        byte[] templateBytes = templateLoader.getNamespaceResourceBytes(ns, templatePath);
        
        log.info("✅ fillTemplate - Namespace: {}, Template: {}, Data keys: {}", 
            ns, templatePath, data != null ? data.keySet() : "null");

        try (ByteArrayInputStream in = new ByteArrayInputStream(templateBytes);
             Workbook workbook = WorkbookFactory.create(in)) {

            // SIMPLIFIED: Populate named ranges directly (no FreeMarker processing)
            if (data != null && !data.isEmpty()) {
                log.info("📋 Populating named ranges from data. Data items: {}", data.size());
                
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    
                    log.info("  Processing key: '{}' with value type: {}", key, 
                        value != null ? value.getClass().getSimpleName() : "null");

                    Name named = workbook.getName(key);
                    if (named != null) {
                        String refers = named.getRefersToFormula();
                        log.info("    ✓ Found named range '{}' -> {}", key, refers);
                        
                        if (refers == null) {
                            log.warn("    ✗ Named range '{}' has null reference", key);
                            continue;
                        }

                        if (value instanceof List) {
                            log.info("    📊 Populating table for '{}' with {} rows", key, ((List<?>) value).size());
                            populateTable(workbook, refers, (List<?>) value);
                        } else {
                            log.info("    📝 Setting single cell for '{}' to value: {}", key, value);
                            setCellValueByNamedRange(workbook, refers, value);
                        }
                    } else {
                        log.warn("  ✗ No named range found for key: '{}'", key);
                    }
                }
            } else {
                log.warn("⚠️  No data provided for filling");
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to fill excel template: " + templatePath, e);
        }
    }

    private void setCellValueByNamedRange(Workbook workbook, String refersTo, Object value) {
        // refersTo format: Sheet1!$A$1 or 'My Sheet'!$B$2
        String[] parts = refersTo.split("!", 2);
        if (parts.length != 2) return;

        String sheetName = parts[0].replace("'", "");
        String cellRef = parts[1].replace("$", "");

        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            // try without quotes
            sheet = workbook.getSheet(parts[0]);
            if (sheet == null) return;
        }

        CellReference cref = new CellReference(cellRef);
        Row row = sheet.getRow(cref.getRow());
        if (row == null) row = sheet.createRow(cref.getRow());
        Cell cell = row.getCell(cref.getCol());
        if (cell == null) cell = row.createCell(cref.getCol());

        setCellValue(cell, value);
    }

    private void populateTable(Workbook workbook, String refersTo, List<?> data) {
        // refersTo format: Sheet1!$A$2:$C$10 or similar
        String[] parts = refersTo.split("!", 2);
        if (parts.length != 2) return;

        String sheetName = parts[0].replace("'", "");
        String range = parts[1];

        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            sheet = workbook.getSheet(parts[0]);
            if (sheet == null) return;
        }

        AreaReference area = new AreaReference(range, SpreadsheetVersion.EXCEL2007);
        CellReference firstCell = area.getFirstCell();
        int startRow = firstCell.getRow();
        int startCol = firstCell.getCol();

        for (int i = 0; i < data.size(); i++) {
            Object rowObj = data.get(i);
            Row row = sheet.getRow(startRow + i);
            if (row == null) row = sheet.createRow(startRow + i);

            if (rowObj instanceof List) {
                List<?> cols = (List<?>) rowObj;
                for (int c = 0; c < cols.size(); c++) {
                    Cell cell = row.getCell(startCol + c);
                    if (cell == null) cell = row.createCell(startCol + c);
                    setCellValue(cell, cols.get(c));
                }
            } else if (rowObj instanceof Map) {
                // For Map, write values in iteration order into columns
                int c = 0;
                for (Object v : ((Map<?, ?>) rowObj).values()) {
                    Cell cell = row.getCell(startCol + c);
                    if (cell == null) cell = row.createCell(startCol + c);
                    setCellValue(cell, v);
                    c++;
                }
            } else {
                // Scalar - write to first column
                Cell cell = row.getCell(startCol);
                if (cell == null) cell = row.createCell(startCol);
                setCellValue(cell, rowObj);
            }
        }
    }

    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }

        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof java.util.Date) {
            CellStyle style = cell.getSheet().getWorkbook().createCellStyle();
            CreationHelper createHelper = cell.getSheet().getWorkbook().getCreationHelper();
            style.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));
            cell.setCellStyle(style);
            cell.setCellValue((java.util.Date) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }
}