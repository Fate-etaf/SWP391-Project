package com.swp5.library_management.controller;

import com.swp5.library_management.dto.ChatRequest;
import com.swp5.library_management.entity.Book;
import com.swp5.library_management.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {
    
    @Autowired
    private BookRepository bookRepository;
    // 🔴 LƯU Ý: Thay bằng API Key Gemini thực tế của bạn (Tạo miễn phí trên Google AI Studio)
    private final String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY"; 
    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY;

   //Hung
   // === 2. XỬ LÝ CHATBOT VÀ TÌM KIẾM SÁCH (SUPER SEARCH) ===
   /**
    * Nhận câu hỏi từ người dùng, lọc từ khóa và tìm kiếm sách thông minh.
    * Tích hợp "Từ điển đồng nghĩa" (Smart Aliases) giúp nhận diện từ khóa (lập trình, cntt...) 
    * và tự động trả lời các câu hỏi về quy định của thư viện.
    */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chatWithAI(@RequestBody ChatRequest request) {
        Map<String, String> responseBody = new HashMap<>();

        if ("YOUR_GEMINI_API_KEY".equals(GEMINI_API_KEY)) {
            String userMsg = request.getMessage().toLowerCase();
            
            //Hung: Xử lý các câu hỏi chung về quy định thư viện (Hardcoded)
            // 1. Hardcoded generic library questions
            if (userMsg.contains("giờ mở cửa") || userMsg.contains("thời gian làm việc") || userMsg.contains("mở cửa")) {
                responseBody.put("reply", "Thư viện mở cửa từ 7h30 sáng đến 21h30 tối, từ Thứ 2 đến Chủ nhật hàng tuần bạn nhé.");
                return ResponseEntity.ok(responseBody);
            }
            if (userMsg.contains("mượn được bao nhiêu cuốn") || userMsg.contains("số lượng sách") || userMsg.contains("mượn tối đa")) {
                responseBody.put("reply", "Hiện tại quy định của thư viện là Sinh viên có thể mượn tối đa 3 cuốn sách cùng lúc.");
                return ResponseEntity.ok(responseBody);
            }
            if (userMsg.contains("mượn bao lâu") || userMsg.contains("thời gian mượn") || userMsg.contains("gia hạn")) {
                responseBody.put("reply", "Bạn có thể mượn sách tối đa trong 14 ngày. Nếu chưa đọc xong, bạn hoàn toàn có thể gia hạn thêm 1 lần thông qua website, miễn là sách đó không có ai đang đặt trước.");
                return ResponseEntity.ok(responseBody);
            }
            if (userMsg.contains("bị phạt") || userMsg.contains("mất sách") || userMsg.contains("trả trễ") || userMsg.contains("quá hạn")) {
                responseBody.put("reply", "Nếu bạn trả sách trễ hạn, hệ thống sẽ tự động tính phí phạt là 5,000đ/ngày. Nếu làm hỏng hoặc mất sách, bạn sẽ phải bồi thường theo quy định của thư viện.");
                return ResponseEntity.ok(responseBody);
            }
            if (userMsg.contains("xin chào") || userMsg.contains("hello") || userMsg.contains("hi") || userMsg.contains("chào bạn")) {
                responseBody.put("reply", "Chào bạn! Mình là FLMS-Bot, trợ lý ảo của thư viện. Mình có thể giúp gì cho bạn hôm nay? (Ví dụ: Tìm sách, hỏi về giờ mở cửa, quy định mượn trả...)");
                return ResponseEntity.ok(responseBody);
            }
            if (userMsg.contains("những loại sách nào") || userMsg.contains("những thể loại nào") || userMsg.contains("danh mục sách") || userMsg.contains("thể loại sách có trong thư viện") || userMsg.contains("các thể loại sách") || userMsg.equals("thể loại") || userMsg.equals("loại sách") || userMsg.equals("chuyên ngành")) {
                responseBody.put("reply", "Thư viện hiện đang có rất nhiều thể loại đa dạng phục vụ học tập và nghiên cứu, tiêu biểu như: Công nghệ Thông tin, Kinh tế, Quản trị Kinh doanh, Thiết kế Đồ họa, Ngoại ngữ, Tiểu thuyết và sách Kỹ năng mềm. Bạn đang muốn tìm sách thuộc chuyên ngành nào?");
                return ResponseEntity.ok(responseBody);
            }
            if (userMsg.contains("luật mượn") || userMsg.contains("quy định mượn") || userMsg.contains("cách mượn sách") || userMsg.contains("quy định thư viện")) {
                responseBody.put("reply", "Thư viện cho phép sinh viên mượn tối đa 3 cuốn sách trong 14 ngày. Nếu trả sách trễ hạn sẽ bị phạt 5,000đ/ngày. Bạn có thể gia hạn trực tuyến thêm 1 lần (14 ngày) trên website nếu cuốn sách đó chưa có ai đặt trước nhé.");
                return ResponseEntity.ok(responseBody);
            }
            if (userMsg.contains("tôi muốn hỏi về sách có trong thư viện") || userMsg.contains("hỏi về sách có trong thư viện") || userMsg.equals("sách trong thư viện")) {
                responseBody.put("reply", "Thư viện hiện có hàng ngàn đầu sách phong phú đa dạng các lĩnh vực. Bạn có thể cho mình biết cụ thể bạn đang quan tâm đến cuốn sách nào, chủ đề hay môn học nào không?");
                return ResponseEntity.ok(responseBody);
            }
            if (userMsg.contains("review") || userMsg.contains("sách hay") || userMsg.contains("gợi ý ngẫu nhiên") || userMsg.contains("một cuốn sách")) {
                List<Book> allBooksList = bookRepository.findAll();
                List<Book> availableBooks = allBooksList.stream().filter(b -> b.getAvailableCount() > 0).collect(Collectors.toList());
                if (availableBooks.isEmpty()) availableBooks = allBooksList; // Fallback
                
                if (!availableBooks.isEmpty()) {
                    Book randomBook = availableBooks.get(new java.util.Random().nextInt(availableBooks.size()));
                    String shelfInfo = (randomBook.getShelfCode() != null && !randomBook.getShelfCode().trim().isEmpty() && !"null".equalsIgnoreCase(randomBook.getShelfCode())) 
                                       ? " tại kệ " + randomBook.getShelfCode() : "";
                    
                    String[] templates = {
                        "Chắc chắn rồi! Mình gợi ý cho bạn cuốn sách: **%s**. \nCuốn sách này cực kỳ thú vị và rất được sinh viên ưa chuộng! Hiện tại thư viện đang còn %d cuốn sẵn sàng để mượn%s.",
                        "Mình nghĩ bạn sẽ rất thích cuốn **%s**. \nĐây là một trong những cuốn sách đáng đọc nhất! Thư viện đang có sẵn %d cuốn%s.",
                        "Một gợi ý tuyệt vời cho bạn: **%s**. \nĐừng bỏ lỡ cơ hội đọc cuốn này nhé! Đang còn %d cuốn có thể mượn%s."
                    };
                    String template = templates[new java.util.Random().nextInt(templates.length)];
                    String reply = String.format(template, randomBook.getTitle(), randomBook.getAvailableCount(), shelfInfo);
                    
                    responseBody.put("reply", reply);
                    return ResponseEntity.ok(responseBody);
                }
            }

            // 2. Book search logic
            List<Book> allBooks = bookRepository.findAll();
            
            // Try to find books where the title or category is mentioned in the user's message
            List<Book> matchedBooks = allBooks.stream()
                .filter(b -> (b.getTitle() != null && userMsg.contains(b.getTitle().toLowerCase())) || 
                             (b.getCategories() != null && b.getCategories().stream().anyMatch(c -> c.getCategoryName() != null && userMsg.contains(c.getCategoryName().toLowerCase()))))
                .collect(Collectors.toList());
            
            String searchStr = "";
            //Hung: Bóc tách các từ thừa để trích xuất ra đúng từ khóa cần tìm kiếm
            // If none found, try to extract the book name by removing question keywords
            if (matchedBooks.isEmpty()) {
                 searchStr = userMsg
                         .replace("tôi muốn hỏi", "").replace("tìm sách", "")
                         .replace("thông tin sách", "").replace("về sách", "").replace("sách", "")
                         .replace("thông tin", "").replace("về", "")
                         .replace("ưu nhược điểm", "").replace("ưu điểm", "").replace("nhược điểm", "")
                         .replace("tính khả thi", "").replace("phù hợp với ai", "").replace("phù hợp", "")
                         .replace("đánh giá", "").replace("review", "").replace("của", "").replace("có", "")
                         .replace("là gì", "").replace("không", "").replace("tác giả", "").replace("ai", "")
                         .replace("mượn cuốn", "").replace("cuốn", "")
                         .replace("những cuốn", "").replace("thể loại", "")
                         .replace("bạn gợi ý", "").replace("vài", "").replace("được không", "").replace("bạn", "")
                         .replace("gợi ý", "").replace("ngành", "").replace("chuyên ngành", "").replace("cho mình", "")
                         .replace("giúp", "").replace("có thể", "").replace("nào", "").replace("?", "")
                         .trim();
                 
                 if (searchStr.length() > 2) {
                     List<String> keywords = new ArrayList<>();
                     keywords.add(searchStr);
                     
                     //Hung: Bộ Từ điển đồng nghĩa (Smart Aliases) để tự động ánh xạ lĩnh vực
                     // Smart aliases (AI giả lập)
                     if (searchStr.contains("lập trình") || searchStr.contains("cntt") || searchStr.contains("it") || searchStr.contains("code")) {
                         keywords.add("công nghệ thông tin");
                         keywords.add("python");
                         keywords.add("java");
                         keywords.add("c++");
                         keywords.add("html");
                         keywords.add("web");
                     }
                     if (searchStr.contains("ngoại ngữ") || searchStr.contains("tiếng anh") || searchStr.contains("học tiếng")) {
                         keywords.add("english");
                         keywords.add("toeic");
                         keywords.add("ielts");
                         keywords.add("ngôn ngữ");
                     }
                     if (searchStr.contains("kinh doanh") || searchStr.contains("bán hàng") || searchStr.contains("làm giàu")) {
                         keywords.add("kinh tế");
                         keywords.add("quản trị");
                         keywords.add("marketing");
                     }
                     if (searchStr.contains("se") || searchStr.contains("phần mềm") || searchStr.contains("software")) {
                         keywords.add("công nghệ thông tin");
                         keywords.add("software");
                         keywords.add("engineering");
                         keywords.add("kỹ nghệ");
                     }

                     //Hung: Quét từ khóa mở rộng trên cả Tiêu đề, Thể loại và Tên Tác giả
                     matchedBooks = allBooks.stream()
                        .filter(b -> {
                            for (String kw : keywords) {
                                if ((b.getTitle() != null && b.getTitle().toLowerCase().contains(kw)) ||
                                    (b.getCategories() != null && b.getCategories().stream().anyMatch(c -> c.getCategoryName() != null && c.getCategoryName().toLowerCase().contains(kw))) ||
                                    (b.getAuthors() != null && b.getAuthors().stream().anyMatch(a -> a.getAuthorName() != null && a.getAuthorName().toLowerCase().contains(kw)))) {
                                    return true;
                                }
                            }
                            return false;
                        })
                        .collect(Collectors.toList());
                 }
            }

            if (!matchedBooks.isEmpty()) {
                List<Book> displayBooks = matchedBooks.stream().limit(3).collect(Collectors.toList());
                StringBuilder reply = new StringBuilder("Mình tìm thấy " + matchedBooks.size() + " cuốn sách phù hợp. Dưới đây là một số gợi ý nổi bật:\n\n");
                for (Book b : displayBooks) {
                    reply.append("- **").append(b.getTitle()).append("**");
                    if (b.getIsbn() != null) reply.append(" (ISBN: ").append(b.getIsbn()).append(")\n");
                    else reply.append("\n");
                    
                    if (b.getAuthors() != null && !b.getAuthors().isEmpty()) {
                        String authors = b.getAuthors().stream().map(a -> a.getAuthorName()).collect(Collectors.joining(", "));
                        reply.append("  Tác giả: ").append(authors).append("\n");
                    }
                    if (b.getPublisher() != null) {
                        reply.append("  Nhà XB: ").append(b.getPublisher().getPublisherName()).append("\n");
                    }
                    if (b.getPublishYear() != null) {
                        reply.append("  Năm XB: ").append(b.getPublishYear()).append("\n");
                    }
                    if (b.getLanguage() != null) {
                        reply.append("  Ngôn ngữ: ").append(b.getLanguage()).append("\n");
                    }
                    if (b.getEdition() != null && !b.getEdition().isEmpty()) {
                        reply.append("  Phiên bản: ").append(b.getEdition()).append("\n");
                    }
                    reply.append("  Tình trạng: Hiện có ").append(b.getAvailableCount()).append(" / ").append(b.getTotalCount()).append(" cuốn\n");

                    if (b.getDescription() != null && !b.getDescription().isEmpty()) {
                        reply.append("  Mô tả: ").append(b.getDescription()).append("\n");
                    }
                    if (b.getShelfCode() != null && !b.getShelfCode().isEmpty()) {
                        reply.append("  Vị trí: ").append(b.getShelfCode()).append("\n");
                    }
                    
                    reply.append("\n");
                }
                responseBody.put("reply", reply.toString());
            } else {
                if (searchStr.length() > 2) {
                    responseBody.put("reply", "Xin lỗi bạn, hiện tại mình không tìm thấy cuốn sách nào liên quan đến từ khóa '" + searchStr + "'. Bạn có thể thử tìm bằng một từ khóa khác hoặc kiểm tra lại chính tả giúp mình nhé!");
                } else {
                    responseBody.put("reply", "Xin lỗi bạn, mình chưa rõ bạn muốn tìm sách gì. Bạn có thể nói cụ thể hơn tên sách, tác giả hoặc chủ đề bạn quan tâm được không?");
                }
            }
            return ResponseEntity.ok(responseBody);
        }

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
            System.out.println("Gemini API Error (" + e.getMessage() + ") - Falling back to hardcoded logic.");
            String userMsg = request.getMessage().toLowerCase();
            
            // 1. Hardcoded generic library questions
            if (userMsg.contains("giờ mở cửa") || userMsg.contains("thời gian làm việc") || userMsg.contains("mở cửa")) {
                responseBody.put("reply", "Thư viện mở cửa từ 7h30 sáng đến 21h30 tối, từ Thứ 2 đến Chủ nhật hàng tuần bạn nhé.");
                return ResponseEntity.ok(responseBody);
            }
            if (userMsg.contains("mượn được bao nhiêu cuốn") || userMsg.contains("số lượng sách") || userMsg.contains("mượn tối đa")) {
                responseBody.put("reply", "Hiện tại quy định của thư viện là Sinh viên có thể mượn tối đa 3 cuốn sách cùng lúc.");
                return ResponseEntity.ok(responseBody);
            }
            if (userMsg.contains("mượn bao lâu") || userMsg.contains("thời gian mượn") || userMsg.contains("gia hạn")) {
                responseBody.put("reply", "Bạn có thể mượn sách tối đa trong 14 ngày. Nếu chưa đọc xong, bạn hoàn toàn có thể gia hạn thêm 1 lần thông qua website, miễn là sách đó không có ai đang đặt trước.");
                return ResponseEntity.ok(responseBody);
            }
            if (userMsg.contains("bị phạt") || userMsg.contains("mất sách") || userMsg.contains("trả trễ") || userMsg.contains("quá hạn")) {
                responseBody.put("reply", "Nếu bạn trả sách trễ hạn, hệ thống sẽ tự động tính phí phạt là 5,000đ/ngày. Nếu làm hỏng hoặc mất sách, bạn sẽ phải bồi thường theo quy định của thư viện.");
                return ResponseEntity.ok(responseBody);
            }
            if (userMsg.contains("xin chào") || userMsg.contains("hello") || userMsg.contains("hi") || userMsg.contains("chào bạn")) {
                responseBody.put("reply", "Chào bạn! Mình là FLMS-Bot, trợ lý ảo của thư viện. Mình có thể giúp gì cho bạn hôm nay? (Ví dụ: Tìm sách, hỏi về giờ mở cửa, quy định mượn trả...)");
                return ResponseEntity.ok(responseBody);
            }
            if (userMsg.contains("những loại sách nào") || userMsg.contains("những thể loại nào") || userMsg.contains("danh mục sách") || userMsg.contains("thể loại sách có trong thư viện") || userMsg.contains("các thể loại sách") || userMsg.equals("thể loại") || userMsg.equals("loại sách") || userMsg.equals("chuyên ngành")) {
                responseBody.put("reply", "Thư viện FPT hiện có rất nhiều thể loại đa dạng! \n📚 Sách chuyên ngành: Công nghệ thông tin (SE, AI, IoT), Kinh tế (Quản trị kinh doanh, Marketing, Tài chính), Ngôn ngữ (Anh, Nhật, Hàn, Trung). \n🎨 Sách kỹ năng: Kỹ năng mềm, Phát triển bản thân, Tư duy logic. \nGiải trí: Tiểu thuyết, Truyện tranh... Bạn đang quan tâm đến lĩnh vực nào nhỉ?");
                return ResponseEntity.ok(responseBody);
            }
            
            if (userMsg.contains("review") || userMsg.contains("sách hay") || userMsg.contains("gợi ý ngẫu nhiên") || userMsg.contains("một cuốn sách")) {
                List<Book> allBooksList = bookRepository.findAll();
                List<Book> availableBooks = allBooksList.stream().filter(b -> b.getAvailableCount() > 0).collect(Collectors.toList());
                if (availableBooks.isEmpty()) availableBooks = allBooksList; // Fallback
                
                if (!availableBooks.isEmpty()) {
                    Book randomBook = availableBooks.get(new java.util.Random().nextInt(availableBooks.size()));
                    String shelfInfo = (randomBook.getShelfCode() != null && !randomBook.getShelfCode().trim().isEmpty() && !"null".equalsIgnoreCase(randomBook.getShelfCode())) 
                                       ? " tại kệ " + randomBook.getShelfCode() : "";
                    
                    String[] templates = {
                        "Chắc chắn rồi! Mình gợi ý cho bạn cuốn sách: **%s**. \nCuốn sách này cực kỳ thú vị và rất được sinh viên ưa chuộng! Hiện tại thư viện đang còn %d cuốn sẵn sàng để mượn%s.",
                        "Mình nghĩ bạn sẽ rất thích cuốn **%s**. \nĐây là một trong những cuốn sách đáng đọc nhất! Thư viện đang có sẵn %d cuốn%s.",
                        "Một gợi ý tuyệt vời cho bạn: **%s**. \nĐừng bỏ lỡ cơ hội đọc cuốn này nhé! Đang còn %d cuốn có thể mượn%s."
                    };
                    String template = templates[new java.util.Random().nextInt(templates.length)];
                    String reply = String.format(template, randomBook.getTitle(), randomBook.getAvailableCount(), shelfInfo);
                    
                    responseBody.put("reply", reply);
                    return ResponseEntity.ok(responseBody);
                }
            }
            
            // 2. TÌM KIẾM ĐỘNG BẰNG SQL NẾU KHÔNG TRÚNG CÂU HỎI CHUNG
            List<Book> allBooks = bookRepository.findAll();
            List<Book> matchedBooks = allBooks.stream()
                .filter(b -> (b.getTitle() != null && userMsg.contains(b.getTitle().toLowerCase())) || 
                             (b.getCategories() != null && b.getCategories().stream().anyMatch(c -> c.getCategoryName() != null && userMsg.contains(c.getCategoryName().toLowerCase()))))
                .collect(Collectors.toList());
            
            String searchStr = "";
            // Bóc tách các từ thừa để trích xuất ra đúng từ khóa cần tìm kiếm
            if (matchedBooks.isEmpty()) {
                 searchStr = userMsg
                         .replace("tôi muốn hỏi", "").replace("tìm sách", "")
                         .replace("thông tin sách", "").replace("về sách", "").replace("sách", "")
                         .replace("thông tin", "").replace("về", "")
                         .replace("ưu nhược điểm", "").replace("ưu điểm", "").replace("nhược điểm", "")
                         .replace("tính khả thi", "").replace("phù hợp với ai", "").replace("phù hợp", "")
                         .replace("đánh giá", "").replace("review", "").replace("của", "").replace("có", "")
                         .replace("là gì", "").replace("không", "").replace("tác giả", "").replace("ai", "")
                         .replace("mượn cuốn", "").replace("cuốn", "")
                         .replace("những cuốn", "").replace("thể loại", "")
                         .replace("bạn gợi ý", "").replace("vài", "").replace("được không", "").replace("bạn", "")
                         .replace("gợi ý", "").replace("ngành", "").replace("chuyên ngành", "").replace("cho mình", "")
                         .replace("giúp", "").replace("có thể", "").replace("nào", "").replace("?", "")
                         .trim();
                 
                 if (searchStr.length() > 2) {
                     List<String> keywords = new ArrayList<>();
                     keywords.add(searchStr);
                     
                     // Bộ Từ điển đồng nghĩa (Smart Aliases) để tự động ánh xạ lĩnh vực
                     if (searchStr.contains("lập trình") || searchStr.contains("cntt") || searchStr.contains("it") || searchStr.contains("code")) {
                         keywords.add("công nghệ thông tin");
                         keywords.add("python");
                         keywords.add("java");
                         keywords.add("c++");
                         keywords.add("html");
                         keywords.add("web");
                     }
                     if (searchStr.contains("ngoại ngữ") || searchStr.contains("tiếng anh") || searchStr.contains("học tiếng")) {
                         keywords.add("english");
                         keywords.add("toeic");
                         keywords.add("ielts");
                         keywords.add("ngôn ngữ");
                     }
                     if (searchStr.contains("kinh doanh") || searchStr.contains("bán hàng") || searchStr.contains("làm giàu")) {
                         keywords.add("kinh tế");
                         keywords.add("quản trị");
                         keywords.add("marketing");
                     }
                     if (searchStr.contains("se") || searchStr.contains("phần mềm") || searchStr.contains("software")) {
                         keywords.add("công nghệ thông tin");
                         keywords.add("software");
                         keywords.add("engineering");
                         keywords.add("kỹ nghệ");
                     }

                     // Quét từ khóa mở rộng trên cả Tiêu đề, Thể loại và Tên Tác giả
                     matchedBooks = allBooks.stream()
                        .filter(b -> {
                            for (String kw : keywords) {
                                if ((b.getTitle() != null && b.getTitle().toLowerCase().contains(kw)) ||
                                    (b.getCategories() != null && b.getCategories().stream().anyMatch(c -> c.getCategoryName() != null && c.getCategoryName().toLowerCase().contains(kw))) ||
                                    (b.getAuthors() != null && b.getAuthors().stream().anyMatch(a -> a.getAuthorName() != null && a.getAuthorName().toLowerCase().contains(kw)))) {
                                    return true;
                                }
                            }
                            return false;
                        })
                        .collect(Collectors.toList());
                 }
            }

            if (!matchedBooks.isEmpty()) {
                List<Book> displayBooks = matchedBooks.stream().limit(3).collect(Collectors.toList());
                StringBuilder reply = new StringBuilder("Mình tìm thấy " + matchedBooks.size() + " cuốn sách phù hợp. Dưới đây là một số gợi ý nổi bật:\n\n");
                for (Book b : displayBooks) {
                    reply.append("- **").append(b.getTitle()).append("**");
                    if (b.getIsbn() != null) reply.append(" (ISBN: ").append(b.getIsbn()).append(")\n");
                    else reply.append("\n");
                    
                    if (b.getAuthors() != null && !b.getAuthors().isEmpty()) {
                        String authors = b.getAuthors().stream().map(a -> a.getAuthorName()).collect(Collectors.joining(", "));
                        reply.append("  Tác giả: ").append(authors).append("\n");
                    }
                    if (b.getPublisher() != null) {
                        reply.append("  Nhà XB: ").append(b.getPublisher().getPublisherName()).append("\n");
                    }
                    if (b.getPublishYear() != null) {
                        reply.append("  Năm XB: ").append(b.getPublishYear()).append("\n");
                    }
                    if (b.getLanguage() != null) {
                        reply.append("  Ngôn ngữ: ").append(b.getLanguage()).append("\n");
                    }
                    if (b.getEdition() != null && !b.getEdition().isEmpty()) {
                        reply.append("  Phiên bản: ").append(b.getEdition()).append("\n");
                    }
                    reply.append("  Tình trạng: Hiện có ").append(b.getAvailableCount()).append(" / ").append(b.getTotalCount()).append(" cuốn\n");

                    if (b.getDescription() != null && !b.getDescription().isEmpty()) {
                        reply.append("  Mô tả: ").append(b.getDescription()).append("\n");
                    }
                    if (b.getShelfCode() != null && !b.getShelfCode().isEmpty()) {
                        reply.append("  Vị trí: ").append(b.getShelfCode()).append("\n");
                    }
                    
                    reply.append("\n");
                }
                responseBody.put("reply", reply.toString());
            } else {
                if (searchStr.length() > 2) {
                    responseBody.put("reply", "Xin lỗi bạn, hiện tại mình không tìm thấy cuốn sách nào liên quan đến từ khóa '" + searchStr + "'. Bạn có thể thử tìm bằng một từ khóa khác hoặc kiểm tra lại chính tả giúp mình nhé!");
                } else {
                    responseBody.put("reply", "Xin lỗi bạn, mình chưa rõ bạn muốn tìm sách gì. Bạn có thể nói cụ thể hơn tên sách, tác giả hoặc chủ đề bạn quan tâm được không?");
                }
            }
            return ResponseEntity.ok(responseBody);
        }
    }
} 
