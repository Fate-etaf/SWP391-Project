-- Script chèn dữ liệu thông báo giả (Mock Data) để test UCR05
-- Đảm bảo thay thế giá trị UserID bằng UserID hợp lệ đang có trong CSDL của bạn (ví dụ: 'SE001').
-- Cột CreatedAt sẽ được default tự động sinh nếu để trống, hoặc ta set cứng.

-- 1. Sách đăng ký Waitlist đã về
INSERT INTO dbo.Notifications (UserID, NotificationType, Title, Content, Status, CreatedAt, IsRead)
VALUES 
('SE001', 'WAITLIST_READY', N'Sách Waitlist đã sẵn sàng', N'Cuốn "Clean Code" bạn đăng ký chờ hiện đã có sẵn tại quầy FPTU Hòa Lạc. Bạn có 24h để đến làm thủ tục mượn sách.', 'Pending', GETDATE(), 0);

-- 2. Nhắc nhở sách sắp đến hạn trả
INSERT INTO dbo.Notifications (UserID, NotificationType, Title, Content, Status, CreatedAt, IsRead)
VALUES 
('SE001', 'DUE_ALERT', N'Nhắc nhở hạn trả sách', N'Sách "Design Patterns" của bạn sẽ đến hạn trả vào ngày mai. Vui lòng trả sách đúng hạn hoặc gia hạn nếu cần.', 'Pending', DATEADD(hour, -2, GETDATE()), 0);

-- 3. Thông báo đã đọc
INSERT INTO dbo.Notifications (UserID, NotificationType, Title, Content, Status, CreatedAt, IsRead)
VALUES 
('SE001', 'SYSTEM_UPDATE', N'Hệ thống bảo trì', N'Hệ thống thư viện FLMS sẽ bảo trì từ 2h-4h sáng mai.', 'Sent', DATEADD(day, -1, GETDATE()), 1);

-- 4. Thông báo phạt tiền (Chưa đọc)
INSERT INTO dbo.Notifications (UserID, NotificationType, Title, Content, Status, CreatedAt, IsRead)
VALUES 
('SE001', 'FINE_ALERT', N'Cảnh báo quá hạn', N'Bạn đã trễ hạn trả sách 3 ngày. Phí phạt hiện tại là 15,000đ. Vui lòng thanh toán sớm.', 'Pending', DATEADD(hour, -5, GETDATE()), 0);

-- 5. Thông báo đặt chỗ thành công (Đã đọc)
INSERT INTO dbo.Notifications (UserID, NotificationType, Title, Content, Status, CreatedAt, IsRead)
VALUES 
('SE001', 'RESERVATION_CONFIRMED', N'Đặt giữ chỗ thành công', N'Yêu cầu giữ chỗ cuốn "Introduction to Algorithms" của bạn đã được xác nhận.', 'Sent', DATEADD(day, -2, GETDATE()), 1);

-- Kiểm tra lại:
-- SELECT * FROM dbo.Notifications ORDER BY CreatedAt DESC;
