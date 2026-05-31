# TÀI LIỆU ĐẶC TẢ USE CASE: UCR06 – RESERVE & CANCEL BOOK ONLINE
**Use Case ID:** UCR06  
**Use Case Name:** Reserve & Cancel Book Online (Đặt giữ chỗ và Hủy đặt chỗ trực tuyến)  
**Version:** 2.0  
**Trạng thái:** Đã cập nhật tính năng Hủy hàng chờ (Cancel Waitlist)

---

## 1. Tóm tắt (Brief Description)
Use Case này cho phép Bạn đọc (Patron) đã đăng nhập vào hệ thống Thư viện FPT University thực hiện đặt giữ trước một bản sách vật lý (Book Copy) còn trống tại một cơ sở (Campus) xác định để đến nhận trong thời hạn quy định (mặc định là 24 giờ). 
- Nếu cơ sở được chọn hết bản sách sẵn sàng, Bạn đọc có thể lựa chọn đăng ký xếp hàng vào **Danh sách chờ (Waitlist)**.
- Bạn đọc có thể chủ động **Hủy đặt giữ chỗ (Cancel Reservation)** đối với các đơn đang chờ nhận sách hoặc **Hủy đăng ký xếp hàng chờ (Cancel Waitlist)** nếu không còn nhu cầu mượn sách.

---

## 2. Tác nhân (Actors)
* **Tác nhân chính:** Bạn đọc (Patron)
* **Tác nhân hỗ trợ:** Hệ thống Email (Email Service)

---

## 3. Tiền điều kiện (Preconditions)
1. Bạn đọc đã đăng nhập thành công vào hệ thống.
2. Tài khoản bạn đọc tồn tại trên hệ thống.

---

## 4. Hậu điều kiện (Postconditions)
* **Trường hợp đặt chỗ thành công (Normal Flow):**
  - Bản sách vật lý được chọn chuyển trạng thái sang `Reserved`.
  - Một đơn đặt giữ chỗ (`Reservation`) được tạo mới với trạng thái `Holding` và thời hạn hết hạn sau 24 giờ.
  - Hệ thống ghi nhận thông báo nội bộ và gửi email xác nhận đặt chỗ kèm thời hạn nhận sách cho Bạn đọc.
* **Trường hợp đăng ký xếp hàng chờ thành công (Exc 3/4):**
  - Một bản ghi `Waitlist` được tạo mới với trạng thái `Waiting`.
  - Hệ thống ghi nhận thông báo nội bộ và gửi email xác nhận xếp hàng thành công kèm số thứ tự hiện tại.
* **Trường hợp hủy đặt chỗ thành công (Alt 1):**
  - Trạng thái đơn đặt chỗ (`Reservation`) chuyển thành `Cancelled`.
  - Bản sách vật lý liên kết được trả về trạng thái tự do `Available` để phục vụ bạn đọc khác.
  - Hệ thống gửi email thông báo hủy đặt sách thành công.
* **Trường hợp hủy xếp hàng chờ thành công (Alt 2):**
  - Trạng thái hàng chờ (`Waitlist`) chuyển thành `Cancelled`.
  - Hệ thống gửi email thông báo đã hủy xếp hàng chờ cho Bạn đọc.

---

## 5. Luồng sự kiện (Flow of Events)

### 5.1. Luồng chính (Basic Flow - Normal Flow)
1. Bạn đọc truy cập vào trang Chi tiết sách (**UCG02**) hoặc trang Tìm kiếm sách (**UCG01**).
2. Hệ thống kiểm tra trạng thái tài khoản của Bạn đọc:
   - Trạng thái phải là `Active` (không bị khóa/tạm ngưng) **[Xem Exc 1a]**.
   - Tài khoản không bị khóa chức năng mượn sách do nợ phạt quá hạn `BorrowingLocked` = `False` **[Xem Exc 1b]**.
3. Hệ thống kiểm tra số lượng đơn đặt giữ chỗ đang hoạt động (`Holding`) của Bạn đọc:
   - Số đơn hiện tại phải nhỏ hơn giới hạn tối đa cho phép (mặc định là 3 đơn) **[Xem Exc 2]**.
