package com.swp5.library_management.service;

import com.swp5.library_management.dto.GraduationCheckDTO;
import com.swp5.library_management.entity.BorrowTicketDetail;
import com.swp5.library_management.entity.FineInvoice;
import com.swp5.library_management.repository.GraduationRepository;
import com.swp5.library_management.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraduationServiceImpl implements GraduationService {

    private final UserRepository userRepository;
    private final GraduationRepository graduationRepository;

    @Override
    public List<GraduationCheckDTO> checkFromExcel(MultipartFile file) {
        // 1. Đọc file Excel thành danh sách thô chứa mã SV + họ tên
        List<GraduationCheckDTO> rawExcelList = parseExcel(file);
        if (rawExcelList.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Thu thập danh sách userId duy nhất
        List<String> studentIds = rawExcelList.stream()
                .map(GraduationCheckDTO::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 3. Kiểm tra các SV tồn tại trong hệ thống + lấy email
        Map<String, String> emailMap = new HashMap<>();
        Set<String> existingIds = new HashSet<>();
        for (String id : studentIds) {
            userRepository.findById(id).ifPresent(user -> {
                existingIds.add(id);
                emailMap.put(id, user.getEmail());
            });
        }

        // 4. Lấy chi tiết sách chưa trả và phiếu phạt chưa thanh toán
        List<BorrowTicketDetail> unreturnedList = graduationRepository.findUnreturnedByUserIds(studentIds);
        List<FineInvoice> unpaidFines = graduationRepository.findUnpaidFinesByUserIds(studentIds);

        // Nhóm sách lại theo từng UserID
        Map<String, List<BorrowTicketDetail>> unreturnedGroup = unreturnedList.stream()
                .filter(d -> d.getBorrowTicket() != null && d.getBorrowTicket().getPatron() != null)
                .collect(Collectors.groupingBy(d -> d.getBorrowTicket().getPatron().getUserId()));

        // Nhóm phạt lại theo từng UserID
        Map<String, List<FineInvoice>> unpaidFinesGroup = unpaidFines.stream()
                .filter(f -> f.getPatron() != null)
                .collect(Collectors.groupingBy(f -> f.getPatron().getUserId()));

        List<GraduationCheckDTO> finalDetailedList = new ArrayList<>();

        // 5. Lọc và ánh xạ từng lỗi cụ thể của học sinh theo thứ tự file Excel
        for (GraduationCheckDTO student : rawExcelList) {
            String sid = student.getStudentId();
            String name = student.getFullName();
            String studentEmail = emailMap.getOrDefault(sid, "");

            // Trường hợp 1: Tài khoản không có trong hệ thống
            if (!existingIds.contains(sid)) {
                finalDetailedList.add(GraduationCheckDTO.builder()
                        .studentId(sid)
                        .fullName(name)
                        .email(studentEmail)
                        .existsInSystem(false)
                        .cleared(false)
                        .copyId("—")
                        .bookTitle("—")
                        .reason("Tài khoản không tồn tại trong hệ thống")
                        .build());
                continue;
            }

            List<BorrowTicketDetail> books = unreturnedGroup.getOrDefault(sid, new ArrayList<>());
            List<FineInvoice> fines = unpaidFinesGroup.getOrDefault(sid, new ArrayList<>());

            // Trường hợp 2: Hoàn thành mọi nghĩa vụ
            if (books.isEmpty() && fines.isEmpty()) {
                finalDetailedList.add(GraduationCheckDTO.builder()
                        .studentId(sid)
                        .fullName(name)
                        .email(studentEmail)
                        .existsInSystem(true)
                        .cleared(true)
                        .copyId("—")
                        .bookTitle("—")
                        .reason("Hoàn thành")
                        .build());
            } else {
                // Trường hợp 3: Có vi phạm -> Tách từng sách chưa trả thành 1 dòng lỗi
                for (BorrowTicketDetail detail : books) {
                    String cId = (detail.getBookCopy() != null) ? detail.getBookCopy().getCopyId() : "—";
                    String title = (detail.getBookCopy() != null && detail.getBookCopy().getBook() != null)
                            ? detail.getBookCopy().getBook().getTitle() : "—";

                    finalDetailedList.add(GraduationCheckDTO.builder()
                            .studentId(sid)
                            .fullName(name)
                            .email(studentEmail)
                            .existsInSystem(true)
                            .cleared(false)
                            .copyId(cId)
                            .bookTitle(title)
                            .reason("Chưa trả sách")
                            .build());
                }

                // Tách từng phiếu phạt chưa thanh toán thành 1 dòng lỗi riêng
                for (FineInvoice fine : fines) {
                    String cId = "—";
                    String title = "—";

                    if (fine.getTicketDetail() != null && fine.getTicketDetail().getBookCopy() != null) {
                        cId = fine.getTicketDetail().getBookCopy().getCopyId();
                        if (fine.getTicketDetail().getBookCopy().getBook() != null) {
                            title = fine.getTicketDetail().getBookCopy().getBook().getTitle();
                        }
                    }

                    // Lý do lấy tự động trong trường reason của FineInvoice
                    String reasonText = fine.getReason();
                    if (reasonText == null || reasonText.isBlank()) {
                        reasonText = "Phạt vi phạm (" + fine.getViolationType() + ")";
                    }

                    finalDetailedList.add(GraduationCheckDTO.builder()
                            .studentId(sid)
                            .fullName(name)
                            .email(studentEmail)
                            .existsInSystem(true)
                            .cleared(false)
                            .copyId(cId)
                            .bookTitle(title)
                            .reason(reasonText)
                            .remainingAmount(fine.getRemainingAmount())
                            .build());
                }
            }
        }

        return finalDetailedList;
    }

    // Đọc Excel dòng tiêu đề dòng 1
    private List<GraduationCheckDTO> parseExcel(MultipartFile file) {
        List<GraduationCheckDTO> list = new ArrayList<>();
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            boolean isFirstRow = true;

            for (Row row : sheet) {
                if (isFirstRow) {
                    isFirstRow = false;
                    continue;
                }

                String studentId = getCellStringValue(row.getCell(0));
                String fullName = getCellStringValue(row.getCell(1));

                if (studentId == null || studentId.isBlank()) {
                    continue;
                }

                GraduationCheckDTO dto = GraduationCheckDTO.builder()
                        .studentId(studentId.trim())
                        .fullName(fullName != null ? fullName.trim() : "")
                        .build();
                list.add(dto);
            }
        } catch (Exception e) {
            log.error("Lỗi khi đọc file Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể đọc file Excel. Vui lòng kiểm tra định dạng file.", e);
        }
        return list;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            default -> null;
        };
    }
}
