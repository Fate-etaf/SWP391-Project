# TÀI LIỆU ĐẶC TẢ USE CASE: UCR10 – CHECK-IN/OUT STUDY ROOM
**Use Case ID:** UCR10 
**Use Case Name:** Check-in / Check-out Study Room (Nhận và Trả phòng học)  
**Primary Actor:** Bạn đọc (Patron)  
**Secondary Actor:** Hệ thống Thời gian (System Clock)

---

## 1. Tóm tắt (Brief Description)
Use Case này cho phép Bạn đọc sử dụng mã QR (được cấp khi đặt phòng) để Check-in (nhận phòng) và Check-out (trả phòng). Đặc biệt, hệ thống tích hợp luồng chạy ngầm tự động hủy đơn và ghi nhận vi phạm No-Show nếu nhóm không đến nhận phòng hoặc không đến đủ số lượng thành viên trong thời gian quy định.

---

## 2. Tiền điều kiện (Preconditions)
1. Bạn đọc có một đơn đặt phòng (`RoomBookings`) đang ở trạng thái `Confirmed` (khi Check-in) hoặc `CheckedIn` (khi Check-out).
2. Thời gian hiện tại phải nằm trong khung giờ hợp lệ của đơn đặt phòng.

---

## 3. Hậu điều kiện (Postconditions)
* Trạng thái đơn đặt phòng được cập nhật thành `CheckedIn`, `CheckedOut` hoặc `NoShow` tùy thuộc vào hành động của Bạn đọc/Hệ thống.
* (Có thể) Quyền đặt phòng của Bạn đọc bị khóa nếu vi phạm nội quy quá nhiều lần.

---

## 4. Luồng sự kiện (Flow of Events)

### 4.1. Luồng Check-in (Nhận phòng)
1. Bạn đọc (Người đại diện book phòng) đến phòng học và tiến hành quét mã QR trên thiết bị quét tại cửa phòng (hoặc ứng dụng).
2. Hệ thống đọc thông tin từ mã QR (bao gồm `BookingID` và `PatronID`).
3. Hệ thống kiểm tra tính hợp lệ:
   - Trạng thái đơn phải là `Confirmed`.
   - Thời gian hiện tại nằm trong khoảng thời gian cho phép (Ví dụ: Từ `StartTime - 15 phút` đến `StartTime + 15 phút`).
4. Hệ thống cập nhật trạng thái đơn trong `RoomBookings` thành `CheckedIn`.
5. Hệ thống gửi thông báo: *"Bạn đã check-in thành công vào phòng [RoomName]. Xin lưu ý các quy định: Không ăn uống, giữ gìn tài sản. Phòng sẽ tự động giải phóng lúc [EndTime]."*

### 4.2. Luồng Check-out (Trả phòng)
1. Khi rời đi, Bạn đọc quét lại mã QR tại cửa (hoặc nhấn nút Trả phòng trên ứng dụng).
2. Hệ thống xác thực đơn đặt phòng đang ở trạng thái `CheckedIn`.
3. Hệ thống cập nhật trạng thái đơn thành `CheckedOut` và ghi nhận thời gian rời đi.
4. Hệ thống hiển thị thông báo trả phòng thành công.

---

### 4.3. Các luồng ngoại lệ (Exception Flows)

#### Exc 1: Bạn đọc không đến nhận phòng (Auto No-Show Cancellation)
1. Hệ thống Thời gian (Scheduler) chạy ngầm định kỳ (mỗi phút).
2. Hệ thống quét các đơn đặt phòng có trạng thái `Confirmed` mà thời gian hiện tại đã vượt quá `StartTime + 15 phút`.
3. Hệ thống tự động cập nhật trạng thái các đơn này thành `NoShow` trong bảng `RoomBookings`.
4. Hệ thống tạo thông báo và Email gửi cho Bạn đọc: *"Ca đặt phòng của bạn đã bị hủy tự động do không check-in trong vòng 15 phút đầu. Quyền đặt phòng của bạn bị đánh dấu vi phạm."*
5. Hệ thống ghi nhận mức độ vi phạm của Bạn đọc theo **Quy tắc 3**.

#### Exc 2: Thủ thư chủ động hủy ca đặt phòng (Thực thi bởi Actor: Librarian)
1. Sau 15 phút kể từ `StartTime`, Thủ thư đi tuần tra thực tế và phát hiện phòng không có đủ số lượng tối thiểu (4 người), hoặc phát hiện vi phạm ăn uống/làm hỏng tài sản.
2. Thủ thư truy cập vào phân hệ Quản lý Đặt phòng, tìm ca đặt của nhóm và chọn **"Hủy & Ghi nhận vi phạm"**.
3. Hệ thống cập nhật trạng thái đơn thành `Cancelled` (hoặc `NoShow`), đồng thời thu hồi quyền đặt phòng của chủ tài khoản.

---

## 5. Quy tắc nghiệp vụ (Business Rules)
1. **Grace Period (Thời gian ân hạn):** Bạn đọc có tối đa 15 phút sau `StartTime` để check-in. Quá 15 phút, hệ thống tự động hủy đơn.
2. **Kiểm tra quân số:** Hệ thống tin tưởng giao diện đăng ký (4-8 người). Nếu trên thực tế không đủ 4 người, nhóm vẫn có thể bị hủy ca book do sự can thiệp thủ công từ Thủ thư.
3. **Xử lý vi phạm (Penalty):** Bạn đọc vi phạm nội quy (Bị hệ thống đánh `NoShow` do vắng mặt, hoặc bị Thủ thư ghi nhận do không đủ số lượng người/vi phạm nội quy phòng học) sẽ bị **hủy quyền book phòng cho các lần sau**. (Quản lý qua trường trạng thái hoặc bảng Vi phạm của thư viện).
4. **Đền bù tài sản:** Xử lý bên ngoài hệ thống đặt phòng, ghi nhận thông qua hóa đơn đền bù (FineInvoices).
