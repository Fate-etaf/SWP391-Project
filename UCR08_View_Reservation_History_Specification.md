# TÀI LIỆU ĐẶC TẢ USE CASE: UCR08 – VIEW RESERVATION & WAITLIST HISTORY
**Use Case ID:** UCR08  
**Use Case Name:** View Reservation & Waitlist History (Xem Lịch sử Đặt giữ chỗ & Hàng chờ)  
**Version:** 1.0  
**Trạng thái:** Đã hoàn thành và triển khai tích hợp  

---

## 1. Tóm tắt (Brief Description)
Use Case này cho phép Bạn đọc (Patron - Sinh viên, Giảng viên) đã đăng nhập vào hệ thống Thư viện FPT University thực hiện xem, quản lý danh sách đặt giữ trước sách (Reservation) và danh sách xếp hàng chờ (Waitlist) của mình. Bạn đọc có thể theo dõi trạng thái đơn, thời hạn đến nhận sách, số thứ tự trong hàng chờ và thực hiện các quyền hủy đơn trực tuyến.

---

## 2. Tác nhân (Actors)
* **Tác nhân chính:** Bạn đọc (Patron)

---

## 3. Tiền điều kiện (Preconditions)
1. Bạn đọc đã đăng nhập thành công vào hệ thống.
2. Tài khoản bạn đọc tồn tại trên hệ thống.

---

## 4. Hậu điều kiện (Postconditions)
* Hệ thống hiển thị đầy đủ thông tin:
  - Mục **Đơn đặt giữ chỗ (Reservations)**: Mã đặt chỗ, Tên sách, Campus nhận sách, Ngày đặt, Hạn nhận sách (24h kể từ khi đặt), Trạng thái đơn (Đang giữ chỗ, Đã nhận sách, Bạn đọc hủy, Đã hết hạn).
  - Mục **Danh sách hàng chờ (Waitlist)**: Tên sách, Campus xếp hàng, Ngày đăng ký, Số thứ tự hiện tại, Trạng thái hàng chờ (Đang đợi, Đã báo có sách, Đã hủy).

---

## 5. Luồng sự kiện (Flow of Events)

### 5.1. Luồng chính (Basic Flow - Normal Flow)
1. Bạn đọc nhấn vào liên kết **"Đặt chỗ của tôi"** hoặc tài khoản cá nhân trên thanh điều hướng đầu trang (Header).
2. Hệ thống kiểm tra Session và lấy định danh của Bạn đọc (`loggedInUserId`).
3. Hệ thống gửi yêu cầu truy vấn đến Cơ sở dữ liệu:
   - Tìm toàn bộ các đơn đặt chỗ thuộc về Bạn đọc này tại bảng `Reservations`, sắp xếp theo thời gian đặt (`ReservedAt`) giảm dần.
   - Tìm toàn bộ các bản ghi đăng ký xếp hàng chờ thuộc về Bạn đọc này tại bảng `Waitlists`, sắp xếp theo thời gian đăng ký (`RequestedAt`) giảm dần.
4. Đối với mỗi bản ghi trong Danh sách xếp hàng chờ có trạng thái là `Waiting` hoặc `Notified`, hệ thống thực hiện tính toán động số thứ tự hiện tại của Bạn đọc trong hàng chờ (bằng cách đếm số người đăng ký trước mình cho cùng một đầu sách tại cùng một cơ sở).
5. Hệ thống hiển thị hai khu vực quản lý riêng biệt: **Sách đang đặt giữ chỗ** và **Sách đang xếp hàng chờ**.

---

### 5.2. Các luồng thay thế (Alternative Flows)

#### Alt 1: Bạn đọc thực hiện Hủy đặt giữ chỗ trực tuyến (Cancel Reservation)
* Thực hiện theo luồng thay thế **Alt 1** của đặc tả **UCR06**.

#### Alt 2: Bạn đọc thực hiện Hủy đăng ký xếp hàng chờ (Cancel Waitlist)
* Thực hiện theo luồng thay thế **Alt 2** của đặc tả **UCR06**.

---

### 5.3. Các luồng ngoại lệ (Exception Flows)

#### Exc 1: Bạn đọc chưa từng thực hiện đặt chỗ hoặc xếp hàng
* Tại bước 3 luồng chính, nếu hệ thống không tìm thấy bất kỳ bản ghi đặt chỗ hoặc hàng chờ nào:
  - Hệ thống hiển thị thông báo trống: *"Bạn hiện không có đơn đặt giữ chỗ hoặc hàng chờ nào hoạt động."* để Bạn đọc nắm được thông tin.

---

## 6. Quy tắc nghiệp vụ (Business Rules)
1. **Phân cấp trạng thái đơn đặt chỗ:**
   - `Holding`: Sách đang được giữ ở quầy, bạn đọc cần đến nhận trước hạn.
   - `Completed`: Sách đã được chuyển sang phiếu mượn thực tế thành công (thủ thư làm check-out).
   - `Cancelled`: Bạn đọc chủ động hủy.
   - `Expired`: Quá 24 giờ mà không đến nhận sách, hệ thống tự động giải phóng bản sách.
2. **Quyền riêng tư tuyệt đối:** Chỉ cho phép truy xuất dữ liệu đặt chỗ và hàng chờ khớp với ID đăng nhập hiện tại lưu trong Session để tránh lộ thông tin và tấn công thay đổi tham số.
