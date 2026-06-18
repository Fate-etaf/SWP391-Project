package com.swp5.library_management.service;

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

import com.swp5.library_management.dto.ReportSummaryDTO;
import com.swp5.library_management.dto.TransactionRecordDTO;
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
            
            // Cấu hình style in đậm cho Header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Cấu hình border viền mảnh cho bảng chi tiết
            CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);

            CellStyle headerBorderStyle = workbook.createCellStyle();
            headerBorderStyle.cloneStyleFrom(borderStyle);
            headerBorderStyle.setFont(headerFont);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // ==================================================================
            // SHEET 1: TỔNG QUAN
            // ==================================================================
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

            // ==================================================================
            // SHEET 2: CHI TIẾT GIAO DỊCH (Đã thêm 4 cột mới)
            // ==================================================================
            Sheet detailSheet = workbook.createSheet("Chi tiết giao dịch");
            
            Row detailHeader = detailSheet.createRow(0);
            String[] columns = {
                "STT", "Mã Bạn đọc", "Tên Bạn đọc", "Ngày mượn", 
                "Hạn trả quy định", "Ngày trả thực tế", "Thủ thư xử lý", 
                "Mã bản sao", "Tên sách", "Số lần gia hạn", "Tiền phạt phát sinh", "Trạng thái"
            };
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = detailHeader.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerBorderStyle);
            }

            int detailRowIdx = 1;
            for (TransactionRecordDTO t : transactions) {
                Row row = detailSheet.createRow(detailRowIdx++);
                
                Cell c0 = row.createCell(0); c0.setCellValue(detailRowIdx - 1); c0.setCellStyle(borderStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(t.getPatronId()); c1.setCellStyle(borderStyle);
                Cell c2 = row.createCell(2); c2.setCellValue(t.getPatronName()); c2.setCellStyle(borderStyle);
                Cell c3 = row.createCell(3); c3.setCellValue(t.getBorrowDate().format(dtf)); c3.setCellStyle(borderStyle);
                Cell c4 = row.createCell(4); c4.setCellValue(t.getDueDate().format(dtf)); c4.setCellStyle(borderStyle);
                Cell c5 = row.createCell(5); c5.setCellValue(t.getReturnDate() != null ? t.getReturnDate().format(dtf) : "Chưa trả"); c5.setCellStyle(borderStyle);
                Cell c6 = row.createCell(6); c6.setCellValue(t.getLibrarianName()); c6.setCellStyle(borderStyle);
                Cell c7 = row.createCell(7); c7.setCellValue(t.getCopyId()); c7.setCellStyle(borderStyle);
                Cell c8 = row.createCell(8); c8.setCellValue(t.getBookTitle()); c8.setCellStyle(borderStyle);
                Cell c9 = row.createCell(9); c9.setCellValue(t.getRenewalCount()); c9.setCellStyle(borderStyle);
                
                // Ghi nhận tiền phạt dưới dạng số (Double) để Excel có thể thực hiện hàm SUM/TÍNH TOÁN nếu cần
                Cell c10 = row.createCell(10); 
                c10.setCellValue(t.getFineAmount() != null ? t.getFineAmount().doubleValue() : 0.0); 
                c10.setCellStyle(borderStyle);
                
                Cell c11 = row.createCell(11); 
                String status = t.getStatus().equals("Borrowing") ? "Đang mượn" : 
                               (t.getStatus().equals("Returned") ? "Đã trả" : 
                               (t.getStatus().equals("Overdue") ? "Quá hạn" : t.getStatus()));
                c11.setCellValue(status); 
                c11.setCellStyle(borderStyle);
            }

            // Tự động kéo giãn độ rộng cột dựa trên nội dung dài nhất
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