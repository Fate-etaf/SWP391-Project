USE [LMSVer2];
GO
SET QUOTED_IDENTIFIER ON;
GO

-- 1. Create user SE001 if not exists
IF NOT EXISTS (SELECT 1 FROM [dbo].[Users] WHERE UserID = 'SE001')
BEGIN
    INSERT [dbo].[Users] ([UserID], [FullName], [Email], [PasswordHash], [Phone], [CampusID], [RoleID], [Status], [BorrowingLocked]) 
    VALUES ('SE001', N'Sinh viên Test (SE001)', 'se001@fpt.edu.vn', 'hash123', '0999999999', 1, 1, 'Active', 0);
    
    INSERT [dbo].[UserRoles] ([UserID], [RoleID]) VALUES ('SE001', 1);
END
GO

-- Find 2 distinct books that have at least 3 copies
DECLARE @BookIDWaitlist INT, @BookIDRenewal INT;

SELECT TOP 1 @BookIDWaitlist = BookID 
FROM [dbo].[BookCopies] 
WHERE CopyStatus = 'Available'
GROUP BY BookID 
HAVING COUNT(*) >= 3;

SELECT TOP 1 @BookIDRenewal = BookID 
FROM [dbo].[BookCopies] 
WHERE CopyStatus = 'Available' AND BookID <> @BookIDWaitlist
GROUP BY BookID 
HAVING COUNT(*) >= 1;

-- 2. Setup Waitlist Test
IF @BookIDWaitlist IS NOT NULL
BEGIN
    DECLARE @TicketID1 INT, @TicketID2 INT, @TicketID3 INT;
    DECLARE @Copy1 VARCHAR(50), @Copy2 VARCHAR(50), @Copy3 VARCHAR(50);
    
    -- Get the 3 copies
    SELECT TOP 1 @Copy1 = CopyID FROM [dbo].[BookCopies] WHERE BookID = @BookIDWaitlist AND CopyStatus = 'Available';
    UPDATE [dbo].[BookCopies] SET CopyStatus = 'Borrowed' WHERE CopyID = @Copy1;
    
    SELECT TOP 1 @Copy2 = CopyID FROM [dbo].[BookCopies] WHERE BookID = @BookIDWaitlist AND CopyStatus = 'Available';
    UPDATE [dbo].[BookCopies] SET CopyStatus = 'Borrowed' WHERE CopyID = @Copy2;
    
    SELECT TOP 1 @Copy3 = CopyID FROM [dbo].[BookCopies] WHERE BookID = @BookIDWaitlist AND CopyStatus = 'Available';
    UPDATE [dbo].[BookCopies] SET CopyStatus = 'Borrowed' WHERE CopyID = @Copy3;

    -- Create Tickets for other users
    INSERT [dbo].[BorrowTickets] ([PatronID], [LibrarianID], [CampusID], [CreatedAt], [Note]) 
    VALUES ('HE190001', 'HE190003', 1, GETDATE(), 'Waitlist Setup 1');
    SET @TicketID1 = SCOPE_IDENTITY();

    INSERT [dbo].[BorrowTickets] ([PatronID], [LibrarianID], [CampusID], [CreatedAt], [Note]) 
    VALUES ('HE190002', 'HE190003', 1, GETDATE(), 'Waitlist Setup 2');
    SET @TicketID2 = SCOPE_IDENTITY();

    INSERT [dbo].[BorrowTickets] ([PatronID], [LibrarianID], [CampusID], [CreatedAt], [Note]) 
    VALUES ('HE190004', 'HE190003', 1, GETDATE(), 'Waitlist Setup 3');
    SET @TicketID3 = SCOPE_IDENTITY();

    -- Create Ticket Details
    INSERT [dbo].[BorrowTicketDetails] ([TicketID], [CopyID], [DueDate], [RenewalCount], [Status])
    VALUES (@TicketID1, @Copy1, DATEADD(day, 14, GETDATE()), 0, 'Borrowing');

    INSERT [dbo].[BorrowTicketDetails] ([TicketID], [CopyID], [DueDate], [RenewalCount], [Status])
    VALUES (@TicketID2, @Copy2, DATEADD(day, 14, GETDATE()), 0, 'Borrowing');

    INSERT [dbo].[BorrowTicketDetails] ([TicketID], [CopyID], [DueDate], [RenewalCount], [Status])
    VALUES (@TicketID3, @Copy3, DATEADD(day, 14, GETDATE()), 0, 'Borrowing');

    -- Add SE001 to Waitlist
    INSERT [dbo].[Waitlists] ([BookID], [PatronID], [CampusID], [RequestedAt], [Status])
    VALUES (@BookIDWaitlist, 'SE001', 1, GETDATE(), 'Waiting');
    
    PRINT 'Waitlist setup for BookID: ' + CAST(@BookIDWaitlist AS VARCHAR);
END
ELSE
BEGIN
    PRINT 'Could not find a book with 3 copies for waitlist setup.';
END

-- 3. Setup Renewal Test
IF @BookIDRenewal IS NOT NULL
BEGIN
    DECLARE @RenewalTicketID INT;
    DECLARE @RenewalCopy VARCHAR(50);

    -- Get 1 copy
    SELECT TOP 1 @RenewalCopy = CopyID FROM [dbo].[BookCopies] WHERE BookID = @BookIDRenewal AND CopyStatus = 'Available';
    UPDATE [dbo].[BookCopies] SET CopyStatus = 'Borrowed' WHERE CopyID = @RenewalCopy;

    -- Create Ticket for SE001
    INSERT [dbo].[BorrowTickets] ([PatronID], [LibrarianID], [CampusID], [CreatedAt], [Note]) 
    VALUES ('SE001', 'HE190003', 1, DATEADD(day, -13, GETDATE()), 'Renewal Setup');
    SET @RenewalTicketID = SCOPE_IDENTITY();

    -- Create Ticket Detail with DueDate = tomorrow
    INSERT [dbo].[BorrowTicketDetails] ([TicketID], [CopyID], [DueDate], [RenewalCount], [Status])
    VALUES (@RenewalTicketID, @RenewalCopy, DATEADD(day, 1, GETDATE()), 0, 'Borrowing');
    
    PRINT 'Renewal setup for BookID: ' + CAST(@BookIDRenewal AS VARCHAR) + ', CopyID: ' + @RenewalCopy;
END
ELSE
BEGIN
    PRINT 'Could not find a book for renewal setup.';
END
GO
