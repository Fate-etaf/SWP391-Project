USE [LMSVer2];
GO
SET QUOTED_IDENTIFIER ON;
GO

-- We need 3 books for our scenarios.
DECLARE @BookID_Scen2 INT; -- All borrowed, SE001 NOT on waitlist
DECLARE @BookID_Scen3 INT; -- All borrowed, SE001 IS #2 on waitlist
DECLARE @BookID_Scen4 INT; -- 1 copy returned, SE001 is Notified

-- Get books that have 3 available copies and haven't been touched yet
WITH CTE AS (
    SELECT BookID, ROW_NUMBER() OVER (ORDER BY BookID) as rn
    FROM [dbo].[BookCopies]
    WHERE CopyStatus = 'Available'
    GROUP BY BookID
    HAVING COUNT(*) = 3
)
SELECT 
    @BookID_Scen2 = MAX(CASE WHEN rn = 1 THEN BookID END),
    @BookID_Scen3 = MAX(CASE WHEN rn = 2 THEN BookID END),
    @BookID_Scen4 = MAX(CASE WHEN rn = 3 THEN BookID END)
FROM CTE;

PRINT 'Scenario 1 (Book has available copies): Pick any other book, e.g., BookID = 2';
PRINT 'Scenario 2 (All copies borrowed, not on waitlist): BookID = ' + ISNULL(CAST(@BookID_Scen2 AS VARCHAR), 'N/A');
PRINT 'Scenario 3 (All copies borrowed, SE001 is #2 in line): BookID = ' + ISNULL(CAST(@BookID_Scen3 AS VARCHAR), 'N/A');
PRINT 'Scenario 4 (Copy returned, SE001 is notified to pick up): BookID = ' + ISNULL(CAST(@BookID_Scen4 AS VARCHAR), 'N/A');

---------------------------------------------------------------------------
-- SCENARIO 2: All copies borrowed, SE001 NOT on waitlist
---------------------------------------------------------------------------
IF @BookID_Scen2 IS NOT NULL
BEGIN
    UPDATE [dbo].[BookCopies] SET CopyStatus = 'Borrowed' WHERE BookID = @BookID_Scen2;
    
    DECLARE @Ticket1 INT;
    INSERT [dbo].[BorrowTickets] (PatronID, LibrarianID, CampusID, CreatedAt, Note) VALUES ('HE190001', 'HE190003', 1, GETDATE(), 'Scen2 Setup');
    SET @Ticket1 = SCOPE_IDENTITY();
    
    INSERT [dbo].[BorrowTicketDetails] (TicketID, CopyID, DueDate, Status, RenewalCount)
    SELECT @Ticket1, CopyID, DATEADD(day, 14, GETDATE()), 'Borrowing', 0 FROM [dbo].[BookCopies] WHERE BookID = @BookID_Scen2;
END

---------------------------------------------------------------------------
-- SCENARIO 3: All copies borrowed, SE001 IS #2 on waitlist
---------------------------------------------------------------------------
IF @BookID_Scen3 IS NOT NULL
BEGIN
    UPDATE [dbo].[BookCopies] SET CopyStatus = 'Borrowed' WHERE BookID = @BookID_Scen3;
    
    DECLARE @Ticket2 INT;
    INSERT [dbo].[BorrowTickets] (PatronID, LibrarianID, CampusID, CreatedAt, Note) VALUES ('HE190002', 'HE190003', 1, GETDATE(), 'Scen3 Setup');
    SET @Ticket2 = SCOPE_IDENTITY();
    
    INSERT [dbo].[BorrowTicketDetails] (TicketID, CopyID, DueDate, Status, RenewalCount)
    SELECT @Ticket2, CopyID, DATEADD(day, 14, GETDATE()), 'Borrowing', 0 FROM [dbo].[BookCopies] WHERE BookID = @BookID_Scen3;

    -- Add HE190004 to waitlist first (Position #1)
    INSERT [dbo].[Waitlists] (BookID, PatronID, CampusID, RequestedAt, Status)
    VALUES (@BookID_Scen3, 'HE190004', 1, DATEADD(hour, -2, GETDATE()), 'Waiting');

    -- Add SE001 to waitlist second (Position #2)
    INSERT [dbo].[Waitlists] (BookID, PatronID, CampusID, RequestedAt, Status)
    VALUES (@BookID_Scen3, 'SE001', 1, GETDATE(), 'Waiting');
END

---------------------------------------------------------------------------
-- SCENARIO 4: Copy available, SE001 is Notified
---------------------------------------------------------------------------
IF @BookID_Scen4 IS NOT NULL
BEGIN
    -- Borrow only 2 copies out of 3. Leave 1 Available.
    DECLARE @AvailableCopy VARCHAR(50);
    SELECT TOP 1 @AvailableCopy = CopyID FROM [dbo].[BookCopies] WHERE BookID = @BookID_Scen4;

    UPDATE [dbo].[BookCopies] SET CopyStatus = 'Borrowed' WHERE BookID = @BookID_Scen4 AND CopyID <> @AvailableCopy;
    
    DECLARE @Ticket3 INT;
    INSERT [dbo].[BorrowTickets] (PatronID, LibrarianID, CampusID, CreatedAt, Note) VALUES ('HE190004', 'HE190003', 1, GETDATE(), 'Scen4 Setup');
    SET @Ticket3 = SCOPE_IDENTITY();
    
    INSERT [dbo].[BorrowTicketDetails] (TicketID, CopyID, DueDate, Status, RenewalCount)
    SELECT @Ticket3, CopyID, DATEADD(day, 14, GETDATE()), 'Borrowing', 0 FROM [dbo].[BookCopies] WHERE BookID = @BookID_Scen4 AND CopyID <> @AvailableCopy;

    -- SE001 is Notified (A copy has been kept for them)
    INSERT [dbo].[Waitlists] (BookID, PatronID, CampusID, RequestedAt, NotifiedAt, Status)
    VALUES (@BookID_Scen4, 'SE001', 1, DATEADD(day, -2, GETDATE()), DATEADD(hour, -1, GETDATE()), 'Notified');
END
GO
