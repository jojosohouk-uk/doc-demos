package com.example.demo.docgen.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ExcelTemplateServiceTest {

    @Test
    void fillsNamedRangesAndTables() throws Exception {
        // Create a simple workbook with named ranges
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Sheet1");

        // A1 will be customerName (FreeMarker placeholder)
        Row r0 = sheet.createRow(0);
        r0.createCell(0).setCellValue("${customerName}");

        // Header for items at row 3 (index 2)
        Row header = sheet.createRow(2);
        header.createCell(0).setCellValue("Item");
        header.createCell(1).setCellValue("Qty");
        header.createCell(2).setCellValue("Price");

        // Create named ranges
        Name nameCustomer = wb.createName();
        nameCustomer.setNameName("customerName");
        nameCustomer.setRefersToFormula("Sheet1!$A$1");

        Name nameItems = wb.createName();
        nameItems.setNameName("items");
        nameItems.setRefersToFormula("Sheet1!$A$4:$C$6");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wb.write(baos);
        wb.close();

        byte[] templateBytes = baos.toByteArray();

        // Mock TemplateLoader to return this workbook bytes
        TemplateLoader loader = Mockito.mock(TemplateLoader.class);
        Mockito.when(loader.getNamespaceResourceBytes("common-templates", "templates/example.xlsx"))
               .thenReturn(templateBytes);

        ExcelTemplateService svc = new ExcelTemplateService(loader);

        Map<String,Object> data = new HashMap<>();
        data.put("customerName", "Alice");

        List<List<Object>> items = new ArrayList<>();
        items.add(Arrays.asList("Apple", 2, 1.5));
        items.add(Arrays.asList("Banana", 3, 0.75));
        data.put("items", items);

        byte[] out = svc.fillTemplate("common-templates", "templates/example.xlsx", data);

        try (Workbook outWb = WorkbookFactory.create(new java.io.ByteArrayInputStream(out))) {
            Sheet outSheet = outWb.getSheet("Sheet1");
            assertEquals("Alice", outSheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Apple", outSheet.getRow(3).getCell(0).getStringCellValue());
            assertEquals(2.0, outSheet.getRow(3).getCell(1).getNumericCellValue());
            assertEquals(0.75, outSheet.getRow(4).getCell(2).getNumericCellValue(), 1e-6);
        }
    }
}