4. Bạn đọc chọn cơ sở (Campus) muốn nhận sách và bấm **"Đặt giữ chỗ ngay"**.
5. Hệ thống tìm kiếm một bản sách vật lý (Book Copy) của đầu sách đó tại cơ sở đã chọn có trạng thái `Available` **[Xem Exc 3]**.
6. Hệ thống thực hiện khóa bản sách (Pessimistic Lock) để tránh xung đột đồng thời **[Xem Exc 4]**.
7. Hệ thống chuyển trạng thái của bản sách từ `Available` sang `Reserved`.
8. Hệ thống tạo đơn đặt chỗ (`Reservation`) mới:
   - Trạng thái đơn: `Holding`.
   - Thời gian đặt: Thời gian hiện tại.
   - Thời gian hết hạn: Thời gian hiện tại + 24 giờ.
9. Hệ thống lưu đơn vào cơ sở dữ liệu.
10. Hệ thống tạo thông báo nội bộ (`Notification`) cho Bạn đọc.
11. Hệ thống kích hoạt gửi email xác nhận đặt chỗ thành công chứa thông tin sách, cơ sở nhận và thời hạn hết hạn.
12. Hệ thống chuyển Bạn đọc về trang Quản lý đặt chỗ cá nhân kèm thông báo thành công.

---

### 5.2. Các luồng thay thế (Alternative Flows)

#### Alt 1: Bạn đọc hủy đơn đặt giữ chỗ (Cancel Reservation)
1. Bạn đọc truy cập trang **Đặt chỗ của tôi** (`/reservations`).
2. Bạn đọc tìm đơn đặt chỗ muốn hủy (chỉ áp dụng cho đơn có trạng thái `Holding`) và nhấn **"Hủy đặt chỗ"**.
3. Hệ thống hiển thị modal yêu cầu xác nhận hủy đơn.
4. Bạn đọc nhấn **"Đồng ý hủy"**.
5. Hệ thống thực hiện:
   - Cập nhật trạng thái đơn đặt chỗ thành `Cancelled`.
   - Tìm bản sách vật lý gắn liền với đơn và chuyển trạng thái bản sách từ `Reserved` sang `Available`.
   - Tạo thông báo nội bộ báo hủy đơn thành công.
   - Gửi email xác nhận hủy đặt chỗ về hòm thư của Bạn đọc.
6. Hệ thống tải lại trang cá nhân và hiển thị thông báo hủy thành công.

#### Alt 2: Bạn đọc hủy đăng ký xếp hàng chờ (Cancel Waitlist) - **[MỚI]**
1. Bạn đọc truy cập trang **Đặt chỗ của tôi** (`/reservations`).
2. Bạn đọc tìm bản ghi hàng chờ muốn hủy tại mục **Danh sách xếp hàng chờ** (chỉ áp dụng cho hàng chờ có trạng thái `Waiting` hoặc `Notified`) và nhấn **"Hủy hàng chờ"**.
3. Hệ thống hiển thị modal yêu cầu xác nhận hủy đăng ký xếp hàng chờ.
4. Bạn đọc nhấn **"Đồng ý hủy"**.
5. Hệ thống thực hiện:
   - Cập nhật trạng thái của bản ghi `Waitlist` thành `Cancelled`.
   - Tạo thông báo nội bộ báo hủy hàng chờ thành công.
   - Gửi email thông báo hủy xếp hàng chờ về hòm thư của Bạn đọc.
6. Hệ thống tải lại trang cá nhân, cập nhật giao diện (trạng thái chuyển thành `Đã hủy đăng ký` và ẩn nút hủy), đồng thời hiển thị thông báo thành công.

---

### 5.3. Các luồng ngoại lệ (Exception Flows)

#### Exc 1: Trạng thái tài khoản không hợp lệ
* **Exc 1a (Tài khoản không Active):** 
  - Tại bước 2 luồng chính, nếu trạng thái tài khoản là `Inactive` hoặc `Suspended`, hệ thống từ chối giao dịch, hiển thị thông báo: *"Giao dịch thất bại! Tài khoản của bạn đang ở trạng thái [Trạng thái]. Vui lòng liên hệ thủ thư để được hỗ trợ."* và dừng Use Case.
