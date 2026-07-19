USE [LMSVer2];
GO
SET QUOTED_IDENTIFIER ON;
GO

DECLARE @BookID_Scen5 INT;

-- Find a book with 3 available copies to use for this scenario
WITH CTE AS (
    SELECT BookID, ROW_NUMBER() OVER (ORDER BY BookID DESC) as rn
    FROM [dbo].[BookCopies]
    WHERE CopyStatus = 'Available'
    GROUP BY BookID
    HAVING COUNT(*) = 3
)
SELECT @BookID_Scen5 = MAX(CASE WHEN rn = 1 THEN BookID END)
FROM CTE;

PRINT 'Scenario 5 (Book not available in current campus): BookID = ' + ISNULL(CAST(@BookID_Scen5 AS VARCHAR), 'N/A');

IF @BookID_Scen5 IS NOT NULL
BEGIN
    -- Move all copies of this book to CampusID = 2 (Da Nang)
    -- Shelf '010' belongs to Campus 2 based on the sample data we inserted earlier
    UPDATE [dbo].[BookCopies] 
    SET CampusID = 2, ShelfCode = '010' 
    WHERE BookID = @BookID_Scen5;
END
GO
