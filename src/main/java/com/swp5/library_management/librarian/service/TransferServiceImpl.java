package com.swp5.library_management.librarian.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swp5.library_management.entity.BookCopy;
import com.swp5.library_management.entity.Campus;
import com.swp5.library_management.entity.TransferDetail;
import com.swp5.library_management.entity.TransferDetailId;
import com.swp5.library_management.entity.TransferRequest;
import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.BookCopyRepository;
import com.swp5.library_management.repository.CampusRepository;
import com.swp5.library_management.repository.TransferRequestRepository;
import com.swp5.library_management.repository.UserRepository;

@Service
public class TransferServiceImpl implements TransferService {

    private final TransferRequestRepository transferRequestRepository;
    private final BookCopyRepository bookCopyRepository;
    private final UserRepository userRepository;
    private final CampusRepository campusRepository;

    public TransferServiceImpl(TransferRequestRepository transferRequestRepository, 
                               BookCopyRepository bookCopyRepository, 
                               UserRepository userRepository,
                               CampusRepository campusRepository) {
        this.transferRequestRepository = transferRequestRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.userRepository = userRepository;
        this.campusRepository = campusRepository;
    }

    @Override
    public List<TransferRequest> getAllTransfers() {
        return transferRequestRepository.findAllWithCampusesOrderByRequestedAtDesc();
    }

    @Override
    @Transactional
    public TransferRequest createTransfer(Integer fromCampusId, Integer toCampusId, List<String> copyIds, String requestedByUserId, String note) {
        // 1. Lấy thông tin cơ bản
        User requestedBy = userRepository.findById(requestedByUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
        Campus fromCampus = campusRepository.findById(fromCampusId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ sở gửi."));
        Campus toCampus = campusRepository.findById(toCampusId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ sở nhận."));

        // 2. Khởi tạo lệnh luân chuyển (TransferRequest)
        TransferRequest request = TransferRequest.builder()
                .requestedBy(requestedBy)
                .fromCampus(fromCampus)
                .toCampus(toCampus)
                .requestedAt(LocalDateTime.now())
                .status("Pending")
                .note(note)
                .details(new ArrayList<>())
                .build();
        
        // Lưu nháp để lấy TransferID (ID tự tăng)
        request = transferRequestRepository.save(request);

        // 3. Xử lý từng cuốn sách vật lý (BookCopy)
        for (String copyId : copyIds) {
            BookCopy copy = bookCopyRepository.findById(copyId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy mã sách: " + copyId));

            // Kiểm tra điều kiện: Sách phải ở cơ sở gửi và phải đang "Available"
            if (!copy.getCampus().getCampusId().equals(fromCampusId)) {
                throw new RuntimeException("Cuốn sách " + copyId + " không nằm ở cơ sở gửi!");
            }
            if (!"Available".equals(copy.getCopyStatus())) {
                throw new RuntimeException("Cuốn sách " + copyId + " đang không có sẵn (Trạng thái: " + copy.getCopyStatus() + ")");
            }

            // Đổi trạng thái sách để không ai mượn được nữa
            copy.setCopyStatus("InTransfer");
            bookCopyRepository.save(copy);

            // Tạo chi tiết luân chuyển (TransferDetail)
            TransferDetailId detailId = new TransferDetailId(request.getTransferId(), copy.getCopyId());
            TransferDetail detail = new TransferDetail(detailId, request, copy);
            request.getDetails().add(detail);
        }

        // Lưu lại toàn bộ Request và Details
        return transferRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void markAsInTransit(Integer transferId) {
        TransferRequest request = transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lệnh luân chuyển."));
        request.setStatus("InTransit");
        request.setShippedAt(LocalDateTime.now());
        transferRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void confirmReceipt(Integer transferId, String confirmedByUserId) {
        TransferRequest request = transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lệnh luân chuyển."));
        
        User confirmedBy = userRepository.findById(confirmedByUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));

        // Cập nhật lệnh
        request.setStatus("Received");
        request.setReceivedAt(LocalDateTime.now());
        request.setConfirmedBy(confirmedBy);

        // Cập nhật sách: Chuyển hộ khẩu sang cơ sở mới và đổi trạng thái về Available
        if (request.getDetails() != null) {
            for (TransferDetail detail : request.getDetails()) {
                BookCopy copy = detail.getCopy();
                copy.setCampus(request.getToCampus()); 
                copy.setCopyStatus("Available");       
                bookCopyRepository.save(copy);
            }
        }
        transferRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void cancelTransfer(Integer transferId) {
        TransferRequest request = transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lệnh luân chuyển."));

        if (!"Pending".equals(request.getStatus())) {
            throw new RuntimeException("Chỉ có thể hủy lệnh khi đang ở trạng thái chờ xử lý (Pending).");
        }

        request.setStatus("Cancelled");

        // Trả lại trạng thái Available cho các cuốn sách ở cơ sở gốc
        if (request.getDetails() != null) {
            for (TransferDetail detail : request.getDetails()) {
                BookCopy copy = detail.getCopy();
                copy.setCopyStatus("Available");
                bookCopyRepository.save(copy);
            }
        }
        transferRequestRepository.save(request);
    }

    @Override
    public TransferRequest getTransferById(Integer transferId) {
        return transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lệnh luân chuyển."));
    }
}