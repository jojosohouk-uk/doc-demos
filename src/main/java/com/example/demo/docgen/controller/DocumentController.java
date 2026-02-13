package com.example.demo.docgen.controller;

import com.example.demo.docgen.model.DocumentGenerationRequest;
import com.example.demo.docgen.service.DocumentComposer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.SpreadsheetVersion;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * REST API for document generation
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentComposer documentComposer;
    private com.example.demo.docgen.service.ExcelTemplateService excelTemplateService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setExcelTemplateService(com.example.demo.docgen.service.ExcelTemplateService excelTemplateService) {
        this.excelTemplateService = excelTemplateService;
    }
    
    /**
     * Generate a PDF document from a template and data
     *
     * POST /api/documents/generate
     * {
     *   "namespace": "tenant-a",
     *   "templateId": "enrollment-form.yaml",
     *   "data": {
     *     "applicant": { "firstName": "John", "lastName": "Doe" },
     *     ...
     *   }
     * }
     *
     * The namespace identifies which tenant's template directory to use (e.g., "tenant-a", "tenant-b").
     * If omitted, defaults to "common-templates".
     * The templateId is relative to {namespace}/templates/ folder.
     *
     * @param request Generation request with namespace, templateId, and data
     * @return PDF document as byte array
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateDocument(@RequestBody DocumentGenerationRequest request) {
        log.info("Received document generation request for template: {} from namespace: {}", request.getTemplateId(), request.getNamespace());
        try {
            byte[] pdf = documentComposer.generateDocument(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "document.pdf");
            headers.setContentLength(pdf.length);

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (com.example.demo.docgen.exception.TemplateLoadingException tle) {
            String code = tle.getCode();
            String description = tle.getDescription();

            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("code", code);
            body.put("description", description);

            if ("TEMPLATE_NOT_FOUND".equals(code)) {
                return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
            } else if ("UNRESOLVED_PLACEHOLDER".equals(code)) {
                return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
            }

            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Fill an Excel template and return the filled workbook.
     * Expects `templateId` to point to an Excel file under namespace templates.
     */
    @PostMapping("/fill-excel")
    public ResponseEntity<?> fillExcel(@RequestBody DocumentGenerationRequest request) {
        log.info("Received fill-excel request for template: {} from namespace: {}", request.getTemplateId(), request.getNamespace());
        try {
            byte[] xlsx = excelTemplateService.fillTemplate(request.getNamespace(), request.getTemplateId(), request.getData());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "filled.xlsx");
            headers.setContentLength(xlsx.length);

            return new ResponseEntity<>(xlsx, headers, HttpStatus.OK);
        } catch (com.example.demo.docgen.exception.TemplateLoadingException tle) {
            String code = tle.getCode();
            String description = tle.getDescription();

            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("code", code);
            body.put("description", description);

            if ("TEMPLATE_NOT_FOUND".equals(code)) {
                return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
            } else if ("UNRESOLVED_PLACEHOLDER".equals(code)) {
                return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
            }

            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Verify filled Excel workbook and return JSON summary of requested keys / named ranges.
     * If `options.verifyKeys` is provided (list of names) it will be used; otherwise keys from `data` are used.
     */
    @PostMapping("/verify-excel")
    public ResponseEntity<?> verifyExcel(@RequestBody DocumentGenerationRequest request) {
        log.info("Received verify-excel request for template: {} from namespace: {}", request.getTemplateId(), request.getNamespace());

        try {
            byte[] xlsx = excelTemplateService.fillTemplate(request.getNamespace(), request.getTemplateId(), request.getData());

            // Determine keys to verify
            Collection<String> keysToCheck;
            if (request.getOptions() != null && request.getOptions().containsKey("verifyKeys")) {
                Object raw = request.getOptions().get("verifyKeys");
                if (raw instanceof Collection) {
                    keysToCheck = (Collection<String>) raw;
                } else if (raw instanceof String) {
                    keysToCheck = List.of((String) raw);
                } else {
                    keysToCheck = request.getData() != null ? request.getData().keySet() : List.of();
                }
            } else {
                keysToCheck = request.getData() != null ? request.getData().keySet() : List.of();
            }

            Map<String, Object> result = new java.util.HashMap<>();

            try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
                for (String key : keysToCheck) {
                    Name name = wb.getName(key);
                    if (name != null) {
                        String refers = name.getRefersToFormula();
                        if (refers != null && refers.contains(":")) {
                            // area -> table rows
                            AreaReference area = new AreaReference(refers, SpreadsheetVersion.EXCEL2007);
                            CellReference start = area.getFirstCell();
                            CellReference end = area.getLastCell();

                            List<List<Object>> rows = new ArrayList<>();
                            for (int r = start.getRow(); r <= end.getRow(); r++) {
                                List<Object> cols = new ArrayList<>();
                                for (int c = start.getCol(); c <= end.getCol(); c++) {
                                    org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheet(start.getSheetName());
                                    if (sheet == null) continue;
                                    org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                                    Object cellVal = null;
                                    if (row != null) {
                                        org.apache.poi.ss.usermodel.Cell cell = row.getCell(c);
                                        if (cell != null) {
                                            switch (cell.getCellType()) {
                                                case STRING: cellVal = cell.getStringCellValue(); break;
                                                case NUMERIC: cellVal = cell.getNumericCellValue(); break;
                                                case BOOLEAN: cellVal = cell.getBooleanCellValue(); break;
                                                default: cellVal = null;
                                            }
                                        }
                                    }
                                    cols.add(cellVal);
                                }
                                rows.add(cols);
                            }
                            result.put(key, rows);
                        } else {
                            // single cell
                            String refersTo = name.getRefersToFormula();
                            CellReference cref = new CellReference(refersTo.replace("$", ""));
                            org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheet(cref.getSheetName());
                            Object cellVal = null;
                            if (sheet != null) {
                                org.apache.poi.ss.usermodel.Row row = sheet.getRow(cref.getRow());
                                if (row != null) {
                                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(cref.getCol());
                                    if (cell != null) {
                                        switch (cell.getCellType()) {
                                            case STRING: cellVal = cell.getStringCellValue(); break;
                                            case NUMERIC: cellVal = cell.getNumericCellValue(); break;
                                            case BOOLEAN: cellVal = cell.getBooleanCellValue(); break;
                                            default: cellVal = null;
                                        }
                                    }
                                }
                            }
                            result.put(key, cellVal);
                        }
                    } else {
                        // no named range - try to read a single cell by A1 if key looks like a cell ref
                        Object cellVal = null;
                        try {
                            CellReference cref = new CellReference(key);
                            org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheetAt(0);
                            org.apache.poi.ss.usermodel.Row row = sheet.getRow(cref.getRow());
                            if (row != null) {
                                org.apache.poi.ss.usermodel.Cell cell = row.getCell(cref.getCol());
                                if (cell != null) {
                                    switch (cell.getCellType()) {
                                        case STRING: cellVal = cell.getStringCellValue(); break;
                                        case NUMERIC: cellVal = cell.getNumericCellValue(); break;
                                        case BOOLEAN: cellVal = cell.getBooleanCellValue(); break;
                                        default: cellVal = null;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            cellVal = null;
                        }
                        result.put(key, cellVal);
                    }
                }
            }

            return ResponseEntity.ok(result);
        } catch (com.example.demo.docgen.exception.TemplateLoadingException tle) {
            String code = tle.getCode();
            String description = tle.getDescription();

            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("code", code);
            body.put("description", description);

            if ("TEMPLATE_NOT_FOUND".equals(code)) {
                return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
            } else if ("UNRESOLVED_PLACEHOLDER".equals(code)) {
                return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
            }

            return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Failed to verify excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Document generation service is running");
    }
}
