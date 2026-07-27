USE [LMSVer2];
GO

-- 1. Drop DefaultShelfCode (Check if it exists first)
IF COL_LENGTH('dbo.Books', 'DefaultShelfCode') IS NOT NULL
BEGIN
    ALTER TABLE [dbo].[Books] DROP COLUMN [DefaultShelfCode];
    PRINT 'Dropped DefaultShelfCode';
END
GO

-- 2. Add MajorID (Check if it doesn't exist)
IF COL_LENGTH('dbo.Books', 'MajorID') IS NULL
BEGIN
    ALTER TABLE [dbo].[Books] ADD [MajorID] INT NULL;
    PRINT 'Added MajorID';
END
GO

-- 3. Add Foreign Key
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Books_Majors')
BEGIN
    ALTER TABLE [dbo].[Books] ADD CONSTRAINT FK_Books_Majors FOREIGN KEY (MajorID) REFERENCES [dbo].[Majors](MajorID);
    PRINT 'Added FK_Books_Majors';
END
GO

-- 4. Backfill ShelfCode in Books
UPDATE b
SET b.ShelfCode = (
    SELECT TOP 1 bc.ShelfCode 
    FROM [dbo].[BookCopies] bc 
    WHERE bc.BookID = b.BookID AND bc.ShelfCode IS NOT NULL 
    ORDER BY CASE WHEN bc.CopyStatus = 'Available' THEN 0 ELSE 1 END
)
FROM [dbo].[Books] b
WHERE b.ShelfCode IS NULL;
PRINT 'Backfilled ShelfCode in Books';
GO

-- 5. Backfill MajorID in Books based on SubjectCode and MajorSubjects
UPDATE b
SET b.MajorID = (
    SELECT TOP 1 ms.MajorID
    FROM [dbo].[MajorSubjects] ms
    WHERE ms.SubjectCode = b.SubjectCode
)
FROM [dbo].[Books] b
WHERE b.MajorID IS NULL AND b.SubjectCode IS NOT NULL;
PRINT 'Backfilled MajorID in Books';
GO
