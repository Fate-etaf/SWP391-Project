package com.swp5.library_management.service.impl;

import com.swp5.library_management.dto.BorrowingHistoryDTO;
import com.swp5.library_management.entity.Book;
import com.swp5.library_management.entity.BookCopy;
import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.repository.BorrowTicketDetailRepository;
import com.swp5.library_management.service.BorrowingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BorrowingServiceImpl implements BorrowingService {

    private final BorrowTicketDetailRepository borrowTicketDetailRepository;

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

    @Override
    public List<BorrowingHistoryDTO> getBorrowingHistory(String patronId) {
        List<BorrowTicketDetail> details = borrowTicketDetailRepository.findHistoryByPatronId(patronId);
        List<BorrowingHistoryDTO> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (BorrowTicketDetail detail : details) {
            BookCopy copy = detail.getBookCopy();
            Book book = copy != null ? copy.getBook() : null;

            String resolvedStatus = "Borrowing";
            if (detail.getStatus() != null && "Lost".equalsIgnoreCase(detail.getStatus())) {
                resolvedStatus = "Lost";
            } else if (detail.getStatus() != null && "Damaged".equalsIgnoreCase(detail.getStatus())) {
                resolvedStatus = "Damaged";
            } else if (detail.getReturnDate() != null) {
                resolvedStatus = "Returned";
            } else if (detail.getDueDate() != null && detail.getDueDate().isBefore(now)) {
                resolvedStatus = "Overdue";
            }

            String authorNames = (book != null) ? book.getAuthorNames() : "Unknown Author";
            String title = (book != null) ? book.getTitle() : "Unknown Title";
            String coverUrl = (book != null) ? book.getCoverImageUrl() : null;
            Integer bookId = (book != null) ? book.getBookId() : null;
            String coverColor = (book != null) ? COVER_COLORS[book.getBookId() % COVER_COLORS.length] : COVER_COLORS[0];
            String returnCampusName = (detail.getReturnCampus() != null) ? detail.getReturnCampus().getCampusName() : null;

            BorrowingHistoryDTO dto = BorrowingHistoryDTO.builder()
                    .ticketDetailId(detail.getTicketDetailId())
                    .ticketId(detail.getBorrowTicket() != null ? detail.getBorrowTicket().getTicketId() : null)
                    .bookId(bookId)
                    .bookTitle(title)
                    .coverImageUrl(coverUrl)
                    .coverColor(coverColor)
                    .authorNames(authorNames)
                    .copyId(copy != null ? copy.getCopyId() : null)
                    .borrowDate(detail.getBorrowTicket() != null ? detail.getBorrowTicket().getCreatedAt() : null)
                    .dueDate(detail.getDueDate())
                    .returnDate(detail.getReturnDate())
                    .renewalCount(detail.getRenewalCount())
                    .status(resolvedStatus)
                    .returnCampusName(returnCampusName)
                    .build();

            list.add(dto);
        }

        return list;
    }
}
