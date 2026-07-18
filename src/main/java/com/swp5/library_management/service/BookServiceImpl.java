package com.swp5.library_management.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.swp5.library_management.dto.AddBookForm;
import com.swp5.library_management.dto.BookDetailDTO;
import com.swp5.library_management.dto.BookSearchResultDTO;
import com.swp5.library_management.dto.CopyRowDTO;
import com.swp5.library_management.entity.*;
import com.swp5.library_management.repository.*;

@Service
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    // ── Bảng màu xoay vòng cho bìa sách mockup ────────────────────────────────
    private static final String[] COVER_COLORS = {
        "from-slate-700 to-slate-900",
        "from-blue-700 to-indigo-900",
        "from-emerald-700 to-teal-900",
        "from-red-700 to-rose-900",
        "from-violet-700 to-purple-900",
        "from-amber-600 to-orange-900",
        "from-cyan-700 to-blue-900",
        "from-fuchsia-700 to-pink-900"
    };

    private final BookRepository      bookRepository;
    private final BookCopyRepository  bookCopyRepository;
    private final AuthorRepository    authorRepository;
    private final PublisherRepository publisherRepository;
    private final SubjectRepository   subjectRepository;
    private final CampusRepository    campusRepository;
    private final ShelfRepository     shelfRepository;

    public BookServiceImpl(BookRepository bookRepository,
                           BookCopyRepository bookCopyRepository,
                           AuthorRepository authorRepository,
                           PublisherRepository publisherRepository,
                           SubjectRepository subjectRepository,
                           CampusRepository campusRepository,
                           ShelfRepository shelfRepository) {
        this.bookRepository      = bookRepository;
        this.bookCopyRepository  = bookCopyRepository;
        this.authorRepository    = authorRepository;
        this.publisherRepository = publisherRepository;
        this.subjectRepository   = subjectRepository;
        this.campusRepository    = campusRepository;
        this.shelfRepository     = shelfRepository;
    }

    // ── Librarian: getAllBooks ─────────────────────────────────────────────────

    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    // ── Librarian: saveBook (Merged from origin/main) ─────────────────────────

    @Override
    @Transactional
    public Book saveBook(AddBookForm form, Integer campusId) {

        // ── 1. Resolve Author(s) — find or create ──────────────────────────────
        Set<Author> authors = new HashSet<>();
        if (StringUtils.hasText(form.getAuthorName())) {
            for (String name : form.getAuthorName().split(",")) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    Author author = authorRepository
                            .findByAuthorNameIgnoreCase(trimmed)
                            .orElseGet(() -> authorRepository.save(
                                    Author.builder().authorName(trimmed).build()));
                    authors.add(author);
                }
            }
        }

        // ── 2. Resolve Publisher — find or create ──────────────────────────────
        Publisher publisher = null;
        if (StringUtils.hasText(form.getPublisherName())) {
            publisher = publisherRepository
                    .findByPublisherNameIgnoreCase(form.getPublisherName().trim())
                    .orElseGet(() -> publisherRepository.save(
                            Publisher.builder()
                                    .publisherName(form.getPublisherName().trim())
                                    .build()));
        }

        // ── 3. Resolve Subject — lookup only (must exist in DB) ───────────────
        Subject subject = null;
        if (StringUtils.hasText(form.getSubjectCode())) {
            subject = subjectRepository
                    .findById(form.getSubjectCode().trim())
                    .orElse(null);   // silently ignore unknown codes
        }

        // ── 4. Build and save the Book ─────────────────────────────────────────
        Book book = new Book();
        book.setTitle(form.getTitle());
        book.setIsbn(form.getIsbn());
        book.setLanguage(StringUtils.hasText(form.getLanguage()) ? form.getLanguage() : "Vietnamese");
        book.setPublishYear(form.getPublishYear());
        book.setCoverImageUrl(form.getCoverImageUrl());
        book.setDescription(form.getDescription());
        book.setEdition(form.getEdition());
        book.setShelfCode(form.getShelfCode());
        book.setCreatedAt(LocalDateTime.now());
        book.setAuthors(authors);
        book.setPublisher(publisher);
        book.setSubject(form.getSubjectCode() != null ? subject : null);

        Book saved = bookRepository.saveAndFlush(book);

        // ── 5. Create BookCopy records if copies count provided ────────────────
        System.out.println("DEBUG SAVEBOOK: copies=" + form.getCopies() + ", campusId=" + campusId);
        if (form.getCopies() != null && form.getCopies() > 0) {
            // Use the logged-in user's campus for the copies
            Campus defaultCampus = null;
            if (campusId != null) {
                defaultCampus = campusRepository.findById(campusId).orElse(null);
            }
            System.out.println("DEBUG SAVEBOOK: defaultCampus=" + (defaultCampus == null ? "null" : defaultCampus.getCampusName()));
            Shelf shelf = shelfRepository
            .findById(saved.getShelfCode())
            .orElseThrow();
            if (defaultCampus != null) {
                for (int i = 1; i <= form.getCopies(); i++) {
                    String copyId = "BOOK-" + saved.getBookId() + "-" + i;
                    System.out.println("DEBUG SAVEBOOK: Creating copy with ID: " + copyId);
                    BookCopy copy = BookCopy.builder()
                            .copyId(copyId)
                            .book(saved)
                            .campus(defaultCampus)
                            .shelf(shelf)
                            .copyStatus("Available")
                            .conditionStatus("Good")
                            .acquiredAt(LocalDateTime.now())
                            .build();
                    BookCopy savedCopy = bookCopyRepository.saveAndFlush(copy);
                    System.out.println("DEBUG SAVEBOOK: Saved copy: " + savedCopy.getCopyId());
                }
            }
        }


        return saved;
    }

    // ── UCG01 – searchBooks ───────────────────────────────────────────────────

    /**
     * Tìm kiếm sách.
     *
     * Chiến lược hiệu năng:
     *   1. Query tìm kiếm trả về List<Book> (lazy copies — tránh CartesianProduct với EntityGraph).
     *   2. Lấy tất cả bookId từ kết quả.
     *   3. Một query batch duy nhất đếm Available copies theo (bookId, campusId).
     *   4. Map kết quả sang DTO mà không cần truy cập lazy collection.
     */
    @Override
    public Page<BookSearchResultDTO> searchBooks(String keyword, String subjectCode, Integer categoryId, Integer majorId, Integer campusId, int page, int size) {
        // Sanitize: blank string → null để WHERE clause bỏ qua bộ lọc
        String kw = StringUtils.hasText(keyword)     ? keyword.trim()     : null;
        String sc = StringUtils.hasText(subjectCode) ? subjectCode.trim() : null;

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Book> bookPage = bookRepository.searchBooks(kw, sc, categoryId, majorId, campusId, pageable);
        
        List<BookSearchResultDTO> dtoList = mapBooksToSearchResults(bookPage.getContent(), campusId);
        return new PageImpl<>(dtoList, pageable, bookPage.getTotalElements());
    }

    // ── UCG01 – E1 Fallback: getRecentBooks ───────────────────────────────────

    @Override
    public List<BookSearchResultDTO> getRecentBooks(Integer campusId) {
        List<Book> recent = bookRepository.findTop8ByOrderByCreatedAtDesc();
        return mapBooksToSearchResults(recent, campusId);
    }

    // ── UCG02 – getBookDetail ─────────────────────────────────────────────────

    /**
     * Load chi tiết sách. BookRepository.findById đã được override với @EntityGraph
     * nên tất cả quan hệ (authors, categories, copies, campus, shelf, subject, publisher)
     * được load trong 1 câu SQL, không có N+1.
     *
     * @throws NoSuchElementException nếu bookId không tồn tại (UCG02 – E1).
     */
    @Override
    public BookDetailDTO getBookDetail(Integer bookId, Integer campusId) {
        
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Book not found with id: " + bookId));
            System.out.println("Book ID = " + bookId);
            System.out.println("Copies from entity = " + book.getCopies().size());
        // ── Lọc và sắp xếp bản sao theo campus ──────────────────────────────
        List<BookCopy> copies;
        if (campusId != null) {
            
            copies = book.getCopies().stream()
                    .filter(c -> c.getCampus().getCampusId().equals(campusId))
                    .collect(Collectors.toList());
        } else {
            copies = new ArrayList<>(book.getCopies());
        }

        // Sắp xếp: Available lên đầu, sau đó theo copyId
        copies.sort(Comparator
                .<BookCopy, Integer>comparing(c -> "Available".equals(c.getCopyStatus()) ? 0 : 1)
                .thenComparing(BookCopy::getCopyId));

        // ── Map sang CopyRowDTO ───────────────────────────────────────────────
        List<CopyRowDTO> copyRows = copies.stream().map(c -> CopyRowDTO.builder()
                .copyId(c.getCopyId())
                .campusName(c.getCampus() != null ? c.getCampus().getCampusName() : "—")
                .conditionStatus(c.getConditionStatus())
                .copyStatus(c.getCopyStatus())
                .shelfCode(c.getShelf()  != null ? c.getShelf().getShelfCode() : "—")
                .shelfName(c.getShelf()  != null ? c.getShelf().getShelfName() : "—")
                .build()
        ).collect(Collectors.toList());

        boolean hasAvailableCopy = copies.stream()
                .anyMatch(c -> "Available".equals(c.getCopyStatus()));

        String categoryNames = book.getCategories().stream()
                .map(Category::getCategoryName)
                .collect(Collectors.joining(", "));

        // Chọn màu bìa giả ngẫu nhiên theo bookId (ổn định, không random mỗi lần)
        String coverColor = COVER_COLORS[book.getBookId() % COVER_COLORS.length];

        return BookDetailDTO.builder()
                .bookId(book.getBookId())
                .title(book.getTitle())
                .isbn(book.getIsbn())
                .publishYear(book.getPublishYear())
                .edition(book.getEdition())
                .language(book.getLanguage())
                .description(book.getDescription())
                .coverImageUrl(book.getCoverImageUrl())
                .coverColor(coverColor)
                .authorNames(book.getAuthorNames())
                .publisherName(book.getPublisher() != null
                        ? book.getPublisher().getPublisherName() : "Chưa có thông tin")
                .subjectCode(book.getSubject()  != null ? book.getSubject().getSubjectCode() : null)
                .subjectName(book.getSubject()  != null ? book.getSubject().getSubjectName() : null)
                .categoryNames(categoryNames.isEmpty() ? "Chưa phân loại" : categoryNames)
                .copies(copyRows)
                .hasAvailableCopy(hasAvailableCopy)
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Chuyển đổi List<Book> sang List<BookSearchResultDTO>.
     *
     * Dùng batch query để tính availableCount thay vì lazy load copies từng cuốn (tránh N+1).
     */
    private List<BookSearchResultDTO> mapBooksToSearchResults(List<Book> books, Integer campusId) {
        if (books.isEmpty()) return List.of();

        List<Integer> bookIds = books.stream()
                .map(Book::getBookId)
                .collect(Collectors.toList());

        // Batch count cho available (có sẵn)
        List<Object[]> availableRows = (campusId != null)
                ? bookCopyRepository.countAvailableByBookIdsAndCampus(bookIds, campusId)
                : bookCopyRepository.countAvailableByBookIds(bookIds);

        Map<Integer, Long> availableMap = new HashMap<>();
        for (Object[] row : availableRows) {
            availableMap.put((Integer) row[0], (Long) row[1]);
        }

        // Batch count cho total (tổng cộng)
        List<Object[]> totalRows = (campusId != null)
                ? bookCopyRepository.countTotalByBookIdsAndCampus(bookIds, campusId)
                : bookCopyRepository.countTotalByBookIds(bookIds);

        Map<Integer, Long> totalMap = new HashMap<>();
        for (Object[] row : totalRows) {
            totalMap.put((Integer) row[0], (Long) row[1]);
        }

        List<BookSearchResultDTO> result = new ArrayList<>();
        for (int i = 0; i < books.size(); i++) {
            Book book = books.get(i);

            String subjectCode = (book.getSubject() != null)
                    ? book.getSubject().getSubjectCode() : null;

            String categoryNames = book.getCategories().stream()
                    .map(Category::getCategoryName)
                    .collect(Collectors.joining(", "));

            result.add(BookSearchResultDTO.builder()
                    .bookId(book.getBookId())
                    .title(book.getTitle())
                    .isbn(book.getIsbn() != null ? book.getIsbn() : "N/A")
                    .authorNames(book.getAuthorNames())
                    .subjectCode(subjectCode)
                    .categoryNames(categoryNames.isEmpty() ? "Chưa phân loại" : categoryNames)
                    .availableCount(availableMap.getOrDefault(book.getBookId(), 0L))
                    .totalCount(totalMap.getOrDefault(book.getBookId(), 0L))
                    .coverImageUrl(book.getCoverImageUrl())
                    .coverColor(COVER_COLORS[i % COVER_COLORS.length])
                    .build());
        }
        return result;
    }
}
