package com.swp5.library_management.service;

import java.io.IOException;

import com.swp5.library_management.dto.ReportDataDTO;
import com.swp5.library_management.dto.ReportFilterDTO;

import jakarta.servlet.http.HttpServletResponse;

public interface ReportService {

    /**
     * Hàm master sinh báo cáo: Tự động rẽ nhánh dữ liệu dựa vào thuộc tính
     * ReportType
     */
    ReportDataDTO<?> generateReport(ReportFilterDTO filter);

    /**
     * Hàm xuất thẳng dữ liệu ra file Excel và trả về qua luồng HTTP
     */
    void exportTransactionReportToExcel(ReportFilterDTO filter, HttpServletResponse response) throws IOException;
}