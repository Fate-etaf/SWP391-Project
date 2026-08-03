-- Script tổng hợp: TẠO MOCK DATA cho Hệ thống
-- Cập nhật User, Phòng học nhóm, và Lịch sử Luân chuyển sách

-------------------------------------------------------------------------
-- PHẦN 1: TẠO USER (LIB01, LIB02)
-------------------------------------------------------------------------
IF NOT EXISTS (SELECT 1 FROM Users WHERE UserID = 'LIB01')
BEGIN
    INSERT INTO Users (UserID, FullName, Email, PasswordHash, Phone, CampusID, RoleID, Status, BorrowingLocked)
    VALUES ('LIB01', N'Librarian 01', 'huybuchaki00@gmail.com', '123', '0123456789', 1, 3, 'Active', 0);
END

IF NOT EXISTS (SELECT 1 FROM Users WHERE UserID = 'LIB02')
BEGIN
    INSERT INTO Users (UserID, FullName, Email, PasswordHash, Phone, CampusID, RoleID, Status, BorrowingLocked)
    VALUES ('LIB02', N'Librarian 02', 'huybuchaki02@gmail.com', '123', '0987654321', 2, 3, 'Active', 0);
END
GO

-------------------------------------------------------------------------
-- PHẦN 2: RESET VÀ TẠO MỚI PHÒNG HỌC NHÓM (STUDY ROOMS)
-------------------------------------------------------------------------
-- Xóa tất cả các lịch đặt phòng (nếu có) để tránh lỗi Foreign Key
DELETE FROM RoomBookings;

-- Xóa tất cả phòng học nhóm hiện có
DELETE FROM StudyRooms;

-- Reset lại cột tự tăng (Identity) về 0 để các ID mới bắt đầu từ 1
DBCC CHECKIDENT ('StudyRooms', RESEED, 0);

-- Tạo 3 phòng cho cơ sở 1 (Hà Nội)
INSERT INTO StudyRooms (CampusID, RoomName, Capacity, Description, Status) VALUES (1, N'Phòng H-4', 4, N'Phòng học nhóm tiêu chuẩn 4 người', 'Available');
INSERT INTO StudyRooms (CampusID, RoomName, Capacity, Description, Status) VALUES (1, N'Phòng H-6', 6, N'Phòng học nhóm 6 người', 'Available');
INSERT INTO StudyRooms (CampusID, RoomName, Capacity, Description, Status) VALUES (1, N'Phòng H-8', 8, N'Phòng học nhóm lớn 8 người', 'Available');

-- Tạo 3 phòng cho cơ sở 2 (Đà Nẵng)
INSERT INTO StudyRooms (CampusID, RoomName, Capacity, Description, Status) VALUES (2, N'Phòng D-4', 4, N'Phòng học nhóm tiêu chuẩn 4 người', 'Available');
INSERT INTO StudyRooms (CampusID, RoomName, Capacity, Description, Status) VALUES (2, N'Phòng D-6', 6, N'Phòng học nhóm 6 người', 'Available');
INSERT INTO StudyRooms (CampusID, RoomName, Capacity, Description, Status) VALUES (2, N'Phòng D-8', 8, N'Phòng học nhóm lớn 8 người', 'Available');

-- Tạo 3 phòng cho cơ sở 3 (TP HCM)
INSERT INTO StudyRooms (CampusID, RoomName, Capacity, Description, Status) VALUES (3, N'Phòng S-4', 4, N'Phòng học nhóm tiêu chuẩn 4 người', 'Available');
INSERT INTO StudyRooms (CampusID, RoomName, Capacity, Description, Status) VALUES (3, N'Phòng S-6', 6, N'Phòng học nhóm 6 người', 'Available');
INSERT INTO StudyRooms (CampusID, RoomName, Capacity, Description, Status) VALUES (3, N'Phòng S-8', 8, N'Phòng học nhóm lớn 8 người', 'Available');

-- Tạo 3 phòng cho cơ sở 4 (Cần Thơ)
INSERT INTO StudyRooms (CampusID, RoomName, Capacity, Description, Status) VALUES (4, N'Phòng C-4', 4, N'Phòng học nhóm tiêu chuẩn 4 người', 'Available');
INSERT INTO StudyRooms (CampusID, RoomName, Capacity, Description, Status) VALUES (4, N'Phòng C-6', 6, N'Phòng học nhóm 6 người', 'Available');
INSERT INTO StudyRooms (CampusID, RoomName, Capacity, Description, Status) VALUES (4, N'Phòng C-8', 8, N'Phòng học nhóm lớn 8 người', 'Available');

