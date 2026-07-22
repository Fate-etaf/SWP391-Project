package com.swp5.library_management.service;

import com.swp5.library_management.dto.GraduationCheckDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface GraduationService {

    /**
     * Đọc file Excel chứa danh sách sinh viên, kiểm tra nghĩa vụ thư viện
     * và trả về danh sách kết quả.
     *
     * @param file file Excel (.xlsx) với cột A = Mã SV, cột B = Họ tên
     * @return danh sách kết quả kiểm tra
     */
    List<GraduationCheckDTO> checkFromExcel(MultipartFile file);
}
