package com.swp5.library_management;

import com.swp5.library_management.dto.AddBookForm;
import com.swp5.library_management.entity.Book;
import com.swp5.library_management.entity.BookCopy;
import com.swp5.library_management.repository.BookCopyRepository;
import com.swp5.library_management.repository.BookRepository;
import com.swp5.library_management.repository.CampusRepository;
import com.swp5.library_management.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LibraryManagementApplicationTests {

	@Autowired
	private CampusRepository campusRepository;

	@Autowired
	private BookService bookService;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private BookCopyRepository bookCopyRepository;

	@Test
	void contextLoads() {
		System.out.println("=== CAMPUS DIAGNOSTICS ===");
		campusRepository.findAll().forEach(campus -> {
			System.out.println("Campus ID: " + campus.getCampusId() + ", Name: " + campus.getCampusName());
		});
		System.out.println("==========================");
	}

	@Autowired
	private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	@Test
	@Transactional
	void testSaveBookWithCopies() {
		AddBookForm form = new AddBookForm();
		form.setTitle("Test Title Debug Mismatch");
		form.setAuthorName("Debug Mismatch Author");
		form.setIsbn("MISMATCH-ISBN-123");
		form.setLanguage("English");
		form.setCopies(3);
		form.setCampusId(1);

		Book saved = bookService.saveBook(form, 1);
		assertNotNull(saved);
		assertNotNull(saved.getBookId());

		System.out.println("=== ID MISMATCH CHECK ===");
		System.out.println("Hibernate Book ID: " + saved.getBookId());

		// Query database directly to see what Campuses exist
		List<Map<String, Object>> campusesDb = jdbcTemplate.queryForList("SELECT * FROM dbo.Campuses");
		System.out.println("Campuses in DB: " + campusesDb);

		// Query database directly to see what BookID was actually inserted for this ISBN
		List<Integer> dbBookIds = jdbcTemplate.queryForList(
				"SELECT BookID FROM dbo.Books WHERE ISBN = 'MISMATCH-ISBN-123'",
				Integer.class
		);
		System.out.println("Database Book IDs found: " + dbBookIds);
		System.out.println("=========================");

		List<BookCopy> copies = bookCopyRepository.findByBookBookId(saved.getBookId());
		System.out.println("=== SAVED COPIES DIAGNOSTICS ===");
		System.out.println("Copies count: " + copies.size());
		System.out.println("================================");

		assertEquals(3, copies.size(), "Copies should be 3");
	}


}


