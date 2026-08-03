package com.swp5.library_management.controller;

import com.swp5.library_management.service.ViolationService;
import com.swp5.library_management.service.BookReturnService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final ViolationService violationService;
    private final BookReturnService bookReturnService;
    private final com.swp5.library_management.repository.UserRepository userRepository;

    @Value("${app.webhook.api-key}")
    private String expectedApiKey;

    private static final Pattern NOPPHAT_PATTERN = Pattern.compile("NOPPHAT(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRASACH_PATTERN = Pattern.compile("TRASACH(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MATSAT_PATTERN = Pattern.compile("MATSAT(\\d+)", Pattern.CASE_INSENSITIVE);

    @GetMapping({ "/webhook", "/webhook/" })
    public ResponseEntity<String> testWebhook() {
        return ResponseEntity.ok("SePay Webhook Endpoint is online. Please use POST method to submit data.");
    }

    @PostMapping({ "/webhook", "/webhook/" })
    public ResponseEntity<Map<String, Object>> handlePaymentWebhook(
            @RequestBody SepayWebhookRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-API-Key", required = false) String xApiKeyHeader) {

        log.info("Received payment webhook: {}", request);

        // 1. Xác thực bảo mật (API Key)
        // SePay cho phép cấu hình gửi API Key qua Header (Authorization: Apikey <token>
        // hoặc X-API-Key: <token>)
        boolean isAuthorized = false;

        if (xApiKeyHeader != null && xApiKeyHeader.equals(expectedApiKey)) {
            isAuthorized = true;
        } else if (authHeader != null) {
            String token = authHeader.replace("Apikey ", "").trim();
            if (token.equals(expectedApiKey)) {
                isAuthorized = true;
            }
        }

        if (!isAuthorized) {
            log.warn("Unauthorized webhook request. Authorization Header: {}, X-API-Key: {}", authHeader,
                    xApiKeyHeader);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Unauthorized key verify failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        // 2. Phân tích nội dung chuyển khoản
        String content = request.getContent();
        if (content == null || content.isBlank()) {
            content = request.getDescription(); // backup field
        }

        if (content == null || content.isBlank()) {
            return badRequest("Nội dung chuyển khoản trống.");
        }

        // Loại bỏ khoảng trắng
        String cleanContent = content.replaceAll("\\s+", "");
        Matcher matcherNopphat = NOPPHAT_PATTERN.matcher(cleanContent);
        Matcher matcherTrasach = TRASACH_PATTERN.matcher(cleanContent);
        Matcher matcherMatsat = MATSAT_PATTERN.matcher(cleanContent);

        String transactionCode = request.getReferenceCode();
        if (transactionCode == null || transactionCode.isBlank()) {
            transactionCode = request.getCode();
        }
        if (transactionCode == null || transactionCode.isBlank()) {
            transactionCode = "AUTO-QR-" + System.currentTimeMillis();
        }

        // Thử tìm kiếm và trích xuất Token bảo mật VietQR (dạng PAY... đứng trước hoặc nằm trong nội dung chuyển khoản)
        String payToken = null;
        Pattern payPattern = Pattern.compile("PAY[A-Z0-9]+", Pattern.CASE_INSENSITIVE);
        Matcher payMatcher = payPattern.matcher(content);
        if (payMatcher.find()) {
            payToken = payMatcher.group(0);
        }

        com.swp5.library_management.utils.PaymentTokenUtil.DecodedToken decoded = null;
        if (payToken != null) {
            java.util.List<com.swp5.library_management.entity.User> librarians = userRepository.findAnyLibrarian();
            decoded = com.swp5.library_management.utils.PaymentTokenUtil.decodeToken(payToken, librarians);
        }

        if (decoded != null) {
            String librarianId = decoded.getLibrarianId();
            if (librarianId == null || librarianId.trim().isEmpty()) {
                librarianId = "SYSTEM_AUTO";
            }
            Integer id = decoded.getId();
            String action = decoded.getAction().toUpperCase();

            log.info("Decrypted Payment Token successfully. Action: {}, ID: {}, Librarian: {}", action, id, librarianId);

            if ("T".equals(action)) {
                try {
                    bookReturnService.processOverdueReturn(id, "QRCode", transactionCode, librarianId, null);
                } catch (IllegalArgumentException e) {
                    log.error("Ticket detail not found for token return: {}", id);
                    return badRequest(e.getMessage());
                } catch (Exception e) {
                    log.error("Internal error processing automatic return with token: " + id, e);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Lỗi xử lý tự động trả sách: " + e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                }

                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("success", true);
                successResponse.put("message", "Hệ thống tự động trả sách và thu tiền quá hạn cho lượt mượn #" + id + " thành công (Token).");
                return ResponseEntity.ok(successResponse);

            } else if ("M".equals(action)) {
                try {
                    bookReturnService.processLost(id, "QRCode", transactionCode, librarianId, null);
                } catch (IllegalArgumentException e) {
                    log.error("Ticket detail not found for token lost reporting: {}", id);
                    return badRequest(e.getMessage());
                } catch (Exception e) {
                    log.error("Internal error processing automatic lost reporting with token: " + id, e);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Lỗi xử lý tự động báo mất: " + e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                }

                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("success", true);
                successResponse.put("message", "Hệ thống tự động ghi nhận báo mất sách và thanh toán gộp thành công cho lượt mượn #" + id + " (Token).");
                return ResponseEntity.ok(successResponse);

            } else if ("D".equals(action)) {
                try {
                    bookReturnService.processDamaged(id, "QRCode", transactionCode, librarianId, null);
                } catch (IllegalArgumentException e) {
                    log.error("Ticket detail not found for token damaged reporting: {}", id);
                    return badRequest(e.getMessage());
                } catch (Exception e) {
                    log.error("Internal error processing automatic damaged reporting with token: " + id, e);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Lỗi xử lý tự động báo hỏng: " + e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                }

                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("success", true);
                successResponse.put("message", "Hệ thống tự động ghi nhận báo hỏng sách và thanh toán gộp thành công cho lượt mượn #" + id + " (Token).");
                return ResponseEntity.ok(successResponse);

            } else if ("F".equals(action)) {
                try {
                    violationService.collectFineQR(id, librarianId, transactionCode);
                } catch (IllegalArgumentException e) {
                    log.error("Fine invoice not found for token: {}", id);
                    return badRequest(e.getMessage());
                } catch (IllegalStateException e) {
                    log.warn("Fine invoice already paid: {}", id);
                    Map<String, Object> successResponse = new HashMap<>();
                    successResponse.put("success", true);
                    successResponse.put("message", "Hóa đơn đã được thanh toán từ trước.");
                    return ResponseEntity.ok(successResponse);
                } catch (Exception e) {
                    log.error("Internal error processing automatic payment for fine " + id, e);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Lỗi xử lý hệ thống: " + e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                }

                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("success", true);
                successResponse.put("message", "Hệ thống tự động ghi nhận thanh toán hóa đơn phạt #" + id + " thành công (Token).");
                return ResponseEntity.ok(successResponse);
            }
        }

        if (matcherNopphat.find()) {
            // Trường hợp 1: Nộp phạt cho hóa đơn phạt ĐÃ ĐƯỢC TẠO (Unpaid FineInvoice)
            String fineIdStr = matcherNopphat.group(1);
            Integer fineId;
            try {
                fineId = Integer.parseInt(fineIdStr);
            } catch (NumberFormatException e) {
                return badRequest("Mã hóa đơn phạt không hợp lệ: " + fineIdStr);
            }

            try {
                log.info("Auto processing fine payment for Fine ID: {} with trade code: {}", fineId, transactionCode);
                // Gọi service gạch nợ với định danh người thực hiện là "SYSTEM_AUTO"
                violationService.collectFineQR(fineId, "SYSTEM_AUTO", transactionCode);
            } catch (IllegalArgumentException e) {
                log.error("Fine invoice not found: {}", fineId);
                return badRequest(e.getMessage());
            } catch (IllegalStateException e) {
                log.warn("Fine invoice already paid: {}", fineId);
                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("success", true);
                successResponse.put("message", "Hóa đơn đã được thanh toán từ trước.");
                return ResponseEntity.ok(successResponse);
            } catch (Exception e) {
                log.error("Internal error processing automatic payment for fine " + fineId, e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Lỗi xử lý hệ thống: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "Hệ thống tự động ghi nhận thanh toán hóa đơn phạt #" + fineId + " thành công.");
            return ResponseEntity.ok(successResponse);

        } else if (matcherTrasach.find()) {
            // Trường hợp 2: Trả sách quá hạn trực tiếp từ giao diện (chưa tạo hóa đơn phạt)
            String ticketDetailIdStr = matcherTrasach.group(1);
            Integer ticketDetailId;
            try {
                ticketDetailId = Integer.parseInt(ticketDetailIdStr);
            } catch (NumberFormatException e) {
                return badRequest("Mã lượt mượn không hợp lệ: " + ticketDetailIdStr);
            }

            try {
                log.info("Auto processing overdue book return & fine payment for TicketDetail ID: {} with trade code: {}", ticketDetailId, transactionCode);
                // Gọi service trả sách quá hạn (tự động tạo hóa đơn phạt dạng PAID)
                bookReturnService.processOverdueReturn(ticketDetailId, "QRCode", transactionCode, "SYSTEM_AUTO", null);
            } catch (IllegalArgumentException e) {
                log.error("Ticket detail not found or process error: {}", ticketDetailId);
                return badRequest(e.getMessage());
            } catch (Exception e) {
                log.error("Internal error automatic returning book for ticket " + ticketDetailId, e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Lỗi xử lý tự động trả sách: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "Hệ thống tự động trả sách và thu tiền quá hạn cho lượt mượn #" + ticketDetailId + " thành công.");
            return ResponseEntity.ok(successResponse);

        } else if (matcherMatsat.find()) {
            // Trường hợp 3: Báo mất sách tự động nhận diện thanh toán từ Webhook
            String ticketDetailIdStr = matcherMatsat.group(1);
            Integer ticketDetailId;
            try {
                ticketDetailId = Integer.parseInt(ticketDetailIdStr);
            } catch (NumberFormatException e) {
                return badRequest("Mã lượt mượn không hợp lệ: " + ticketDetailIdStr);
            }

            try {
                log.info("Auto processing lost book combined fine payment for TicketDetail ID: {} with trade code: {}", ticketDetailId, transactionCode);
                // Gọi processLost với payNow = true và phương thức QRCode
                bookReturnService.processLost(ticketDetailId, "QRCode", transactionCode, "SYSTEM_AUTO", null);
            } catch (IllegalArgumentException e) {
                log.error("Ticket detail not found or process error: {}", ticketDetailId);
                return badRequest(e.getMessage());
            } catch (Exception e) {
                log.error("Internal error automatic lost/damaged book reporting for ticket " + ticketDetailId, e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Lỗi xử lý tự động báo mất: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "Hệ thống tự động ghi nhận báo mất sách và thanh toán gộp thành công cho lượt mượn #" + ticketDetailId);
            return ResponseEntity.ok(successResponse);

        } else {
            log.info("Nội dung chuyển khoản không khớp cú pháp NOPPHAT{id} hoặc TRASACH{id}: {}", content);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Nội dung giao dịch không liên quan đến thu phạt / trả sách.");
            return ResponseEntity.ok(response);
        }
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.badRequest().body(response);
    }

    @Data
    public static class SepayWebhookRequest {
        private Long id;
        private String gateway;
        private String transactionDate;
        private String accountNumber;
        private String subAccount;
        private Double transferAmount; // Số tiền GD
        private Double accumulated;
        private String code; // Mã giao dịch ngân hàng
        private String content; // Nội dung chuyển khoản
        private String transferType; // in / out
        private String description; // Mô tả chi tiết giao dịch
        private String referenceCode; // Mã đối chiếu ngân hàng
        private String signature;
    }
}
