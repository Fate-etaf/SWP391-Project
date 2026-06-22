package com.swp5.library_management.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swp5.library_management.entity.BookCopy;
import com.swp5.library_management.entity.Campus;
import com.swp5.library_management.entity.TransferDetail;
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
    public TransferRequest getTransferById(Integer transferId) {
        return transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lệnh luân chuyển!"));
    }

    @Override
    @Transactional
    public void createTransfer(Integer fromCampusId, Integer toCampusId, List<String> copyIds, String requestedByUserId,
            String note) {
        if (fromCampusId.equals(toCampusId)) {
            throw new RuntimeException("Cơ sở xuất phát và cơ sở đích không được trùng nhau!");
        }

        Campus fromCampus = campusRepository.findById(fromCampusId)
                .orElseThrow(() -> new RuntimeException("Cơ sở xuất phát không hợp lệ!"));
        Campus toCampus = campusRepository.findById(toCampusId)
                .orElseThrow(() -> new RuntimeException("Cơ sở đích không hợp lệ!"));
        User requester = userRepository.findById(requestedByUserId)
                .orElseThrow(() -> new RuntimeException("Người dùng không hợp lệ!"));

        // Tạo Lệnh Luân chuyển mới
        TransferRequest request = new TransferRequest();
        request.setFromCampus(fromCampus);
        request.setToCampus(toCampus);
        request.setRequestedBy(requester);
        request.setRequestedAt(LocalDateTime.now());
        request.setStatus("Pending"); // Trạng thái mặc định ban đầu
        request.setNote(note);

        List<TransferDetail> details = new ArrayList<>();

        // Kiểm tra và khóa các bản sao sách
        for (String copyId : copyIds) {
            // Tối ưu: Cắt bỏ khoảng trắng thừa nếu thủ thư nhập "CP-01, CP-02"
            String cleanCopyId = copyId.trim();
            if (cleanCopyId.isEmpty())
                continue;

            BookCopy copy = bookCopyRepository.findById(cleanCopyId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy mã sách: " + cleanCopyId));

            // Kiểm tra trạng thái có Sẵn sàng không
            if (!"Available".equals(copy.getCopyStatus())) {
                throw new RuntimeException(
                        "Bản sao " + cleanCopyId + " không ở trạng thái Available (Đang có người mượn hoặc giữ).");
            }

            // Kiểm tra sách có đúng nằm ở cơ sở của người tạo lệnh không
            if (!copy.getCampus().getCampusId().equals(fromCampusId)) {
                throw new RuntimeException("Bản sao " + cleanCopyId + " không nằm tại kho của cơ sở bạn!");
            }

            // Đổi trạng thái sách sang InTransfer để sinh viên không mượn được nữa
            copy.setCopyStatus("InTransfer");
            bookCopyRepository.save(copy);

            TransferDetail detail = new TransferDetail();
            detail.setTransferRequest(request);
            detail.setCopy(copy);
            details.add(detail);
        }

        if (details.isEmpty()) {
            throw new RuntimeException("Vui lòng nhập ít nhất 1 mã sách hợp lệ!");
        }

        request.setDetails(details);
        transferRequestRepository.save(request); // Lưu lệnh luân chuyển (Cascade lưu luôn TransferDetail)
    }

    @Override
    @Transactional
    public void cancelTransfer(Integer transferId) {
        TransferRequest request = getTransferById(transferId);

        if (!"Pending".equals(request.getStatus())) {
            throw new RuntimeException("Chỉ có thể hủy lệnh luân chuyển khi đang chờ xử lý (Pending)!");
        }

        request.setStatus("Cancelled");

        // Nhả các cuốn sách về lại trạng thái Available cho cơ sở xuất phát
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
    @Transactional
    public void markAsInTransit(Integer transferId, Integer librarianCampusId) {
        TransferRequest request = getTransferById(transferId);

        if (!"Pending".equals(request.getStatus())) {
            throw new RuntimeException("Lệnh luân chuyển này không ở trạng thái chờ xuất kho!");
        }

        // BẢO MẬT: Chỉ Thủ thư thuộc cơ sở xuất phát (Từ đâu) mới được bấm Xuất kho
        if (!request.getFromCampus().getCampusId().equals(librarianCampusId)) {
            throw new RuntimeException(
                    "Từ chối truy cập: Chỉ thủ thư tại cơ sở ĐÓNG GÓI mới có quyền xuất kho lô hàng này!");
        }

        request.setStatus("InTransit");
        request.setShippedAt(LocalDateTime.now()); // Ghi nhận thời gian giao cho shipper
        transferRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void confirmReceipt(Integer transferId, String confirmedByUserId) {
        TransferRequest request = getTransferById(transferId);

        if (!"InTransit".equals(request.getStatus())) {
            throw new RuntimeException("Lệnh luân chuyển chưa được xuất kho, không thể xác nhận nhận hàng!");
        }

        User user = userRepository.findById(confirmedByUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người xác nhận!"));

        request.setStatus("Received");
        request.setReceivedAt(LocalDateTime.now()); // Ghi nhận thời gian nhập kho
        request.setConfirmedBy(user);

        // Cập nhật tọa độ (CampusID) mới cho sách vật lý và mở khóa Available
        if (request.getDetails() != null) {
            for (TransferDetail detail : request.getDetails()) {
                BookCopy copy = detail.getCopy();
                copy.setCampus(request.getToCampus()); // Đổi hộ khẩu sách sang cơ sở đích
                copy.setCopyStatus("Available"); // Sẵn sàng lên kệ cho mượn
                bookCopyRepository.save(copy);
            }
        }

        transferRequestRepository.save(request);
    }
}