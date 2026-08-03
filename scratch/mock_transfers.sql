-- Script tạo Mock Data Luân chuyển sách (Transfer Requests)
-- Đồng bộ với số lượng sách thực tế đang có ở mỗi cơ sở.

-- 1. Xóa toàn bộ dữ liệu luân chuyển hiện tại (Lịch sử và Pending)
DELETE FROM TransferDetails;
DELETE FROM TransferRequests;
DBCC CHECKIDENT ('TransferRequests', RESEED, 0);

-- 2. Khai báo các tài khoản thủ thư đại diện cho các cơ sở
DECLARE @LibHN VARCHAR(20) = (SELECT TOP 1 UserID FROM Users WHERE CampusID = 1 AND RoleID = 3);
DECLARE @LibDN VARCHAR(20) = (SELECT TOP 1 UserID FROM Users WHERE CampusID = 2 AND RoleID = 3);
DECLARE @LibHCM VARCHAR(20) = (SELECT TOP 1 UserID FROM Users WHERE CampusID = 3 AND RoleID = 3);
DECLARE @LibCT VARCHAR(20) = (SELECT TOP 1 UserID FROM Users WHERE CampusID = 4 AND RoleID = 3);
DECLARE @TransferID INT;

-------------------------------------------------------------------------
-- A. LỊCH SỬ TRANSFER (Trạng thái Received, Rejected, Cancelled)
-------------------------------------------------------------------------

-- Lịch sử 1: Đã nhận thành công (Từ Đà Nẵng -> Hà Nội)
-- Sách hiện tại đã thuộc về Hà Nội (CampusID = 1)
INSERT INTO TransferRequests (RequestedBy, FromCampusID, ToCampusID, RequestedAt, ShippedAt, ReceivedAt, ConfirmedBy, ShippedBy, Status, Note)
VALUES (@LibHN, 2, 1, DATEADD(DAY, -15, GETDATE()), DATEADD(DAY, -12, GETDATE()), DATEADD(DAY, -10, GETDATE()), @LibHN, @LibDN, 'Received', N'Xin vài cuốn sách lập trình C từ Đà Nẵng ra Hà Nội.');
SET @TransferID = SCOPE_IDENTITY();
INSERT INTO TransferDetails (TransferID, CopyID)
SELECT TOP 2 @TransferID, CopyID FROM BookCopies WHERE CampusID = 1 AND CopyStatus = 'Available';

-- Lịch sử 2: Đã nhận thành công (Từ Hà Nội -> HCM)
-- Sách hiện tại đã thuộc về HCM (CampusID = 3)
INSERT INTO TransferRequests (RequestedBy, FromCampusID, ToCampusID, RequestedAt, ShippedAt, ReceivedAt, ConfirmedBy, ShippedBy, Status, Note)
VALUES (@LibHCM, 1, 3, DATEADD(DAY, -20, GETDATE()), DATEADD(DAY, -18, GETDATE()), DATEADD(DAY, -16, GETDATE()), @LibHCM, @LibHN, 'Received', N'Hồ Chí Minh thiếu sách kinh tế, nhờ Hà Nội chuyển vào.');
SET @TransferID = SCOPE_IDENTITY();
INSERT INTO TransferDetails (TransferID, CopyID)
SELECT TOP 3 @TransferID, CopyID FROM BookCopies WHERE CampusID = 3 AND CopyStatus = 'Available';

-- Lịch sử 3: Bị từ chối (Từ Cần Thơ -> Đà Nẵng)
-- Đà Nẵng xin Cần Thơ, nhưng Cần Thơ từ chối. Sách hiện tại vẫn ở Cần Thơ (CampusID = 4)
INSERT INTO TransferRequests (RequestedBy, FromCampusID, ToCampusID, RequestedAt, ConfirmedBy, Status, Note)
VALUES (@LibDN, 4, 2, DATEADD(DAY, -5, GETDATE()), @LibCT, 'Rejected', N'Đà Nẵng xin mượn sách chuyên ngành Đồ họa.');
SET @TransferID = SCOPE_IDENTITY();
INSERT INTO TransferDetails (TransferID, CopyID)
SELECT TOP 1 @TransferID, CopyID FROM BookCopies WHERE CampusID = 4 AND CopyStatus = 'Available';

