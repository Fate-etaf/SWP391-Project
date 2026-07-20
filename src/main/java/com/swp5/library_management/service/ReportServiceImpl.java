package com.swp5.library_management.service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.swp5.library_management.dto.ReportDataDTO;
import com.swp5.library_management.dto.ReportFilterDTO;
import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.repository.BorrowTicketDetailRepository;
import com.swp5.library_management.repository.specification.TransactionSpecification;

import jakarta.servlet.http.HttpServletResponse;

@Service
public class ReportServiceImpl implements ReportService {

    private final BorrowTicketDetailRepository borrowDetailRepo;

    public ReportServiceImpl(BorrowTicketDetailRepository borrowDetailRepo) {
        this.borrowDetailRepo = borrowDetailRepo;
    }

    @Override
    public ReportDataDTO<?> generateReport(ReportFilterDTO filter) {
        // Tùy theo User bấm Tab Báo cáo nào trên màn hình, ta điều hướng xử lý tương
        // ứng
        String reportType = filter.getReportType();
        if (reportType == null)
            reportType = "BORROW";

        switch (reportType) {
            case "BORROW":
                return generateBorrowReport(filter);
            case "FINE":
                // return generateFineReport(filter); // Sẽ code ở các bước sau
            case "TRANSFER":
                // return generateTransferReport(filter); // Sẽ code ở các bước sau
            default:
                return generateBorrowReport(filter);
        }
    }

    private ReportDataDTO<BorrowTicketDetail> generateBorrowReport(ReportFilterDTO filter) {
        ReportDataDTO<BorrowTicketDetail> data = new ReportDataDTO<>();

        // 1. Phân trang & Sắp xếp (Dữ liệu giao dịch luôn ưu tiên thời gian mới nhất
        // lên đầu)
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(),
                Sort.by(Sort.Direction.DESC, "borrowTicket.createdAt"));

        // 2. Sinh câu truy vấn SQL thông minh
        Specification<BorrowTicketDetail> spec = TransactionSpecification.buildBorrowFilter(filter);

        // 3. Kéo dữ liệu từ Database
        Page<BorrowTicketDetail> pageData = borrowDetailRepo.findAll(spec, pageable);

        // 4. Xử lý Hard Limit: Bảo vệ trình duyệt khỏi bị sập do dữ liệu quá lớn
        long totalRecords = pageData.getTotalElements();
        if (totalRecords > 1000) {
            data.setHardLimited(true);
        }

        data.setTableData(pageData);

        // 5. Gắn Top Cards nhanh
        data.setTotalRecords(totalRecords);
        data.setTotalBooksCirculated(totalRecords);

        return data;
    }

    @Override
    public void exportTransactionReportToExcel(ReportFilterDTO filter, HttpServletResponse response)
            throws IOException {
        // 1. Lấy TOÀN BỘ dữ liệu (Dùng Sort, BỎ QUA Pageable để lấy Full danh sách)
        Specification<BorrowTicketDetail> spec = TransactionSpecification.buildBorrowFilter(filter);
        List<BorrowTicketDetail> exportData = borrowDetailRepo.findAll(spec,
                Sort.by(Sort.Direction.DESC, "borrowTicket.createdAt"));

        // 2. Khởi tạo file Excel (XSSFWorkbook cho định dạng .xlsx)
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Lich_Su_Giao_Dich");

            // 3. Tạo style cho Dòng Tiêu Đề (Header)
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 4. Tạo Header
            Row headerRow = sheet.createRow(0);
            String[] headers = { "Mã Phiếu", "Mã Bản Sao", "Tên Sách", "Người Mượn", "Mã SV/CB", "Thủ Thư Xử Lý",
                    "Ngày Mượn", "Hạn Trả", "Ngày Trả Thực Tế", "Trạng Thái" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 5. Đổ dữ liệu vào các dòng
            int rowIdx = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (BorrowTicketDetail detail : exportData) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue("#" + detail.getBorrowTicket().getTicketId());
                row.createCell(1).setCellValue(detail.getBookCopy().getCopyId());
                row.createCell(2).setCellValue(detail.getBookCopy().getBook().getTitle());
                row.createCell(3).setCellValue(detail.getBorrowTicket().getPatron().getFullName());
                row.createCell(4).setCellValue(detail.getBorrowTicket().getPatron().getUserId());

                String librarianName = detail.getBorrowTicket().getLibrarian() != null
                        ? detail.getBorrowTicket().getLibrarian().getFullName()
                        : "N/A";
                row.createCell(5).setCellValue(librarianName);

                row.createCell(6)
                        .setCellValue(detail.getBorrowTicket().getCreatedAt() != null
                                ? detail.getBorrowTicket().getCreatedAt().format(formatter)
                                : "");
                row.createCell(7)
                        .setCellValue(detail.getDueDate() != null ? detail.getDueDate().format(formatter) : "");
                row.createCell(8).setCellValue(
                        detail.getReturnDate() != null ? detail.getReturnDate().format(formatter) : "Chưa trả");
                row.createCell(9).setCellValue(detail.getStatus() != null ? detail.getStatus() : "");
            }

            // Tự động căn chỉnh độ rộng cột
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 6. Cấu hình HTTP Response để trình duyệt tự động tải file xuống
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=Lich_Su_Giao_Dich.xlsx");

            // 7. Ghi dữ liệu file Excel vào luồng xuất (OutputStream) của HTTP
            workbook.write(response.getOutputStream());
            response.flushBuffer();
        }
    }
}