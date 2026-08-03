-- Script bổ sung cột ShippedBy vào bảng TransferRequests
-- Khắc phục lỗi Whitelabel 500 do thiếu cột khi fetch dữ liệu lười (Lazy Load)

-- 1. Thêm cột ShippedBy (kiểu VARCHAR(20) cho đồng bộ với UserID)
ALTER TABLE [dbo].[TransferRequests] ADD [ShippedBy] [varchar](20) NULL;
GO

-- 2. Thêm khóa ngoại (Foreign Key) trỏ tới bảng Users
ALTER TABLE [dbo].[TransferRequests]  WITH CHECK ADD FOREIGN KEY([ShippedBy])
REFERENCES [dbo].[Users] ([UserID])
GO
