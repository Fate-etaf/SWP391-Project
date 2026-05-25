package com.swp5.library_management.controller;

import com.swp5.library_management.entity.Book;
import com.swp5.library_management.service.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * GET /books  →  hiển thị danh sách sách.
     * Nếu có truyền tham số ?keyword=... thì sẽ thực hiện tìm kiếm.
     * Nếu không có keyword thì hiển thị toàn bộ sách.
     */
    @GetMapping
    public String listBooks(
            @RequestParam(required = false) String keyword,
            Model model
    ) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            // Nếu người dùng nhập từ khóa tìm kiếm
            model.addAttribute("books", bookService.searchBooks(keyword));
        } else {
            // Nếu tải trang mặc định hoặc tìm kiếm rỗng
            model.addAttribute("books", bookService.getAllBooks());
        }

        model.addAttribute("keyword", keyword); // Trả lại keyword để giữ text trên ô tìm kiếm của giao diện
        return "books/list";
    }

    @GetMapping("/add")
    public String showAddBookForm(Model model) {
        model.addAttribute("book", new Book());
        return "books/add";
    }

    @org.springframework.web.bind.annotation.PostMapping("/add")
    public String saveBook(@org.springframework.web.bind.annotation.ModelAttribute("book") Book book) {
        bookService.saveBook(book);
        return "redirect:/books";
    }
}