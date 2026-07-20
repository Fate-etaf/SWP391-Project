package com.swp5.library_management.service;

import java.util.List;

import com.swp5.library_management.dto.CategoryCardDTO;
import com.swp5.library_management.dto.FeaturedBookDTO;
import com.swp5.library_management.dto.HomeStatsDTO;

/**
 * Interface định nghĩa "hợp đồng" (contract) cho tầng Service của trang chủ.
 *
 * <p>Lý do dùng Interface thay vì class trực tiếp:
 * <ul>
 *   <li>Controller chỉ phụ thuộc vào abstraction (interface) này, không phụ
 *       thuộc vào implementation cụ thể. Đây là nguyên tắc Dependency Inversion
 *       trong SOLID.</li>
 *   <li>Dễ thay thế implementation sau này (VD: thêm cache, đổi nguồn dữ liệu)
 *       mà không cần sửa Controller.</li>
 *   <li>Dễ mock trong unit test.</li>
 * </ul>
 */
public interface HomeService {

    /**
     * Lấy dữ liệu thống kê tổng quan (số sách, bản sao, bạn đọc, cơ sở).
     *
     * @return {@link HomeStatsDTO} chứa các con số đã tính từ DB.
     */
    HomeStatsDTO getHomeStats();

    /**
     * Lấy danh sách các cơ sở để populate dropdown tìm kiếm.
     *
     * @return Danh sách cơ sở (List<com.swp5.library_management.entity.Campus>).
     */
    List<com.swp5.library_management.entity.Campus> getCampuses();

    /**
     * Lấy danh sách thể loại nổi bật để hiển thị phần Categories.
     *
     * @param limit Số lượng thể loại muốn hiển thị (thường là 5).
     * @return Danh sách {@link CategoryCardDTO} đã gắn sẵn CSS class và số sách.
     */
    List<CategoryCardDTO> getFeaturedCategories(int limit);

    /**
     * Lấy danh sách sách mới nhập kho để hiển thị phần Featured Books.
     *
     * @param campusId ID của cơ sở (để lọc số lượng bản sao có sẵn), null nếu là admin hoặc không xác định.
     * @return Danh sách {@link FeaturedBookDTO} từ 4 cuốn sách mới nhất trong DB.
     */
    List<FeaturedBookDTO> getFeaturedBooks(Integer campusId);

    /**
     * Lấy tất cả các chuyên ngành và sách ngoài chuyên ngành, mỗi chuyên ngành kèm theo 5 sách ngẫu nhiên.
     * @param campusId ID của cơ sở để tính số bản sao.
     */
    List<com.swp5.library_management.dto.BookSectionDTO> getMajorsWithRandomBooks(Integer campusId);
}
