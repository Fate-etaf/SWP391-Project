package com.swp5.library_management.service;

import com.swp5.library_management.repository.BookCopyRepository;
import com.swp5.library_management.repository.BorrowTicketDetailRepository;
import com.swp5.library_management.repository.FineInvoiceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
@ExtendWith(MockitoExtension.class)
public class ViolationServiceImplTest {

    @Mock
    private BorrowTicketDetailRepository borrowTicketDetailRepository;

    @Mock
    private FineInvoiceRepository fineInvoiceRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @InjectMocks
    private ViolationServiceImpl violationService;

    @BeforeEach
    public void setUp() {
        System.out.println("\n-------------------------------------------");
    }

    @AfterEach
    public void tearDown() {
        System.out.println("-------------------------------------------");
    }

    // ==========================================
    // NORMAL CASES (N)
    // ==========================================

    @Test
    @DisplayName("N-01: Future Date")
    public void testCalculateOverdueDays_FutureDate() {
        LocalDate dueDate = LocalDate.now().plusDays(5);
        System.out.println("[RUNNING TEST] TC-N-01: Future Date (Not Overdue)");
        System.out.println("Input: dueDate = " + dueDate + " (Today + 5 days)");
        
        long overdueDays = violationService.calculateOverdueDays(dueDate);
        
        System.out.println("Expected Output: 0");
        System.out.println("Actual Output:   " + overdueDays);
        assertEquals(0, overdueDays, "Should return 0 days for future due date");
    }

    @Test
    @DisplayName("N-02: Normal Past Date")
    public void testCalculateOverdueDays_PastDate() {
        LocalDate dueDate = LocalDate.now().minusDays(10);
        System.out.println("[RUNNING TEST] TC-N-02: Normal Past Date (Overdue)");
        System.out.println("Input: dueDate = " + dueDate + " (Today - 10 days)");
        
        long overdueDays = violationService.calculateOverdueDays(dueDate);
        
        System.out.println("Expected Output: 10");
        System.out.println("Actual Output:   " + overdueDays);
        assertEquals(10, overdueDays, "Should return exactly 10 days overdue");
    }

    @Test
    @DisplayName("N-03: Far Past Date")
    public void testCalculateOverdueDays_FarPastDate() {
        LocalDate dueDate = LocalDate.now().minusDays(365);
        System.out.println("[RUNNING TEST] TC-N-03: Far Past Date (Extremely Overdue)");
        System.out.println("Input: dueDate = " + dueDate + " (Today - 365 days)");
        
        long overdueDays = violationService.calculateOverdueDays(dueDate);
        
        System.out.println("Expected Output: 365");
        System.out.println("Actual Output:   " + overdueDays);
        assertEquals(365, overdueDays, "Should return 365 days overdue");
    }

    // ==========================================
    // ABNORMAL CASES (A)
    // ==========================================

    @Test
    @DisplayName("A-01: Null Due Date")
    public void testCalculateOverdueDays_NullDate() {
        LocalDate dueDate = null;
        System.out.println("[RUNNING TEST] TC-A-01: Null Due Date (Abnormal)");
        System.out.println("Input: dueDate = null");
        
        long overdueDays = violationService.calculateOverdueDays(dueDate);
        
        System.out.println("Expected Output: 0");
        System.out.println("Actual Output:   " + overdueDays);
        assertEquals(0, overdueDays, "Should return 0 days when dueDate is null");
    }

    // ==========================================
    // BOUNDARY CASES (B)
    // ==========================================

    @Test
    @DisplayName("B-01: Today (Exact boundary)")
    public void testCalculateOverdueDays_Today() {
        LocalDate dueDate = LocalDate.now();
        System.out.println("[RUNNING TEST] TC-B-01: Today (Exact boundary)");
        System.out.println("Input: dueDate = " + dueDate + " (Today)");
        
        long overdueDays = violationService.calculateOverdueDays(dueDate);
        
        System.out.println("Expected Output: 0");
        System.out.println("Actual Output:   " + overdueDays);
        assertEquals(0, overdueDays, "Should return 0 days when due date is today");
    }

    @Test
    @DisplayName("B-02: Yesterday (Boundary - 1 day overdue)")
    public void testCalculateOverdueDays_Yesterday() {
        LocalDate dueDate = LocalDate.now().minusDays(1);
        System.out.println("[RUNNING TEST] TC-B-02: Yesterday (Boundary - Overdue 1 day)");
        System.out.println("Input: dueDate = " + dueDate + " (Today - 1 day)");
        
        long overdueDays = violationService.calculateOverdueDays(dueDate);
        
        System.out.println("Expected Output: 1");
        System.out.println("Actual Output:   " + overdueDays);
        assertEquals(1, overdueDays, "Should return 1 day overdue for yesterday");
    }

    @Test
    @DisplayName("B-03: Tomorrow (Boundary - Tomorrow)")
    public void testCalculateOverdueDays_Tomorrow() {
        LocalDate dueDate = LocalDate.now().plusDays(1);
        System.out.println("[RUNNING TEST] TC-B-03: Tomorrow (Boundary - Not Overdue)");
        System.out.println("Input: dueDate = " + dueDate + " (Today + 1 day)");
        
        long overdueDays = violationService.calculateOverdueDays(dueDate);
        
        System.out.println("Expected Output: 0");
        System.out.println("Actual Output:   " + overdueDays);
        assertEquals(0, overdueDays, "Should return 0 days overdue for tomorrow");
    }
}