-- Tạo 3 phòng cho cơ sở 5 (Quy Nhơn)
INSERT INTO StudyRooms (CampusID, RoomName, Capacity, Description, Status) VALUES (5, N'Phòng Q-4', 4, N'Phòng học nhóm tiêu chuẩn 4 người', 'Available');
INSERT INTO StudyRooms (CampusID, RoomName, Capacity, Description, Status) VALUES (5, N'Phòng Q-6', 6, N'Phòng học nhóm 6 người', 'Available');
INSERT INTO StudyRooms (CampusID, RoomName, Capacity, Description, Status) VALUES (5, N'Phòng Q-8', 8, N'Phòng học nhóm lớn 8 người', 'Available');
GO

-------------------------------------------------------------------------
-- PHẦN 3: RESET VÀ TẠO MỚI LUÂN CHUYỂN SÁCH (TRANSFERS)
-------------------------------------------------------------------------
-- Xóa toàn bộ dữ liệu luân chuyển hiện tại (Lịch sử và Pending)
DELETE FROM TransferDetails;
DELETE FROM TransferRequests;
DBCC CHECKIDENT ('TransferRequests', RESEED, 0);

-- Khai báo các tài khoản thủ thư đại diện cho các cơ sở
DECLARE @LibHN VARCHAR(20) = (SELECT TOP 1 UserID FROM Users WHERE CampusID = 1 AND RoleID = 3);
DECLARE @LibDN VARCHAR(20) = (SELECT TOP 1 UserID FROM Users WHERE CampusID = 2 AND RoleID = 3);
DECLARE @LibHCM VARCHAR(20) = (SELECT TOP 1 UserID FROM Users WHERE CampusID = 3 AND RoleID = 3);
DECLARE @LibCT VARCHAR(20) = (SELECT TOP 1 UserID FROM Users WHERE CampusID = 4 AND RoleID = 3);
DECLARE @TransferID INT;

-- A. LỊCH SỬ TRANSFER (Trạng thái Received, Rejected, Cancelled)

-- Lịch sử 1: Đã nhận thành công (Từ Đà Nẵng -> Hà Nội)
INSERT INTO TransferRequests (RequestedBy, FromCampusID, ToCampusID, RequestedAt, ShippedAt, ReceivedAt, ConfirmedBy, ShippedBy, Status, Note)
VALUES (@LibHN, 2, 1, DATEADD(DAY, -15, GETDATE()), DATEADD(DAY, -12, GETDATE()), DATEADD(DAY, -10, GETDATE()), @LibHN, @LibDN, 'Received', N'Xin vài cuốn sách lập trình C từ Đà Nẵng ra Hà Nội.');
SET @TransferID = SCOPE_IDENTITY();
INSERT INTO TransferDetails (TransferID, CopyID)
SELECT TOP 2 @TransferID, CopyID FROM BookCopies WHERE CampusID = 1 AND CopyStatus = 'Available';

-- Lịch sử 2: Đã nhận thành công (Từ Hà Nội -> HCM)
INSERT INTO TransferRequests (RequestedBy, FromCampusID, ToCampusID, RequestedAt, ShippedAt, ReceivedAt, ConfirmedBy, ShippedBy, Status, Note)
VALUES (@LibHCM, 1, 3, DATEADD(DAY, -20, GETDATE()), DATEADD(DAY, -18, GETDATE()), DATEADD(DAY, -16, GETDATE()), @LibHCM, @LibHN, 'Received', N'Hồ Chí Minh thiếu sách kinh tế, nhờ Hà Nội chuyển vào.');
SET @TransferID = SCOPE_IDENTITY();
INSERT INTO TransferDetails (TransferID, CopyID)
SELECT TOP 3 @TransferID, CopyID FROM BookCopies WHERE CampusID = 3 AND CopyStatus = 'Available';

-- Lịch sử 3: Bị từ chối (Từ Cần Thơ -> Đà Nẵng)
INSERT INTO TransferRequests (RequestedBy, FromCampusID, ToCampusID, RequestedAt, ConfirmedBy, Status, Note)
VALUES (@LibDN, 4, 2, DATEADD(DAY, -5, GETDATE()), @LibCT, 'Rejected', N'Đà Nẵng xin mượn sách chuyên ngành Đồ họa.');
SET @TransferID = SCOPE_IDENTITY();
INSERT INTO TransferDetails (TransferID, CopyID)
SELECT TOP 1 @TransferID, CopyID FROM BookCopies WHERE CampusID = 4 AND CopyStatus = 'Available';

