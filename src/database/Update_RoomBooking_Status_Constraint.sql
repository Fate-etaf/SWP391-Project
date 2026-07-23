-- Kịch bản cập nhật: Thêm trạng thái 'Evicted' (Vi phạm nội quy bị mời ra) vào cột Status của bảng RoomBookings

USE [LMSVer2]; -- Sửa tên Database ở đây nếu cần
GO

PRINT '1. Dang tim kiem Check Constraint cu tren cot Status cua bang RoomBookings...';

DECLARE @ConstraintName nvarchar(200);

-- Tìm tên của Check Constraint cũ (do constraint cũ chưa được đặt tên, SQL Server đã tự sinh tên ngẫu nhiên)
SELECT @ConstraintName = Name 
FROM sys.check_constraints 
WHERE parent_object_id = object_id('dbo.RoomBookings') 
  AND definition LIKE '%NoShow%';

IF @ConstraintName IS NOT NULL
BEGIN
    PRINT 'Da tim thay Constraint cu: ' + @ConstraintName + '. Tien hanh xoa...';
    EXEC('ALTER TABLE [dbo].[RoomBookings] DROP CONSTRAINT ' + @ConstraintName);
    PRINT 'Xoa thanh cong Constraint cu.';
END
ELSE
BEGIN
    PRINT 'Khong tim thay Constraint cu hoac da bi xoa tu truoc.';
END

PRINT '2. Tien hanh them Check Constraint moi voi Status ''Evicted''...';
GO

-- Thêm lại Constraint mới với tên gọi rõ ràng hơn và bổ sung trạng thái 'Evicted'
ALTER TABLE [dbo].[RoomBookings] 
WITH CHECK ADD CONSTRAINT [CHK_RoomBooking_Status] 
CHECK  (([Status]='Evicted' OR [Status]='NoShow' OR [Status]='Cancelled' OR [Status]='CheckedOut' OR [Status]='CheckedIn' OR [Status]='Confirmed'));
GO

PRINT 'Hoan tat cap nhat!';
GO
