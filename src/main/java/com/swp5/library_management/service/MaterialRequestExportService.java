package com.swp5.library_management.service;

import com.swp5.library_management.entity.MaterialRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MaterialRequestExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] exportToExcel(List<MaterialRequest> requests) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Material Requests");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Data cell style with borders
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setWrapText(true);

            // Headers
            String[] headers = {
                "No.", "Request ID", "Patron ID", "Patron Name", "Patron Role", "Title", "Author",
                "ISBN", "Publisher", "Language", "Priority", "Reason",
                "Status", "Feedback", "Reviewed By", "Reviewed At", "Created At"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 1;
            for (MaterialRequest req : requests) {
                Row row = sheet.createRow(rowIdx);
                int col = 0;

                createCell(row, col++, rowIdx, dataStyle);  // No.
                createCell(row, col++, "REQ" + String.format("%03d", req.getRequestId()), dataStyle);
                createCell(row, col++, req.getPatron() != null ? req.getPatron().getUserId() : "", dataStyle);
                createCell(row, col++, req.getPatron() != null ? req.getPatron().getFullName() : "", dataStyle);
                createCell(row, col++, req.getPatron() != null ? req.getPatron().getRoleNameDisplay() : "", dataStyle);
                createCell(row, col++, req.getTitle() != null ? req.getTitle() : "", dataStyle);
                createCell(row, col++, req.getAuthor() != null ? req.getAuthor() : "", dataStyle);
                createCell(row, col++, req.getIsbn() != null ? req.getIsbn() : "", dataStyle);
                createCell(row, col++, req.getPublisher() != null ? req.getPublisher() : "", dataStyle);
                createCell(row, col++, req.getLanguage() != null ? req.getLanguage() : "", dataStyle);
                createCell(row, col++, req.getPriority() != null ? req.getPriority() : "", dataStyle);
                createCell(row, col++, req.getReason() != null ? req.getReason() : "", dataStyle);
                createCell(row, col++, req.getStatus() != null ? req.getStatus() : "", dataStyle);
                createCell(row, col++, req.getFeedback() != null ? req.getFeedback() : "", dataStyle);
                createCell(row, col++, req.getReviewedBy() != null ? req.getReviewedBy() : "", dataStyle);
                createCell(row, col++, req.getReviewedAt() != null ? req.getReviewedAt().format(DATE_FMT) : "", dataStyle);
                createCell(row, col++, req.getCreatedAt() != null ? req.getCreatedAt().format(DATE_FMT) : "", dataStyle);

                rowIdx++;
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Cap max width at 50 characters (50 * 256 units)
                if (sheet.getColumnWidth(i) > 50 * 256) {
                    sheet.setColumnWidth(i, 50 * 256);
                }
            }

            // Freeze header row
            sheet.createFreezePane(0, 1);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value != null ? value.toString() : "");
        }
        cell.setCellStyle(style);
    }
}
