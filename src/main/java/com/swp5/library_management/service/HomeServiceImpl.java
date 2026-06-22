package com.swp5.library_management.service;

import com.swp5.library_management.dto.CategoryCardDTO;
import com.swp5.library_management.dto.FeaturedBookDTO;
import com.swp5.library_management.dto.HomeStatsDTO;
import com.swp5.library_management.entity.Book;
import com.swp5.library_management.entity.Category;
import com.swp5.library_management.repository.BookCopyRepository;
import com.swp5.library_management.repository.BookRepository;
import com.swp5.library_management.repository.CampusRepository;
import com.swp5.library_management.repository.CategoryRepository;
import com.swp5.library_management.repository.UserRepository;
import com.swp5.library_management.repository.MajorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.*;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeServiceImpl implements HomeService {

    private final BookRepository      bookRepository;
    private final BookCopyRepository  bookCopyRepository;
    private final CampusRepository    campusRepository;
    private final CategoryRepository  categoryRepository;
    private final UserRepository      userRepository;   // ← Đã inject được vì User entity tồn tại
    private final MajorRepository     majorRepository;

    // ── Bảng màu xoay vòng cho thẻ Category ───────────────────────────────────
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

        // FIX: Không còn hardcode 0 — lấy thật từ DB bảng Users
        long activeReaders   = userRepository.countByStatus("Active");

        return HomeStatsDTO.builder()
                .totalBooks(totalBooks)
                .availableCopies(availableCopies)
                .activeReaders(activeReaders)
                .totalCampuses(totalCampuses)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. DANH SÁCH CƠ SỞ
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<com.swp5.library_management.entity.Campus> getCampuses() {
        return campusRepository.findAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. THỂ LOẠI NỔI BẬT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<CategoryCardDTO> getFeaturedCategories(int limit) {
        List<Category> categories = categoryRepository
                .findAllByOrderByCategoryIdAsc(PageRequest.of(0, limit));

        Map<Integer, Long> countByCategoryId = buildCategoryCountMap();

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
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
        List<Book> books = bookRepository.findTop4ByOrderByCreatedAtDesc();

        List<FeaturedBookDTO> result = new ArrayList<>();
        for (int i = 0; i < books.size(); i++) {
            Book book = books.get(i);

            String categoryName = book.getCategories().stream()
                    .findFirst()
                    .map(Category::getCategoryName)
                    .orElse("Chưa phân loại");

            String subjectCode = (book.getSubject() != null)
                    ? book.getSubject().getSubjectCode()
                    : "N/A";

            result.add(FeaturedBookDTO.builder()
                    .bookId(book.getBookId())
                    .title(book.getTitle())
                    .authorNames(book.getAuthorNames())
                    .subjectCode(subjectCode)
                    .isbn(book.getIsbn() != null ? book.getIsbn() : "N/A")
                    .categoryName(categoryName)
                    .available(book.getAvailableCount() > 0)
                    .coverColor(COVER_COLORS[i % COVER_COLORS.length])
                    .coverImageUrl(book.getCoverImageUrl())
                    .build());
        }
        return result;
    }

    @Override
    public List<com.swp5.library_management.dto.BookSectionDTO> getMajorsWithRandomBooks() {
        List<com.swp5.library_management.entity.Major> allMajors = majorRepository.findAll();
        List<com.swp5.library_management.dto.BookSectionDTO> result = new ArrayList<>();

        for (com.swp5.library_management.entity.Major major : allMajors) {
            List<com.swp5.library_management.entity.Book> randomBooks = bookRepository.findTop5RandomByMajor(major.getMajorId());
            
            if (randomBooks.isEmpty()) {
                continue;
            }

            result.add(com.swp5.library_management.dto.BookSectionDTO.builder()
                    .sectionName(major.getMajorName())
                    .books(mapToFeaturedBookDTOs(randomBooks))
                    .build());
        }

        // Sách ngoài chuyên ngành
        List<com.swp5.library_management.entity.Book> outsideBooks = bookRepository.findTop5RandomOutsideMajors();
        if (!outsideBooks.isEmpty()) {
            result.add(com.swp5.library_management.dto.BookSectionDTO.builder()
                    .sectionName("Sách ngoài chuyên ngành")
                    .books(mapToFeaturedBookDTOs(outsideBooks))
                    .build());
        }

        return result;
    }

    private List<com.swp5.library_management.dto.FeaturedBookDTO> mapToFeaturedBookDTOs(List<com.swp5.library_management.entity.Book> books) {
        List<com.swp5.library_management.dto.FeaturedBookDTO> bookDTOs = new ArrayList<>();
        for (int i = 0; i < books.size(); i++) {
            com.swp5.library_management.entity.Book book = books.get(i);
            
            String subjectCode = (book.getSubject() != null)
                    ? book.getSubject().getSubjectCode()
                    : "N/A";

            String categoryName = "N/A";
            if (book.getCategories() != null && !book.getCategories().isEmpty()) {
                categoryName = book.getCategories().iterator().next().getCategoryName();
            }
                    
            bookDTOs.add(com.swp5.library_management.dto.FeaturedBookDTO.builder()
                    .bookId(book.getBookId())
                    .title(book.getTitle())
                    .authorNames(book.getAuthorNames())
                    .subjectCode(subjectCode)
                    .isbn(book.getIsbn() != null ? book.getIsbn() : "N/A")
                    .categoryName(categoryName)
                    .available(book.getAvailableCount() > 0)
                    .coverColor(COVER_COLORS[i % COVER_COLORS.length])
                    .coverImageUrl(book.getCoverImageUrl())
                    .build());
        }
        return bookDTOs;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private Map<Integer, Long> buildCategoryCountMap() {
        Map<Integer, Long> map = new HashMap<>();
        bookRepository.countBooksGroupedByCategory()
                .forEach(row -> map.put((Integer) row[0], (Long) row[1]));
        return map;
    }
}