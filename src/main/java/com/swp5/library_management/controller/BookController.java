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
    @GetMapping
    public String listBooks(
            @RequestParam(required = false) String keyword,
            Model model
    ) {

        model.addAttribute(
                "books",
                bookService.searchBooks(keyword)
        );

        model.addAttribute("keyword", keyword);

        return "books/list";
    }
}
