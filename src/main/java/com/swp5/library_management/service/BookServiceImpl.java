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

        // ── 0. Check ISBN Uniqueness ───────────────────────────────────────────
        if (StringUtils.hasText(form.getIsbn())) {
            String cleanIsbn = form.getIsbn().trim();
            if (bookRepository.existsByIsbn(cleanIsbn)) {
                throw new IllegalArgumentException("Mã ISBN '" + cleanIsbn + "' đã tồn tại trong hệ thống!");
            }
        }

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
    public Page<BookSearchResultDTO> searchBooks(String keyword, String subjectCode, Integer categoryId, Integer majorId, Integer filterCampusId, Integer displayCampusId, int page, int size) {
        // Sanitize: blank string → null để WHERE clause bỏ qua bộ lọc
        String kw = StringUtils.hasText(keyword)     ? keyword.trim()     : null;
        String sc = StringUtils.hasText(subjectCode) ? subjectCode.trim() : null;

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        // Lọc DB theo filterCampusId (nếu user chọn dropdown)
        Page<Book> bookPage = bookRepository.searchBooks(kw, sc, categoryId, majorId, filterCampusId, pageable);
        
        // Tính toán hiển thị số lượng theo displayCampusId (cơ sở của sinh viên)
        List<BookSearchResultDTO> dtoList = mapBooksToSearchResults(bookPage.getContent(), displayCampusId);
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
                .shelfNumber(c.getShelf() != null ? c.getShelf().getShelfNumber() : null)
                .shelfName(c.getShelf()  != null ? c.getShelf().getShelfName() : "—")
                .shelfCodeTopic(c.getShelf() != null ? c.getShelf().getShelfCodeTopic() : "—")
                .build()
        ).collect(Collectors.toList());

        boolean hasAvailableCopy = copies.stream()
                .anyMatch(c -> "Available".equals(c.getCopyStatus()));

        String categoryNames = book.getCategories().stream()
                .map(Category::getCategoryName)
                .collect(Collectors.joining(", "));

        // Tra cứu thông tin chi tiết của Kệ sách dựa trên shelfCode của Book
        com.swp5.library_management.entity.Shelf mainShelf = book.getShelfCode() != null 
                ? shelfRepository.findById(book.getShelfCode()).orElse(null) : null;

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
                .shelfCode(book.getShelfCode())
                .shelfNumber(mainShelf != null ? mainShelf.getShelfNumber() : null)
                .shelfName(mainShelf != null ? mainShelf.getShelfName() : null)
                .shelfCodeTopic(mainShelf != null ? mainShelf.getShelfCodeTopic() : null)
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

    // ── Excel Import ──────────────────────────────────────────────────────────

    /**
     * Tạo file Excel mẫu (template) để librarian download và điền thông tin.
     * Cột: Title | AuthorName | ISBN | PublisherName | PublishYear | Edition | Language | SubjectCode | ShelfCode | Copies | Description | CoverImageUrl
     */
    @Override
    public byte[] generateImportTemplate() throws Exception {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Books");

            // Header style
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.ORANGE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);

            // Create header row
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Title (*)", "AuthorName (*)", "ISBN", "PublisherName",
                "PublishYear", "Edition", "Language", "SubjectCode",
                "ShelfCode", "Copies", "Description", "CoverImageUrl"
            };
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }
            sheet.setColumnWidth(0, 8000);
            sheet.setColumnWidth(10, 10000);

            // Sample row
            org.apache.poi.ss.usermodel.Row sampleRow = sheet.createRow(1);
            String[] sampleData = {
                "Lập Trình Java Cơ Bản", "Nguyễn Văn A", "978-604-1234-56-7",
                "NXB Giáo Dục", "2023", "1st Edition", "Vietnamese",
                "PRF192", "A1", "5", "Sách lập trình Java dành cho sinh viên.", ""
            };
            for (int i = 0; i < sampleData.length; i++) {
                sampleRow.createCell(i).setCellValue(sampleData[i]);
            }

            // Instruction sheet
            org.apache.poi.ss.usermodel.Sheet instrSheet = workbook.createSheet("Hướng dẫn");
            String[] instructions = {
                "HƯỚNG DẪN SỬ DỤNG FILE IMPORT SÁCH",
                "",
                "(*) = Bắt buộc phải nhập",
                "- Title: Tên sách (bắt buộc)",
                "- AuthorName: Tên tác giả (bắt buộc). Nhiều tác giả cách nhau dấu phẩy.",
                "- ISBN: Mã ISBN (tùy chọn, nhưng không được trùng nếu có)",
                "- PublisherName: Tên nhà xuất bản (sẽ tự tạo mới nếu chưa có)",
                "- PublishYear: Năm xuất bản (số nguyên, ví dụ: 2023)",
                "- Edition: Phiên bản (ví dụ: 1st Edition)",
                "- Language: Ngôn ngữ (mặc định: Vietnamese nếu để trống)",
                "- SubjectCode: Mã môn học (phải tồn tại trong hệ thống)",
                "- ShelfCode: Mã kệ sách (phải tồn tại trong hệ thống)",
                "- Copies: Số bản sao cần tạo (số nguyên >= 0, mặc định: 0)",
                "- Description: Mô tả nội dung sách (tùy chọn)",
                "- CoverImageUrl: URL ảnh bìa sách (tùy chọn)"
            };
            for (int i = 0; i < instructions.length; i++) {
                org.apache.poi.ss.usermodel.Row row = instrSheet.createRow(i);
                row.createCell(0).setCellValue(instructions[i]);
            }
            instrSheet.setColumnWidth(0, 20000);

            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    /**
     * Đọc và xử lý file Excel import sách hàng loạt.
     * Cột theo thứ tự: Title | AuthorName | ISBN | PublisherName | PublishYear | Edition | Language | SubjectCode | ShelfCode | Copies | Description | CoverImageUrl
     */
    @Override
    @Transactional
    public ImportResult importBooksFromExcel(java.io.InputStream inputStream, Integer campusId) throws Exception {
        int successCount = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();

        try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(inputStream)) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                errors.add("File Excel không có sheet dữ liệu.");
                return new ImportResult(0, 1, errors);
            }

            int lastRow = sheet.getLastRowNum();
            for (int rowIdx = 1; rowIdx <= lastRow; rowIdx++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                String title = getCellString(row, 0);
                if (title == null || title.isBlank()) continue;

                String authorName     = getCellString(row, 1);
                String isbn           = getCellString(row, 2);
                String publisherName  = getCellString(row, 3);
                String publishYearStr = getCellString(row, 4);
                String edition        = getCellString(row, 5);
                String language       = getCellString(row, 6);
                String subjectCode    = getCellString(row, 7);
                String shelfCode      = getCellString(row, 8);
                String copiesStr      = getCellString(row, 9);
                String description    = getCellString(row, 10);
                String coverImageUrl  = getCellString(row, 11);

                if (authorName == null || authorName.isBlank()) {
                    errors.add("Dòng " + (rowIdx + 1) + ": Thiếu tên tác giả (AuthorName).");
                    continue;
                }

                Integer publishYear = null;
                if (publishYearStr != null && !publishYearStr.isBlank()) {
                    try { publishYear = Integer.parseInt(publishYearStr.trim()); }
                    catch (NumberFormatException e) {
                        errors.add("Dòng " + (rowIdx + 1) + ": PublishYear không hợp lệ ('" + publishYearStr + "').");
                        continue;
                    }
                }

                Integer copies = 0;
                if (copiesStr != null && !copiesStr.isBlank()) {
                    try { copies = Integer.parseInt(copiesStr.trim()); }
                    catch (NumberFormatException e) {
                        errors.add("Dòng " + (rowIdx + 1) + ": Copies không hợp lệ ('" + copiesStr + "').");
                        continue;
                    }
                }

                try {
                    String cleanIsbn = (isbn != null && !isbn.isBlank()) ? isbn.trim() : null;

                    // ── Kiểm tra ISBN trùng: nếu đã tồn tại thì chỉ thêm copies ──────────
                    if (cleanIsbn != null) {
                        java.util.Optional<com.swp5.library_management.entity.Book> existingOpt =
                                bookRepository.findByIsbn(cleanIsbn);

                        if (existingOpt.isPresent()) {
                            com.swp5.library_management.entity.Book existingBook = existingOpt.get();

                            if (copies != null && copies > 0) {
                                Campus campus = campusId != null
                                        ? campusRepository.findById(campusId).orElse(null) : null;

                                // Lấy shelf từ shelfCode trong dòng Excel (ưu tiên) hoặc shelf của sách cũ
                                String resolvedShelfCode = (shelfCode != null && !shelfCode.isBlank())
                                        ? shelfCode.trim() : existingBook.getShelfCode();
                                Shelf shelf = (resolvedShelfCode != null)
                                        ? shelfRepository.findById(resolvedShelfCode).orElse(null) : null;

                                // Tính offset để không trùng copyId với các bản sao đã có
                                int existingCopyCount = bookCopyRepository.countByBook(existingBook);

                                if (campus != null) {
                                    for (int i = 1; i <= copies; i++) {
                                        String copyId = "BOOK-" + existingBook.getBookId() + "-" + (existingCopyCount + i);
                                        BookCopy copy = BookCopy.builder()
                                                .copyId(copyId)
                                                .book(existingBook)
                                                .campus(campus)
                                                .shelf(shelf)
                                                .copyStatus("Available")
                                                .conditionStatus("Good")
                                                .acquiredAt(LocalDateTime.now())
                                                .build();
                                        bookCopyRepository.saveAndFlush(copy);
                                    }
                                    successCount++;
                                } else {
                                    errors.add("Dòng " + (rowIdx + 1) + " ('" + title + "'): ISBN '" + cleanIsbn
                                            + "' đã tồn tại — thêm " + copies + " bản sao, nhưng campus không hợp lệ.");
                                }
                            } else {
                                // Copies = 0 và ISBN đã tồn tại — bỏ qua im lặng, không cần làm gì
                            }
                            continue; // Xử lý dòng tiếp theo
                        }
                    }

                    // ── ISBN mới hoặc không có ISBN: tạo sách mới bình thường ──────────
                    AddBookForm form = new AddBookForm();
                    form.setTitle(title.trim());
                    form.setAuthorName(authorName.trim());
                    form.setIsbn(cleanIsbn);
                    form.setPublisherName(publisherName != null && !publisherName.isBlank() ? publisherName.trim() : null);
                    form.setPublishYear(publishYear);
                    form.setEdition(edition != null && !edition.isBlank() ? edition.trim() : null);
                    form.setLanguage(language != null && !language.isBlank() ? language.trim() : "Vietnamese");
                    form.setSubjectCode(subjectCode != null && !subjectCode.isBlank() ? subjectCode.trim() : null);
                    form.setShelfCode(shelfCode != null && !shelfCode.isBlank() ? shelfCode.trim() : null);
                    form.setCopies(copies);
                    form.setDescription(description != null && !description.isBlank() ? description.trim() : null);
                    form.setCoverImageUrl(coverImageUrl != null && !coverImageUrl.isBlank() ? coverImageUrl.trim() : null);

                    saveBook(form, campusId);
                    successCount++;
                } catch (IllegalArgumentException e) {
                    errors.add("Dòng " + (rowIdx + 1) + " ('" + title + "'): " + e.getMessage());
                } catch (Exception e) {
                    errors.add("Dòng " + (rowIdx + 1) + " ('" + title + "'): Lỗi - " + e.getMessage());
                }
            }
        }

        return new ImportResult(successCount, errors.size(), errors);
    }

    /** Đọc giá trị cell thành String, hỗ trợ cả cell kiểu số và chuỗi. */
    private String getCellString(org.apache.poi.ss.usermodel.Row row, int colIdx) {
        org.apache.poi.ss.usermodel.Cell cell = row.getCell(colIdx, org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:  return cell.getStringCellValue();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return String.valueOf((int) cell.getNumericCellValue());
                }
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val)) return String.valueOf((long) val);
                return String.valueOf(val);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return cell.getStringCellValue(); }
                catch (Exception e) { return String.valueOf(cell.getNumericCellValue()); }
            default: return null;
        }
    }
}
