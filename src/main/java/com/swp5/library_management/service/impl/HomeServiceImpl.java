package com.swp5.library_management.service.impl;

import com.swp5.library_management.Entity.Book;
import com.swp5.library_management.Entity.Campus;
import com.swp5.library_management.Entity.Category;
import com.swp5.library_management.dto.CategoryCardDTO;
import com.swp5.library_management.dto.FeaturedBookDTO;
import com.swp5.library_management.dto.HomeStatsDTO;
import com.swp5.library_management.repository.BookCopyRepository;
import com.swp5.library_management.repository.BookRepository;
import com.swp5.library_management.repository.CampusRepository;
import com.swp5.library_management.repository.CategoryRepository;
import com.swp5.library_management.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation của {@link HomeService}.
 *
 * <p>Đây là nơi duy nhất chứa toàn bộ logic nghiệp vụ phục vụ trang chủ:
 * truy vấn DB qua các Repository, tính toán, định dạng số liệu,
 * rồi đóng gói kết quả vào DTO để trả cho Controller.
 *
 * <p>Annotation {@code @Transactional(readOnly = true)} đánh dấu toàn bộ
 * class là read-only transaction — đây là best practice cho các method
 * chỉ đọc dữ liệu vì nó:
 * <ul>
 *   <li>Giúp Hibernate bỏ qua bước "dirty checking" (kiểm tra thay đổi), tăng hiệu năng.</li>
 *   <li>Cho phép lazy-loading hoạt động đúng trong phạm vi transaction.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeServiceImpl implements HomeService {

    private final BookRepository      bookRepository;
    private final BookCopyRepository  bookCopyRepository;
    private final CampusRepository    campusRepository;
    private final CategoryRepository  categoryRepository;

    // ── Bảng màu xoay vòng cho thẻ Category ───────────────────────────────────
    // Các giá trị này là CSS Tailwind thuần UI, không có ý nghĩa nghiệp vụ —
    // đặt ở đây (Service/DTO) là đúng chỗ, không nên đưa vào Entity hay View.
    private static final String[] CATEGORY_BG_CLASSES = {
        "bg-blue-50 text-blue-600",
        "bg-emerald-50 text-emerald-600",
        "bg-amber-50 text-amber-600",
        "bg-indigo-50 text-indigo-600",
        "bg-rose-50 text-rose-600"
    };
    private static final String[] CATEGORY_COLOR_KEYS = {
        "blue", "green", "amber", "indigo", "rose"
    };

    // ── Bảng màu xoay vòng cho bìa sách mockup ────────────────────────────────
    private static final String[] COVER_COLORS = {
        "from-slate-700 to-slate-900",
        "from-blue-700 to-indigo-900",
        "from-emerald-700 to-teal-900",
        "from-red-700 to-rose-900",
        "from-violet-700 to-purple-900",
        "from-amber-700 to-orange-900"
    };

    // ─────────────────────────────────────────────────────────────────────────
    // 1. THỐNG KÊ TỔNG QUAN
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public HomeStatsDTO getHomeStats() {
        long totalBooks      = bookRepository.count();
        long availableCopies = bookCopyRepository.countByCopyStatus("Available");
        long totalCampuses   = campusRepository.count();

        // TODO: Thay giá trị 0 bằng userRepository.countByStatus("Active")
        //       sau khi có Entity User/Reader trong hệ thống.
        long activeReaders = 0L;

        return HomeStatsDTO.builder()
                .totalBooks(totalBooks)
                .availableCopies(availableCopies)
                .activeReaders(activeReaders)
                .totalCampuses(totalCampuses)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. DANH SÁCH CƠ SỞ (cho dropdown tìm kiếm)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<String> getCampusNames() {
        // Lấy tất cả Campus từ DB, chỉ cần tên để hiển thị trong dropdown.
        return campusRepository.findAll()
                .stream()
                .map(Campus::getCampusName)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. THỂ LOẠI NỔI BẬT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<CategoryCardDTO> getFeaturedCategories(int limit) {
        // Bước 1: Lấy N category đầu tiên từ DB (PageRequest giới hạn kết quả).
        List<Category> categories = categoryRepository
                .findAllByOrderByCategoryIdAsc(PageRequest.of(0, limit));

        // Bước 2: Lấy toàn bộ số lượng sách theo category trong 1 câu query.
        //         Nếu gọi từng category một trong vòng lặp → N+1 queries → chậm.
        //         Dùng Map để tra cứu O(1) thay vì O(N).
        Map<Integer, Long> countByCategoryId = buildCategoryCountMap();

        // Bước 3: Ghép Category + count + CSS class thành DTO
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US); // "1,240"
        List<CategoryCardDTO> result = new ArrayList<>();

        for (int i = 0; i < categories.size(); i++) {
            Category cat   = categories.get(i);
            long     count = countByCategoryId.getOrDefault(cat.getCategoryId(), 0L);

            result.add(CategoryCardDTO.builder()
                    .name(cat.getCategoryName())
                    .bookCount(nf.format(count) + " đầu sách")
                    .bgClass(CATEGORY_BG_CLASSES[i % CATEGORY_BG_CLASSES.length])
                    .colorKey(CATEGORY_COLOR_KEYS[i % CATEGORY_COLOR_KEYS.length])
                    .build());
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. SÁCH NỔI BẬT MỚI NHẤT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<FeaturedBookDTO> getFeaturedBooks() {
        // Repository đã dùng @EntityGraph để JOIN FETCH tất cả quan hệ cần thiết
        // trong 1 câu SQL → không lo N+1 hay LazyInitializationException.
        List<Book> books = bookRepository.findTop4ByOrderByCreatedAtDesc();

        List<FeaturedBookDTO> result = new ArrayList<>();
        for (int i = 0; i < books.size(); i++) {
            Book book = books.get(i);

            // Lấy tên thể loại đầu tiên (dùng findFirst vì Set không có thứ tự)
            String categoryName = book.getCategories().stream()
                    .findFirst()
                    .map(Category::getCategoryName)
                    .orElse("Chưa phân loại");

            String subjectCode = (book.getSubject() != null)
                    ? book.getSubject().getSubjectCode()
                    : "N/A";

            result.add(FeaturedBookDTO.builder()
                    .title(book.getTitle())
                    .authorNames(book.getAuthorNames())   // method helper có sẵn trong Entity Book
                    .subjectCode(subjectCode)
                    .isbn(book.getIsbn() != null ? book.getIsbn() : "N/A")
                    .categoryName(categoryName)
                    .available(book.getAvailableCount() > 0) // method helper có sẵn trong Entity Book
                    .coverColor(COVER_COLORS[i % COVER_COLORS.length])
                    .build());
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Xây dựng Map [CategoryID → số sách] từ 1 câu query duy nhất.
     *
     * <p>Tách ra thành private method riêng để giữ code trong
     * {@link #getFeaturedCategories} gọn gàng và dễ đọc.
     */
    private Map<Integer, Long> buildCategoryCountMap() {
        Map<Integer, Long> map = new HashMap<>();
        bookRepository.countBooksGroupedByCategory()
                .forEach(row -> map.put((Integer) row[0], (Long) row[1]));
        return map;
    }
}