* **Exc 1b (Tài khoản bị khóa do nợ phạt quá hạn):**
  - Tại bước 2 luồng chính, nếu tài khoản bị khóa chức năng đặt/mượn (`borrowingLocked` = `True`), hệ thống từ chối giao dịch, hiển thị thông báo: *"Giao dịch thất bại! Tài khoản của bạn đang bị khóa do có khoản phạt chưa thanh toán. Vui lòng hoàn tất nộp phạt trước khi đặt sách."* và dừng Use Case.

#### Exc 2: Vượt quá giới hạn đặt chỗ cho phép
* Tại bước 3 luồng chính, nếu số lượng đơn đặt giữ chỗ đang ở trạng thái `Holding` của Bạn đọc đạt từ 3 đơn trở lên:
  - Hệ thống từ chối giao dịch, hiển thị thông báo lỗi: *"Thao tác bị từ chối! Bạn đã có [X]/3 đơn đặt giữ chỗ đang hoạt động. Vui lòng hủy bớt đơn cũ trước khi tiếp tục."* và dừng Use Case.

#### Exc 3: Cơ sở được chọn hết bản sách vật lý sẵn sàng
* Tại bước 5 luồng chính, nếu hệ thống không tìm thấy bất kỳ bản sách nào của đầu sách đó tại cơ sở được chọn có trạng thái `Available`:
  - Hệ thống chuyển hướng Bạn đọc sang giao diện **Đăng ký xếp hàng chờ**.
  - Bạn đọc xác nhận muốn đăng ký xếp hàng chờ.
  - Hệ thống kiểm tra xem Bạn đọc đã có hàng chờ đang hoạt động (`Waiting` hoặc `Notified`) cho đầu sách này tại cơ sở này chưa.
    - *Nếu đã tồn tại*: Báo lỗi trùng lặp và dừng Use Case.
    - *Nếu chưa*: Tạo bản ghi `Waitlist` mới với trạng thái `Waiting`, ghi nhận thông báo, gửi email xác nhận kèm số thứ tự (số người đang đợi + 1), chuyển Bạn đọc về trang cá nhân và hiển thị thông báo đăng ký hàng chờ thành công.

#### Exc 4: Xung đột đồng thời (Race Condition)
* Tại bước 6 luồng chính, nếu bản sách vật lý vừa được tìm thấy đã bị thay đổi trạng thái sang `Reserved` hoặc `Borrowed` bởi một tiến trình song song của bạn đọc khác ngay trước khi thực hiện khóa:
  - Hệ thống ghi nhận cảnh báo xung đột (Log).
  - Hệ thống tự động chuyển đổi luồng xử lý sang **Exc 3** (Gợi ý Bạn đọc đăng ký xếp hàng chờ do sách đã bị đặt).

---

## 6. Quy tắc nghiệp vụ (Business Rules)
1. **Thời hạn giữ sách (Hold Expiry):** Đơn đặt chỗ thành công có thời hạn mặc định là 24 giờ kể từ thời điểm đặt. Nếu quá thời gian này mà Bạn đọc không đến nhận sách, hệ thống tự động quét và chuyển trạng thái đơn sang `Expired`, đồng thời giải phóng bản sách vật lý về `Available`.
2. **Giới hạn đặt giữ chỗ:** Mỗi Bạn đọc chỉ được phép sở hữu tối đa **3 đơn đặt giữ chỗ đang hoạt động (Holding)** cùng một lúc. Giới hạn này không tính các đơn đã hoàn thành (`Completed`), đã hủy (`Cancelled`) hoặc đã hết hạn (`Expired`). Không giới hạn số lượng đăng ký trong hàng xếp hàng chờ (`Waitlist`).
3. **Tính toán số thứ tự hàng chờ (Waitlist Position):** Số thứ tự của Bạn đọc trong hàng chờ được tính dựa trên số lượng các bản ghi hàng chờ khác có trạng thái `Waiting` hoặc `Notified` của cùng đầu sách đó tại cùng cơ sở đó và có thời gian đăng ký (`requestedAt`) sớm hơn thời gian đăng ký của Bạn đọc.
