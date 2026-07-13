USE [LMSVer2]
GO

-- 1. Tìm tên Constraint tự sinh hiện tại đang check cột Status trong bảng TransferRequests
DECLARE @ConstraintName NVARCHAR(200)

SELECT @ConstraintName = name 
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID('TransferRequests')
AND definition LIKE '%[Status]=''Pending''%'

-- 2. Xóa Constraint tự sinh đó (nếu tìm thấy)
IF @ConstraintName IS NOT NULL
BEGIN
    DECLARE @DropSQL NVARCHAR(500) = 'ALTER TABLE [dbo].[TransferRequests] DROP CONSTRAINT [' + @ConstraintName + ']'
    EXEC(@DropSQL)
    PRINT 'Đã xóa constraint cũ: ' + @ConstraintName
END
GO

-- 3. Tạo lại Constraint mới với một cái tên cố định và thêm Accepted, Rejected
ALTER TABLE [dbo].[TransferRequests] 
ADD CONSTRAINT [CHK_TransferRequests_Status] 
CHECK (([Status]='Cancelled' OR [Status]='Received' OR [Status]='InTransit' OR [Status]='Pending' OR [Status]='Accepted' OR [Status]='Rejected'))
GO

PRINT 'Đã thêm Constraint mới CHK_TransferRequests_Status thành công!'