-- Lịch sử 4: Đã hủy (Từ HCM -> Hà Nội)
INSERT INTO TransferRequests (RequestedBy, FromCampusID, ToCampusID, RequestedAt, Status, Note)
VALUES (@LibHN, 3, 1, DATEADD(DAY, -7, GETDATE()), 'Cancelled', N'Hà Nội xin sách HCM nhưng đã tìm thấy trong kho nên hủy đơn.');
SET @TransferID = SCOPE_IDENTITY();
INSERT INTO TransferDetails (TransferID, CopyID)
SELECT TOP 2 @TransferID, CopyID FROM BookCopies WHERE CampusID = 3 AND CopyStatus = 'Available' AND CopyID NOT IN (SELECT CopyID FROM TransferDetails);

-- B. YÊU CẦU ĐANG XỬ LÝ (Trạng thái Pending, Accepted, InTransit)

-- Yêu cầu 1: Pending (Từ Hà Nội -> Đà Nẵng)
INSERT INTO TransferRequests (RequestedBy, FromCampusID, ToCampusID, RequestedAt, Status, Note)
VALUES (@LibDN, 1, 2, DATEADD(HOUR, -5, GETDATE()), 'Pending', N'Đà Nẵng đang thiếu nhiều sách giáo trình, Hà Nội chi viện khẩn cấp!');
SET @TransferID = SCOPE_IDENTITY();
INSERT INTO TransferDetails (TransferID, CopyID)
SELECT TOP 2 @TransferID, CopyID FROM BookCopies WHERE CampusID = 1 AND CopyStatus = 'Available' AND CopyID NOT IN (SELECT CopyID FROM TransferDetails);

-- Yêu cầu 2: Pending (Từ Cần Thơ -> Hà Nội)
INSERT INTO TransferRequests (RequestedBy, FromCampusID, ToCampusID, RequestedAt, Status, Note)
VALUES (@LibHN, 4, 1, DATEADD(DAY, -1, GETDATE()), 'Pending', N'Sinh viên Hà Nội đang cần gấp sách này làm đồ án.');
SET @TransferID = SCOPE_IDENTITY();
INSERT INTO TransferDetails (TransferID, CopyID)
SELECT TOP 1 @TransferID, CopyID FROM BookCopies WHERE CampusID = 4 AND CopyStatus = 'Available' AND CopyID NOT IN (SELECT CopyID FROM TransferDetails);

-- Yêu cầu 3: Accepted (Từ HCM -> Hà Nội)
INSERT INTO TransferRequests (RequestedBy, FromCampusID, ToCampusID, RequestedAt, ConfirmedBy, Status, Note)
VALUES (@LibHN, 3, 1, DATEADD(DAY, -2, GETDATE()), @LibHCM, 'Accepted', N'Hà Nội xin 2 cuốn sách ngoại ngữ từ Hồ Chí Minh.');
SET @TransferID = SCOPE_IDENTITY();
INSERT INTO TransferDetails (TransferID, CopyID)
SELECT TOP 2 @TransferID, CopyID FROM BookCopies WHERE CampusID = 3 AND CopyStatus = 'Available' AND CopyID NOT IN (SELECT CopyID FROM TransferDetails);

-- Yêu cầu 4: InTransit (Từ Đà Nẵng -> Hà Nội)
INSERT INTO TransferRequests (RequestedBy, FromCampusID, ToCampusID, RequestedAt, ShippedAt, ConfirmedBy, ShippedBy, Status, Note)
VALUES (@LibHN, 2, 1, DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, -1, GETDATE()), @LibDN, @LibDN, 'InTransit', N'Hà Nội cần sách gấp, Đà Nẵng chuyển phát nhanh nhé.');
SET @TransferID = SCOPE_IDENTITY();
INSERT INTO TransferDetails (TransferID, CopyID)
SELECT TOP 1 @TransferID, CopyID FROM BookCopies WHERE CampusID = 2 AND CopyStatus = 'Available' AND CopyID NOT IN (SELECT CopyID FROM TransferDetails);
GO
