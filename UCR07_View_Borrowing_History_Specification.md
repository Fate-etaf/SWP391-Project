# TÀI LIỆU ĐẶC TẢ USE CASE: UCR07 – VIEW BORROWING HISTORY
**Use Case ID:** UCR07  
**Use Case Name:** View Borrowing History (Xem Lịch sử Mượn trả Sách)  
**Version:** 1.0  
**Trạng thái:** Đã hoàn thành và triển khai tích hợp  

---

## 1. Tóm tắt (Brief Description)
Use Case này cho phép Bạn đọc (Patron - Sinh viên, Giảng viên) đã đăng nhập vào hệ thống Thư viện FPT University thực hiện xem lại toàn bộ lịch sử mượn sách giấy của mình tại quầy. Hệ thống cung cấp giao diện trực quan hiển thị thông tin sách, thời hạn trả, ngày trả thực tế, trạng thái xử lý hiện tại và bộ lọc động để quản lý dễ dàng.

---

## 2. Tác nhân (Actors)
* **Tác nhân chính:** Bạn đọc (Patron)

---

## 3. Tiền điều kiện (Preconditions)
1. Bạn đọc đã đăng nhập thành công vào hệ thống.
2. Tài khoản bạn đọc tồn tại trên hệ thống.

---

## 4. Hậu điều kiện (Postconditions)
* Hệ thống hiển thị danh sách tất cả các lượt mượn sách của Bạn đọc kèm theo các thông tin chi tiết: Ảnh bìa, Tên sách, Mã bản sao (Copy ID), Ngày mượn, Hạn trả, Ngày trả thực tế (nếu có), Cơ sở mượn (Campus) và Trạng thái phân loại.

---

## 5. Luồng sự kiện (Flow of Events)

### 5.1. Luồng chính (Basic Flow - Normal Flow)
1. Bạn đọc nhấn vào liên kết **"Lịch sử mượn trả"** trên thanh điều hướng đầu trang (Header).
2. Hệ thống kiểm tra Session và lấy định danh của Bạn đọc (`loggedInUserId`).
3. Hệ thống gửi yêu cầu truy vấn đến Cơ sở dữ liệu để tìm toàn bộ các bản ghi chi tiết phiếu mượn (`BorrowTicketDetail`) liên kết với mã Bạn đọc đó, sắp xếp theo thời gian tạo (`CreatedAt`) giảm dần (mới nhất lên đầu).
4. Đối với mỗi bản ghi chi tiết phiếu mượn, hệ thống tự động xác định trạng thái hiển thị theo quy tắc nghiệp vụ:
   - Nếu trạng thái bản sao ghi nhận là mất mát (`Lost`) → Hiển thị trạng thái **Bị mất**.
   - Nếu trạng thái bản sao ghi nhận là hư hỏng nặng (`Damaged`) → Hiển thị trạng thái **Bị hỏng**.
   - Nếu ngày trả thực tế đã được ghi nhận (`returnDate != null`) → Hiển thị trạng thái **Đã trả**.
   - Nếu chưa trả (`returnDate == null`) và hạn trả trước thời gian hiện tại (`dueDate < LocalDateTime.now()`) → Hiển thị trạng thái **Quá hạn**.
   - Các trường hợp còn lại (chưa trả và chưa quá hạn) → Hiển thị trạng thái **Đang mượn**.
5. Hệ thống kết xuất dữ liệu và hiển thị lên màn hình dưới dạng lưới thẻ (Grid Cards) trực quan.

---

### 5.2. Các luồng thay thế (Alternative Flows)

#### Alt 1: Lọc danh sách mượn trả theo trạng thái (Filter by Status)
1. Trên trang Lịch sử mượn trả, Bạn đọc nhấn chọn một trong các tab lọc: **Tất cả**, **Đang mượn**, **Đã trả**, **Quá hạn**, hoặc **Bị mất / Bị hỏng**.
2. Hệ thống sử dụng JavaScript phía Client quét qua danh sách hiển thị và chỉ giữ lại những thẻ sách có trạng thái tương ứng với tab được chọn (không cần tải lại trang).

#### Alt 2: Tìm kiếm theo tiêu đề sách (Search within History)
1. Bạn đọc nhập từ khóa vào ô tìm kiếm tiêu đề sách trên trang lịch sử.
2. Hệ thống tự động lọc nhanh và chỉ hiển thị các bản ghi mượn sách có tên sách chứa từ khóa tìm kiếm (không phân biệt chữ hoa chữ thường).

---

### 5.3. Các luồng ngoại lệ (Exception Flows)

#### Exc 1: Bạn đọc chưa có lịch sử mượn sách
* Tại bước 3 luồng chính, nếu hệ thống không tìm thấy bất kỳ bản ghi mượn sách nào của Bạn đọc:
  - Hệ thống hiển thị giao diện trống kèm hình ảnh minh họa thân thiện và thông báo: *"Bạn chưa mượn cuốn sách nào. Hãy khám phá kho sách và mượn cuốn sách đầu tiên nhé!"* kèm theo nút bấm dẫn tới trang Tìm kiếm sách.

---

## 6. Quy tắc nghiệp vụ (Business Rules)
1. **Phân loại trạng thái động (Dynamic Status Resolution):** Trạng thái của cuốn sách trong lịch sử không cố định hoàn toàn trong DB mà được tính toán động tại thời điểm truy cập dựa trên sự kết hợp giữa Ngày trả thực tế (`ReturnDate`), Hạn trả (`DueDate`) so với ngày giờ hiện tại, và trạng thái hư hỏng/mất mát từ bảng chi tiết.
2. **Quyền riêng tư:** Bạn đọc chỉ được xem lịch sử mượn trả của chính tài khoản mình đang đăng nhập. Hệ thống chặn đứng mọi hành vi truy cập trái phép bằng cách đọc trực tiếp ID từ Session thay vì nhận ID qua tham số URL (tránh lỗi ID-idor).
