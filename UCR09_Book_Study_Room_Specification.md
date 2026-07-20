# TÀI LIỆU ĐẶC TẢ USE CASE: UCR09 – BOOK STUDY ROOM
**Use Case ID:** UCR09  
**Use Case Name:** Book Study Room (Đặt phòng học nhóm)  
**Primary Actor:** Bạn đọc (Patron - Student/Lecturer)  
**Secondary Actor:** Hệ thống Email, Hệ thống Thời gian (System Clock)

---

## 1. Tóm tắt (Brief Description)
Use Case này cho phép Bạn đọc (Patron) đã đăng nhập vào hệ thống thực hiện đặt trước phòng học nhóm tại các cơ sở của thư viện theo khung giờ mong muốn. Hệ thống sẽ kiểm tra tính hợp lệ của khung giờ, các ràng buộc nghiệp vụ (tối đa 2 giờ, số người, tần suất đặt...) và ghi nhận thông tin đặt phòng.

---

## 2. Tiền điều kiện (Preconditions)
1. Bạn đọc đã đăng nhập thành công vào hệ thống thư viện.
2. Trạng thái tài khoản của Bạn đọc là `Active` và không bị khóa quyền đặt phòng (chưa vi phạm nội quy trước đó).

---

## 3. Hậu điều kiện (Postconditions)
* **Trường hợp thành công (Normal Flow):**
  - Một bản ghi `RoomBookings` mới được tạo với trạng thái `Confirmed`.
  - Mã xác thực `QRCode` được sinh ra để phục vụ quá trình Check-in.
  - Hệ thống gửi thông báo nội bộ và Email xác nhận cho Bạn đọc.

---

## 4. Luồng sự kiện (Flow of Events)

### 4.1. Luồng chính (Basic Flow)
1. Bạn đọc truy cập vào chức năng **Đặt phòng học (Book Study Room)** trên hệ thống.
2. Hệ thống hiển thị giao diện chọn **Cơ sở (Campus)** và **Ngày**. (Lưu ý: Chỉ cho phép chọn ngày hiện tại hoặc trước tối đa 1 ngày).
3. Hệ thống truy vấn bảng `StudyRooms` (với `Status` = 'Available') và danh sách các khung giờ còn trống trong ngày dựa trên bảng `RoomBookings`.
4. Hệ thống hiển thị danh sách các phòng và các khung giờ khả dụng từ 08:30 đến 17:00 (Thứ 2 - Thứ 6).
5. Bạn đọc chọn một phòng cụ thể và điền các thông tin:
   - Khung giờ bắt đầu (`StartTime`).
   - Khung giờ kết thúc (`EndTime`).
   - Số lượng người tham gia (`ParticipantCount` - Giao diện chặn ở mức 4 đến 8 người).
   - Lý do sử dụng (`Purpose`).
6. Hệ thống kiểm tra các quy định kinh doanh:
   - Thời gian sử dụng (`EndTime` - `StartTime`) tối đa là 2 giờ.
   - Tài khoản (Nhóm) này chưa có đơn đặt phòng nào khác trong cùng ngày hôm đó (Chỉ book 1 ca/ngày).
   - `ParticipantCount` nằm trong khoảng từ 4 đến 8.
7. Hệ thống xác nhận khung giờ đó chưa bị người khác đặt trùng **[Xem Exc 1]**.
8. Bạn đọc nhấn nút **"Xác nhận đặt phòng"**.
9. Hệ thống thực hiện:
   - Lưu bản ghi vào bảng `RoomBookings` với trạng thái `Confirmed`, thời gian tạo `CreatedAt` = thời gian hiện tại.
   - Khởi tạo mã `QRCode` lưu vào CSDL.
10. Hệ thống hiển thị thông báo thành công cùng với lời nhắc nhở nội quy phòng học.
11. Hệ thống tạo thông báo nội bộ và kích hoạt Hệ thống Email gửi thư xác nhận (kèm QRCode) đến Bạn đọc.

---

### 4.2. Các luồng ngoại lệ (Exception Flows)

#### Exc 1: Khung giờ đã bị chiếm chỗ (Race Condition)
1. Tại bước 7 của luồng chính, hệ thống phát hiện khung giờ vừa được một Bạn đọc khác đặt thành công.
2. Hệ thống từ chối lưu đơn đặt phòng.
3. Hệ thống hiển thị thông báo lỗi: *"Rất tiếc, khung giờ bạn chọn vừa được người khác đặt. Vui lòng chọn khung giờ khác."*
4. Hệ thống tự động làm mới lại lịch phòng để Bạn đọc chọn lại.

---

## 5. Quy tắc nghiệp vụ (Business Rules)
1. **Chỉ hiển thị phòng có thể đặt:** Các phòng có `Status` = 'Available' trong bảng `StudyRooms`.
2. **Khung giờ phục vụ:** 08:30 - 17:00 từ Thứ Hai tới Thứ Sáu.
3. **Giới hạn thời gian (Max duration):** Tối đa 2 giờ cho 1 lần đăng ký.
4. **Giới hạn đặt trước (Advance Booking):** Bạn đọc chỉ được phép book trước tối đa 1 ngày.
5. **Giới hạn tần suất:** Mỗi tài khoản/nhóm chỉ được book duy nhất 1 ca/ngày. (Xác định bằng cách đếm số booking của `PatronID` trong ngày đó có trạng thái khác `Cancelled` và `NoShow`).
6. **Số lượng thành viên:** Tối thiểu 4 người, tối đa 8 người.
7. **Nội quy sử dụng:** Không xê dịch, làm hư hỏng đồ dùng; không mang đồ ăn/thức uống/đồ dễ cháy nổ vào phòng. Vi phạm sẽ bị thu hồi quyền đặt phòng và đền bù tài sản (Xử lý bởi Thủ thư ở màn hình quản trị).
