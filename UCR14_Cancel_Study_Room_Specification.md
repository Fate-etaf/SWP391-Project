# TÀI LIỆU ĐẶC TẢ USE CASE: UCR14 – CANCEL STUDY ROOM
**Use Case ID:** UCR14  
**Use Case Name:** Cancel Study Room (Hủy đặt phòng học)  
**Primary Actor:** Bạn đọc (Patron)  

---

## 1. Tóm tắt (Brief Description)
Use Case này cho phép Bạn đọc chủ động hủy đơn đặt phòng học đã được xác nhận (Confirmed) trước khi khung giờ sử dụng bắt đầu, nhằm giải phóng tài nguyên phòng cho các Bạn đọc khác.

---

## 2. Tiền điều kiện (Preconditions)
1. Bạn đọc đã đăng nhập thành công vào hệ thống.
2. Bạn đọc có ít nhất một đơn đặt phòng trong bảng `RoomBookings` đang ở trạng thái `Confirmed`.

---

## 3. Hậu điều kiện (Postconditions)
* Đơn đặt phòng chuyển trạng thái thành `Cancelled`.
* Khung giờ của phòng học đó được giải phóng thành công, cho phép người khác đặt lại.

---

## 4. Luồng sự kiện (Flow of Events)

### 4.1. Luồng chính (Basic Flow)
1. Bạn đọc truy cập vào trang **Danh sách đặt phòng của tôi** trên hệ thống.
2. Hệ thống hiển thị danh sách các đơn đặt phòng kèm trạng thái tương ứng.
3. Bạn đọc tìm kiếm một đơn đặt phòng đang ở trạng thái `Confirmed` (Chờ sử dụng) và nhấn nút **"Hủy đặt phòng"**.
4. Hệ thống hiển thị hộp thoại cảnh báo: *"Bạn có chắc chắn muốn hủy đặt phòng này không?"*.
5. Bạn đọc nhấn nút **"Đồng ý hủy"**.
6. Hệ thống thực hiện:
   - Cập nhật trường `Status` của bản ghi trong bảng `RoomBookings` thành `Cancelled`.
   - Xóa bỏ các block lịch bảo lưu để hệ thống hiểu rằng khung giờ này của phòng lại quay về trạng thái trống.
7. Hệ thống hiển thị thông báo thành công cho Bạn đọc trên màn hình.
8. Hệ thống sinh một thông báo xác nhận hủy vào Hộp thư nội bộ và kích hoạt Hệ thống Email gửi thư xác nhận hủy đơn đến cho Bạn đọc.

---

### 4.2. Các luồng ngoại lệ (Exception Flows)

#### Exc 1: Hủy đơn khi thời gian đã bắt đầu
1. Tại bước 6, nếu hệ thống kiểm tra thấy thời gian hiện tại đã vượt qua `StartTime` (tức là phòng đã bắt đầu trong khung giờ sử dụng).
2. Hệ thống từ chối yêu cầu hủy.
3. Hệ thống thông báo lỗi: *"Không thể hủy đặt phòng khi thời gian sử dụng đã bắt đầu."*

---

## 5. Quy tắc nghiệp vụ (Business Rules)
1. Đơn vị chỉ cho phép chuyển sang trạng thái `Cancelled` nếu trạng thái hiện tại đang là `Confirmed`.
2. Không thể hủy đối với các trạng thái kết thúc (`CheckedIn`, `CheckedOut`, `NoShow`, `Cancelled`).
