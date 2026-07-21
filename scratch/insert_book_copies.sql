USE [LMSVer2];
GO

DECLARE @BookID INT;
DECLARE @CampusID INT;
DECLARE @ShelfCode VARCHAR(20);
DECLARE @i INT;
DECLARE @CopyID VARCHAR(50);
DECLARE @Condition VARCHAR(20);
DECLARE @Status VARCHAR(20);
DECLARE @Count INT;

DECLARE book_cursor CURSOR FOR 
SELECT BookID FROM [dbo].[Books];

OPEN book_cursor;

FETCH NEXT FROM book_cursor INTO @BookID;

WHILE @@FETCH_STATUS = 0
BEGIN
    SET @i = 1;
    WHILE @i <= 3 -- Số lượng copy muốn tạo cho mỗi cuốn sách (3 cuốn)
    BEGIN
        -- Generate CopyID (Format: BC-{BookID}-00{i})
        SET @CopyID = 'BC-' + CAST(@BookID AS VARCHAR) + '-00' + CAST(@i AS VARCHAR);
        
        SELECT @Count = COUNT(*) FROM [dbo].[BookCopies] WHERE CopyID = @CopyID;
        
        IF @Count = 0
        BEGIN
            -- Pick random campus between 1 and 5
            SET @CampusID = ABS(CHECKSUM(NEWID()) % 5) + 1;
            
            -- Pick random shelf from that campus
            SELECT TOP 1 @ShelfCode = ShelfCode FROM [dbo].[Shelves] WHERE CampusID = @CampusID ORDER BY NEWID();
            
            -- If no shelf found for campus, fallback
            IF @ShelfCode IS NULL 
            BEGIN
                SELECT TOP 1 @CampusID = CampusID, @ShelfCode = ShelfCode FROM [dbo].[Shelves];
            END

            SET @Condition = CASE ABS(CHECKSUM(NEWID()) % 3) WHEN 0 THEN 'New' WHEN 1 THEN 'Good' ELSE 'Fair' END;
            SET @Status = 'Available';
            
            INSERT INTO [dbo].[BookCopies] ([CopyID], [BookID], [CampusID], [ShelfCode], [ConditionStatus], [CopyStatus], [AcquiredAt], [QRCode])
            VALUES (@CopyID, @BookID, @CampusID, @ShelfCode, @Condition, @Status, GETDATE(), 'QR-' + @CopyID);
        END
        
        SET @i = @i + 1;
    END

    FETCH NEXT FROM book_cursor INTO @BookID;
END

CLOSE book_cursor;
DEALLOCATE book_cursor;
GO
