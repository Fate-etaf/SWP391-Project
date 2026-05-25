package com.swp5.library_management.controller;

import com.swp5.library_management.dto.AddBookForm;
import com.swp5.library_management.service.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /** Show the full book catalogue. */
    @GetMapping({"", "/"})
    public String listBooks(Model model) {
        model.addAttribute("books", bookService.getAllBooks());
        return "books/list";
    }

    /** Show the Add Book form. */
    @GetMapping("/add")
    public String showAddBookForm(Model model) {
        model.addAttribute("bookForm", new AddBookForm());
        return "books/add";
    }

    /** Handle Add Book form submission. */
    @PostMapping("/add")
    public String saveBook(@ModelAttribute("bookForm") AddBookForm form) {
        bookService.saveBook(form);
        return "redirect:/books";
    }
}