-- Lịch sử 4: Đã hủy (Từ HCM -> Hà Nội)
-- Hà Nội xin sách HCM, nhưng sau đó Hà Nội tự hủy đơn. Sách hiện tại vẫn ở HCM (CampusID = 3)
INSERT INTO TransferRequests (RequestedBy, FromCampusID, ToCampusID, RequestedAt, Status, Note)
VALUES (@LibHN, 3, 1, DATEADD(DAY, -7, GETDATE()), 'Cancelled', N'Hà Nội xin sách HCM nhưng đã tìm thấy trong kho nên hủy đơn.');
SET @TransferID = SCOPE_IDENTITY();
INSERT INTO TransferDetails (TransferID, CopyID)
SELECT TOP 2 @TransferID, CopyID FROM BookCopies WHERE CampusID = 3 AND CopyStatus = 'Available' AND CopyID NOT IN (SELECT CopyID FROM TransferDetails);

-------------------------------------------------------------------------
-- B. YÊU CẦU ĐANG XỬ LÝ (Trạng thái Pending, Accepted, InTransit)
-------------------------------------------------------------------------

-- Yêu cầu 1: Pending (Từ Hà Nội -> Đà Nẵng)
-- Đà Nẵng đang xin sách của Hà Nội (Chưa được duyệt). Sách hiện tại vẫn ở Hà Nội (CampusID = 1)
INSERT INTO TransferRequests (RequestedBy, FromCampusID, ToCampusID, RequestedAt, Status, Note)
VALUES (@LibDN, 1, 2, DATEADD(HOUR, -5, GETDATE()), 'Pending', N'Đà Nẵng đang thiếu nhiều sách giáo trình, Hà Nội chi viện khẩn cấp!');
SET @TransferID = SCOPE_IDENTITY();
INSERT INTO TransferDetails (TransferID, CopyID)
SELECT TOP 2 @TransferID, CopyID FROM BookCopies WHERE CampusID = 1 AND CopyStatus = 'Available' AND CopyID NOT IN (SELECT CopyID FROM TransferDetails);

-- Yêu cầu 2: Pending (Từ Cần Thơ -> Hà Nội)
-- Hà Nội đang xin sách của Cần Thơ (Chưa được duyệt). Sách hiện tại vẫn ở Cần Thơ (CampusID = 4)
INSERT INTO TransferRequests (RequestedBy, FromCampusID, ToCampusID, RequestedAt, Status, Note)
VALUES (@LibHN, 4, 1, DATEADD(DAY, -1, GETDATE()), 'Pending', N'Sinh viên Hà Nội đang cần gấp sách này làm đồ án.');
SET @TransferID = SCOPE_IDENTITY();
INSERT INTO TransferDetails (TransferID, CopyID)
SELECT TOP 1 @TransferID, CopyID FROM BookCopies WHERE CampusID = 4 AND CopyStatus = 'Available' AND CopyID NOT IN (SELECT CopyID FROM TransferDetails);

-- Yêu cầu 3: Accepted (Từ HCM -> Hà Nội)
-- Hà Nội xin sách của HCM, HCM đã ĐỒNG Ý nhưng chưa chuyển đi. Sách hiện tại vẫn ở HCM (CampusID = 3)
INSERT INTO TransferRequests (RequestedBy, FromCampusID, ToCampusID, RequestedAt, ConfirmedBy, Status, Note)
VALUES (@LibHN, 3, 1, DATEADD(DAY, -2, GETDATE()), @LibHCM, 'Accepted', N'Hà Nội xin 2 cuốn sách ngoại ngữ từ Hồ Chí Minh.');
SET @TransferID = SCOPE_IDENTITY();
INSERT INTO TransferDetails (TransferID, CopyID)
SELECT TOP 2 @TransferID, CopyID FROM BookCopies WHERE CampusID = 3 AND CopyStatus = 'Available' AND CopyID NOT IN (SELECT CopyID FROM TransferDetails);

-- Yêu cầu 4: InTransit (Từ Đà Nẵng -> Hà Nội)
-- Hà Nội xin sách Đà Nẵng, Đà Nẵng ĐÃ GỬI ĐI. Đang trên đường vận chuyển. Sách ở database tạm thời vẫn thuộc Đà Nẵng (CampusID = 2)
INSERT INTO TransferRequests (RequestedBy, FromCampusID, ToCampusID, RequestedAt, ShippedAt, ConfirmedBy, ShippedBy, Status, Note)
VALUES (@LibHN, 2, 1, DATEADD(DAY, -3, GETDATE()), DATEADD(DAY, -1, GETDATE()), @LibDN, @LibDN, 'InTransit', N'Hà Nội cần sách gấp, Đà Nẵng chuyển phát nhanh nhé.');
SET @TransferID = SCOPE_IDENTITY();
INSERT INTO TransferDetails (TransferID, CopyID)
SELECT TOP 1 @TransferID, CopyID FROM BookCopies WHERE CampusID = 2 AND CopyStatus = 'Available' AND CopyID NOT IN (SELECT CopyID FROM TransferDetails);
