package com.swp5.library_management.admin.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.swp5.library_management.admin.dto.ReportSummaryDTO;
import com.swp5.library_management.admin.dto.TransactionRecordDTO;
import com.swp5.library_management.repository.BorrowTicketDetailRepository;
import com.swp5.library_management.repository.FineInvoiceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final BorrowTicketDetailRepository borrowDetailRepo;
    private final FineInvoiceRepository fineInvoiceRepo;

    // Lấy số liệu thống kê để hiển thị lên Dashboard báo cáo
    public ReportSummaryDTO getCampusReportSummary(Integer campusId, LocalDateTime startDate, LocalDateTime endDate) {
        return ReportSummaryDTO.builder()
                .totalBorrowed(borrowDetailRepo.countBorrowedInPeriod(campusId, startDate, endDate))
                .totalReturned(borrowDetailRepo.countReturnedInPeriod(campusId, startDate, endDate))
                .totalOverdue(borrowDetailRepo.countCurrentOverdue(campusId))
                .totalFinesCollected(fineInvoiceRepo.sumFinesCollectedInPeriod(campusId, startDate, endDate))
                .totalFinesPending(fineInvoiceRepo.sumFinesPending(campusId))
                .build();
    }

    // Tạo file Excel bằng Apache POI
    public ByteArrayInputStream generateExcelReport(Integer campusId, LocalDateTime startDate, LocalDateTime endDate) throws IOException {
        ReportSummaryDTO data = getCampusReportSummary(campusId, startDate, endDate);
        List<TransactionRecordDTO> transactions = borrowDetailRepo.getTransactionDetails(campusId, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Format cho Header (in đậm)
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            // Format có viền (cho bảng chi tiết)
            CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);
            CellStyle headerBorderStyle = workbook.createCellStyle();
            headerBorderStyle.cloneStyleFrom(borderStyle);
            headerBorderStyle.setFont(headerFont);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // ==========================================
            // SHEET 1: TỔNG QUAN (Như cũ)
            // ==========================================
            Sheet summarySheet = workbook.createSheet("Tổng quan");
            Row titleRow = summarySheet.createRow(0);
            titleRow.createCell(0).setCellValue("BÁO CÁO HOẠT ĐỘNG THƯ VIỆN CƠ SỞ " + campusId);
            titleRow.getCell(0).setCellStyle(headerStyle);

            summarySheet.createRow(1).createCell(0).setCellValue("Từ ngày: " + startDate.toLocalDate() + " - Đến ngày: " + endDate.toLocalDate());

            Row dataHeaderRow = summarySheet.createRow(3);
            dataHeaderRow.createCell(0).setCellValue("Chỉ số");
            dataHeaderRow.createCell(1).setCellValue("Giá trị");
            dataHeaderRow.getCell(0).setCellStyle(headerStyle);
            dataHeaderRow.getCell(1).setCellStyle(headerStyle);

            int rowIdx = 4;
            createDataRow(summarySheet.createRow(rowIdx++), "Tổng lượt sách mượn", String.valueOf(data.getTotalBorrowed()));
            createDataRow(summarySheet.createRow(rowIdx++), "Tổng lượt sách trả", String.valueOf(data.getTotalReturned()));
            createDataRow(summarySheet.createRow(rowIdx++), "Sách đang quá hạn", String.valueOf(data.getTotalOverdue()));
            createDataRow(summarySheet.createRow(rowIdx++), "Tiền phạt đã thu (VNĐ)", data.getTotalFinesCollected().toString());
            createDataRow(summarySheet.createRow(rowIdx++), "Tiền phạt chờ thu (VNĐ)", data.getTotalFinesPending().toString());

            summarySheet.autoSizeColumn(0);
            summarySheet.autoSizeColumn(1);

            // ==========================================
            // SHEET 2: CHI TIẾT GIAO DỊCH (Mới thêm)
            // ==========================================
            Sheet detailSheet = workbook.createSheet("Chi tiết giao dịch");
            
            // Header của bảng chi tiết
            Row detailHeader = detailSheet.createRow(0);
            String[] columns = {"STT", "Ngày mượn", "Ngày trả (thực tế)", "Người mượn", "Thủ thư", "Mã bản sao", "Tên sách", "Trạng thái"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = detailHeader.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerBorderStyle);
            }

            // Đổ data vào bảng
            int detailRowIdx = 1;
            for (TransactionRecordDTO t : transactions) {
                Row row = detailSheet.createRow(detailRowIdx++);
                
                // Cột 0: STT
                Cell c0 = row.createCell(0); c0.setCellValue(detailRowIdx - 1); c0.setCellStyle(borderStyle);
                // Cột 1: Ngày mượn
                Cell c1 = row.createCell(1); c1.setCellValue(t.getBorrowDate().format(dtf)); c1.setCellStyle(borderStyle);
                // Cột 2: Ngày trả
                Cell c2 = row.createCell(2); c2.setCellValue(t.getReturnDate() != null ? t.getReturnDate().format(dtf) : "Chưa trả"); c2.setCellStyle(borderStyle);
                // Cột 3: Người mượn
                Cell c3 = row.createCell(3); c3.setCellValue(t.getPatronName()); c3.setCellStyle(borderStyle);
                // Cột 4: Thủ thư duyệt
                Cell c4 = row.createCell(4); c4.setCellValue(t.getLibrarianName()); c4.setCellStyle(borderStyle);
                // Cột 5: Mã bản sao
                Cell c5 = row.createCell(5); c5.setCellValue(t.getCopyId()); c5.setCellStyle(borderStyle);
                // Cột 6: Tên sách
                Cell c6 = row.createCell(6); c6.setCellValue(t.getBookTitle()); c6.setCellStyle(borderStyle);
                // Cột 7: Trạng thái (Borrowing, Returned, Overdue...)
                Cell c7 = row.createCell(7); 
                String status = t.getStatus().equals("Borrowing") ? "Đang mượn" : 
                               (t.getStatus().equals("Returned") ? "Đã trả" : 
                               (t.getStatus().equals("Overdue") ? "Quá hạn" : t.getStatus()));
                c7.setCellValue(status); 
                c7.setCellStyle(borderStyle);
            }

            // Tự động căn chỉnh độ rộng cột cho đẹp
            for (int i = 0; i < columns.length; i++) {
                detailSheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private void createDataRow(Row row, String metric, String value) {
        row.createCell(0).setCellValue(metric);
        row.createCell(1).setCellValue(value);
    }
}