package com.swp5.library_management.controller;

import com.swp5.library_management.dto.ChatRequest;
import com.swp5.library_management.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {
private final BookRepository bookRepository;
    // 🔴 LƯU Ý: Thay bằng API Key Gemini thực tế của bạn (Tạo miễn phí trên Google AI Studio)
    private final String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY"; 
    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY;

   // 2. Chỉ có DUY NHẤT một hàm chatWithAI nhận request này:
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chatWithAI(@RequestBody ChatRequest request) {
        Map<String, String> responseBody = new HashMap<>();
        try {
            // 2. Lấy danh sách toàn bộ sách hiện có từ SQL Server lên
            // Giả sử Entity Book của bạn có hàm getTitle() hoặc getBookName() để lấy tên sách
            String availableBooks = "Không có";
            try {
                availableBooks = bookRepository.findAll().stream()
                        .map(book -> "- " + book.getTitle() + " (Mã: " + book.getBookId() + ")") 
                        .collect(Collectors.joining("\\n"));
            } catch (Exception dbEx) {
                // Phòng trường hợp lỗi DB thì AI vẫn chạy bằng dữ liệu nền
                System.out.println("Chưa lấy được danh sách sách từ DB: " + dbEx.getMessage());
            }

            RestTemplate restTemplate = new RestTemplate();

            // 3. Đút danh sách sách thực tế vào System Prompt để ép AI học thuộc lòng
            String systemPrompt = "Bạn là trợ lý ảo thông minh tên FLMS-Bot của Thư viện số Đại học FPT. "
                    + "Nhiệm vụ của bạn là tư vấn mượn sách, review góp ý nội dung và gợi ý tài liệu học tập cho bạn đọc.\\n"
                    + "DƯỚI ĐÂY LÀ DANH SÁCH CÁC CUỐN SÁCH THỰC TẾ ĐANG CÓ TRONG THƯ VIỆN CỦA TRƯỜNG:\\n"
                    + availableBooks + "\\n\\n"
                    + "LƯU Ý QUAN TRỌNG: Khi sinh viên hỏi mua/mượn hoặc tìm kiếm sách, bạn CHỈ ĐƯỢC PHÉP gợi ý các cuốn sách có trong danh sách thực tế trên. "
                    + "Hãy trả lời ngắn gọn, lịch sự, xưng hô là 'Mình' và gọi bạn đọc là 'Bạn' hoặc 'Cậu'.\\n\\n"
                    + "Câu hỏi của bạn đọc: ";

            String fullPrompt = systemPrompt + request.getMessage();

            // Tạo chuỗi JSON gửi đi theo cấu trúc chuẩn của Gemini API
            String jsonRequest = "{\"contents\": [{\"parts\": [{\"text\": \"" + fullPrompt.replace("\"", "\\\"").replace("\n", "\\n") + "\"}]}]}";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(jsonRequest, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(GEMINI_URL, entity, String.class);
            String responseStr = response.getBody();

            // Sử dụng Regex bốc nhanh nội dung phản hồi từ AI
            String aiAnswer = "";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"text\"\\s*:\\s*\"([^\"]*)\"");
            java.util.regex.Matcher matcher = pattern.matcher(responseStr);
            
            if (matcher.find()) {
                aiAnswer = matcher.group(1);
                aiAnswer = aiAnswer.replace("\\n", "\n").replace("\\\"", "\"");
            } else {
                aiAnswer = "Mình đã nghe rõ câu hỏi, nhưng hệ thống xử lý chữ đang bận. Cậu thử hỏi lại nhé!";
            }

            responseBody.put("reply", aiAnswer);
            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            e.printStackTrace();
            responseBody.put("reply", "Lỗi kết nối hệ thống trợ lý ngầm: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBody);
        }
    }
}