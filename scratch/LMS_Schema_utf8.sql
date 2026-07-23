USE [master]
GO
/****** Object:  Database [LMSVer2]    Script Date: 6/14/2026 8:24:17 PM ******/
CREATE DATABASE [LMSVer2]
 CONTAINMENT = NONE
 ON  PRIMARY 
( NAME = N'LMSVer2', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL16.MSSQLSERVER\MSSQL\DATA\LMSVer2.mdf' , SIZE = 8192KB , MAXSIZE = UNLIMITED, FILEGROWTH = 65536KB )
 LOG ON 
( NAME = N'LMSVer2_log', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL16.MSSQLSERVER\MSSQL\DATA\LMSVer2_log.ldf' , SIZE = 8192KB , MAXSIZE = 2048GB , FILEGROWTH = 65536KB )
 WITH CATALOG_COLLATION = DATABASE_DEFAULT, LEDGER = OFF
GO
ALTER DATABASE [LMSVer2] SET COMPATIBILITY_LEVEL = 160
GO
IF (1 = FULLTEXTSERVICEPROPERTY('IsFullTextInstalled'))
begin
EXEC [LMSVer2].[dbo].[sp_fulltext_database] @action = 'enable'
end
GO
ALTER DATABASE [LMSVer2] SET ANSI_NULL_DEFAULT OFF 
GO
ALTER DATABASE [LMSVer2] SET ANSI_NULLS OFF 
GO
ALTER DATABASE [LMSVer2] SET ANSI_PADDING OFF 
GO
ALTER DATABASE [LMSVer2] SET ANSI_WARNINGS OFF 
GO
ALTER DATABASE [LMSVer2] SET ARITHABORT OFF 
GO
ALTER DATABASE [LMSVer2] SET AUTO_CLOSE ON 
GO
ALTER DATABASE [LMSVer2] SET AUTO_SHRINK OFF 
GO
ALTER DATABASE [LMSVer2] SET AUTO_UPDATE_STATISTICS ON 
GO
ALTER DATABASE [LMSVer2] SET CURSOR_CLOSE_ON_COMMIT OFF 
GO
ALTER DATABASE [LMSVer2] SET CURSOR_DEFAULT  GLOBAL 
GO
ALTER DATABASE [LMSVer2] SET CONCAT_NULL_YIELDS_NULL OFF 
GO
ALTER DATABASE [LMSVer2] SET NUMERIC_ROUNDABORT OFF 
GO
ALTER DATABASE [LMSVer2] SET QUOTED_IDENTIFIER OFF 
GO
ALTER DATABASE [LMSVer2] SET RECURSIVE_TRIGGERS OFF 
GO
ALTER DATABASE [LMSVer2] SET  ENABLE_BROKER 
GO
ALTER DATABASE [LMSVer2] SET AUTO_UPDATE_STATISTICS_ASYNC OFF 
GO
ALTER DATABASE [LMSVer2] SET DATE_CORRELATION_OPTIMIZATION OFF 
GO
ALTER DATABASE [LMSVer2] SET TRUSTWORTHY OFF 
GO
ALTER DATABASE [LMSVer2] SET ALLOW_SNAPSHOT_ISOLATION OFF 
GO
ALTER DATABASE [LMSVer2] SET PARAMETERIZATION SIMPLE 
GO
ALTER DATABASE [LMSVer2] SET READ_COMMITTED_SNAPSHOT OFF 
GO
ALTER DATABASE [LMSVer2] SET HONOR_BROKER_PRIORITY OFF 
GO
ALTER DATABASE [LMSVer2] SET RECOVERY SIMPLE 
GO
ALTER DATABASE [LMSVer2] SET  MULTI_USER 
GO
ALTER DATABASE [LMSVer2] SET PAGE_VERIFY CHECKSUM  
GO
ALTER DATABASE [LMSVer2] SET DB_CHAINING OFF 
GO
ALTER DATABASE [LMSVer2] SET FILESTREAM( NON_TRANSACTED_ACCESS = OFF ) 
GO
ALTER DATABASE [LMSVer2] SET TARGET_RECOVERY_TIME = 60 SECONDS 
GO
ALTER DATABASE [LMSVer2] SET DELAYED_DURABILITY = DISABLED 
GO
ALTER DATABASE [LMSVer2] SET ACCELERATED_DATABASE_RECOVERY = OFF  
GO
ALTER DATABASE [LMSVer2] SET QUERY_STORE = ON
GO
ALTER DATABASE [LMSVer2] SET QUERY_STORE (OPERATION_MODE = READ_WRITE, CLEANUP_POLICY = (STALE_QUERY_THRESHOLD_DAYS = 30), DATA_FLUSH_INTERVAL_SECONDS = 900, INTERVAL_LENGTH_MINUTES = 60, MAX_STORAGE_SIZE_MB = 1000, QUERY_CAPTURE_MODE = AUTO, SIZE_BASED_CLEANUP_MODE = AUTO, MAX_PLANS_PER_QUERY = 200, WAIT_STATS_CAPTURE_MODE = ON)
GO
USE [LMSVer2]
GO
/****** Object:  Table [dbo].[Authors]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Authors](
	[AuthorID] [int] IDENTITY(1,1) NOT NULL,
	[AuthorName] [nvarchar](150) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[AuthorID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[BookAuthors]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BookAuthors](
	[BookID] [int] NOT NULL,
	[AuthorID] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[BookID] ASC,
	[AuthorID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[BookCategories]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BookCategories](
	[BookID] [int] NOT NULL,
	[CategoryID] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[BookID] ASC,
	[CategoryID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[BookCopies]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BookCopies](
	[CopyID] [varchar](30) NOT NULL,
	[BookID] [int] NOT NULL,
	[CampusID] [int] NOT NULL,
	[ShelfCode] [varchar](50) NOT NULL,
	[ConditionStatus] [varchar](20) NOT NULL,
	[CopyStatus] [varchar](20) NOT NULL,
	[AcquiredAt] [datetime] NULL,
	[QRCode] [varchar](30) NULL,
PRIMARY KEY CLUSTERED 
(
	[CopyID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Books]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Books](
	[BookID] [int] IDENTITY(1,1) NOT NULL,
	[SubjectCode] [varchar](20) NULL,
	[ISBN] [varchar](20) NULL,
	[Title] [nvarchar](300) NOT NULL,
	[PublisherID] [int] NULL,
	[PublishYear] [int] NULL,
	[Edition] [varchar](50) NULL,
	[Language] [varchar](50) NOT NULL,
	[Description] [nvarchar](max) NULL,
	[CoverImageURL] [varchar](500) NULL,
	[DefaultShelfCode] [varchar](50) NULL,
	[CreatedAt] [datetime] NOT NULL,
	[ShelfCode] [varchar](50) NULL,
PRIMARY KEY CLUSTERED 
(
	[BookID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[ISBN] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[BorrowTicketDetails]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BorrowTicketDetails](
	[TicketDetailID] [int] IDENTITY(1,1) NOT NULL,
	[TicketID] [int] NOT NULL,
	[CopyID] [varchar](30) NOT NULL,
	[DueDate] [datetime] NOT NULL,
	[ReturnDate] [datetime] NULL,
	[RenewalCount] [int] NOT NULL,
	[Status] [varchar](255) NULL,
	[ReturnCampusID] [int] NULL,
PRIMARY KEY CLUSTERED 
(
	[TicketDetailID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[BorrowTickets]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BorrowTickets](
	[TicketID] [int] IDENTITY(1,1) NOT NULL,
	[PatronID] [varchar](20) NOT NULL,
	[LibrarianID] [varchar](20) NOT NULL,
	[CampusID] [int] NOT NULL,
	[CreatedAt] [datetime] NOT NULL,
	[Note] [varchar](255) NULL,
PRIMARY KEY CLUSTERED 
(
	[TicketID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Campuses]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Campuses](
	[CampusID] [int] IDENTITY(1,1) NOT NULL,
	[CampusName] [nvarchar](100) NOT NULL,
	[Address] [nvarchar](255) NOT NULL,
	[Phone] [varchar](20) NULL,
PRIMARY KEY CLUSTERED 
(
	[CampusID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Categories]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Categories](
	[CategoryID] [int] IDENTITY(1,1) NOT NULL,
	[CategoryName] [varchar](100) NOT NULL,
	[Description] [nvarchar](500) NULL,
PRIMARY KEY CLUSTERED 
(
	[CategoryID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[FineInvoices]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[FineInvoices](
	[FineID] [int] IDENTITY(1,1) NOT NULL,
	[PatronID] [varchar](20) NOT NULL,
	[TicketDetailID] [int] NOT NULL,
	[FineAmount] [numeric](38, 2) NULL,
	[RemainingAmount] [numeric](38, 2) NULL,
	[ViolationType] [varchar](255) NULL,
	[Reason] [varchar](255) NULL,
	[CreatedAt] [datetime] NOT NULL,
	[PaidAt] [datetime] NULL,
	[PaidStatus] [varchar](255) NULL,
	[ProcessedBy] [varchar](20) NULL,
PRIMARY KEY CLUSTERED 
(
	[FineID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Majors]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Majors](
	[MajorID] [int] IDENTITY(1,1) NOT NULL,
	[MajorCode] [varchar](50) NOT NULL,
	[MajorName] [varchar](200) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[MajorID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
 CONSTRAINT [UKgwibpsu1b1qbsegirmtncl6i5] UNIQUE NONCLUSTERED 
(
	[MajorCode] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[MajorSubjects]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[MajorSubjects](
	[MajorID] [int] NOT NULL,
	[SubjectCode] [varchar](20) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[MajorID] ASC,
	[SubjectCode] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[MaterialRequests]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[MaterialRequests](
	[RequestID] [int] IDENTITY(1,1) NOT NULL,
	[PatronID] [varchar](20) NOT NULL,
	[Title] [nvarchar](300) NOT NULL,
	[ISBN] [varchar](20) NULL,
	[Author] [nvarchar](200) NULL,
	[Language] [nvarchar](50) NULL,
	[BookLink] [nvarchar](500) NULL,
	[RequestUrgency] [nvarchar](100) NULL,
	[Reason] [nvarchar](1000) NOT NULL,
	[Email] [varchar](255) NOT NULL,
	[Status] [varchar](20) NOT NULL,
	[Feedback] [nvarchar](1000) NULL,
	[CreatedAt] [datetime] NOT NULL,
	[ReviewedBy] [nvarchar](50) NULL,
	[ReviewedAt] [datetime] NULL,
PRIMARY KEY CLUSTERED 
(
	[RequestID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Notifications]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Notifications](
	[NotificationID] [int] IDENTITY(1,1) NOT NULL,
	[UserID] [varchar](20) NOT NULL,
	[NotificationType] [varchar](50) NOT NULL,
	[Title] [nvarchar](200) NOT NULL,
	[Content] [nvarchar](max) NULL,
	[Status] [varchar](20) NOT NULL,
	[SentAt] [datetime] NULL,
	[CreatedAt] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[NotificationID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Publishers]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Publishers](
	[PublisherID] [int] IDENTITY(1,1) NOT NULL,
	[PublisherName] [nvarchar](150) NOT NULL,
	[Address] [nvarchar](255) NULL,
	[Phone] [varchar](20) NULL,
PRIMARY KEY CLUSTERED 
(
	[PublisherID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Reservations]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Reservations](
	[ReservationID] [int] IDENTITY(1,1) NOT NULL,
	[PatronID] [varchar](20) NOT NULL,
	[BookID] [int] NOT NULL,
	[CopyID] [varchar](30) NULL,
	[PickupCampusID] [int] NOT NULL,
	[ReservedAt] [datetime] NOT NULL,
	[ExpirationDate] [datetime] NOT NULL,
	[Status] [varchar](20) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[ReservationID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Roles]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Roles](
	[RoleID] [int] IDENTITY(1,1) NOT NULL,
	[RoleName] [varchar](255) NULL,
PRIMARY KEY CLUSTERED 
(
	[RoleID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[RoleName] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[RoomBookings]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[RoomBookings](
	[BookingID] [int] IDENTITY(1,1) NOT NULL,
	[RoomID] [int] NOT NULL,
	[PatronID] [varchar](20) NOT NULL,
	[BookingDate] [date] NOT NULL,
	[StartTime] [time](0) NOT NULL,
	[EndTime] [time](0) NOT NULL,
	[Purpose] [nvarchar](300) NULL,
	[ParticipantCount] [int] NULL,
	[Status] [varchar](20) NOT NULL,
	[QRCode] [image] NULL,
	[CreatedAt] [datetime] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[BookingID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Shelves]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Shelves](
	[ShelfCode] [varchar](50) NOT NULL,
	[ShelfNumber] [int] NOT NULL,
	[CampusID] [int] NOT NULL,
	[ShelfName] [nvarchar](100) NULL,
	[ShelfCodeTopic] [nvarchar](100) NULL,
PRIMARY KEY CLUSTERED 
(
	[ShelfCode] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
 CONSTRAINT [UKm5pjidtk8ep68bnqg3xhqxaqi] UNIQUE NONCLUSTERED 
(
	[CampusID] ASC,
	[ShelfCode] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
 CONSTRAINT [UQ_Shelf] UNIQUE NONCLUSTERED 
(
	[CampusID] ASC,
	[ShelfCode] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[StudyRooms]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StudyRooms](
	[RoomID] [int] IDENTITY(1,1) NOT NULL,
	[CampusID] [int] NOT NULL,
	[RoomName] [nvarchar](100) NOT NULL,
	[Capacity] [int] NOT NULL,
	[Description] [nvarchar](500) NULL,
	[Status] [varchar](20) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[RoomID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Subjects]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Subjects](
	[SubjectID] [int] IDENTITY(1,1) NOT NULL,
	[SubjectCode] [varchar](20) NOT NULL,
	[SubjectName] [nvarchar](200) NOT NULL,
	[Description] [nvarchar](500) NULL,
PRIMARY KEY CLUSTERED 
(
	[SubjectID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[SubjectCode] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SystemConfig]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SystemConfig](
	[ConfigKey] [varchar](100) NOT NULL,
	[ConfigValue] [nvarchar](500) NOT NULL,
	[Description] [nvarchar](300) NULL,
	[UpdatedAt] [datetime] NOT NULL,
	[UpdatedBy] [varchar](20) NULL,
PRIMARY KEY CLUSTERED 
(
	[ConfigKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[TransferDetails]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[TransferDetails](
	[TransferID] [int] NOT NULL,
	[CopyID] [varchar](30) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[TransferID] ASC,
	[CopyID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[TransferRequests]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[TransferRequests](
	[TransferID] [int] IDENTITY(1,1) NOT NULL,
	[RequestedBy] [varchar](20) NOT NULL,
	[FromCampusID] [int] NOT NULL,
	[ToCampusID] [int] NOT NULL,
	[RequestedAt] [datetime] NOT NULL,
	[ShippedAt] [datetime] NULL,
	[ReceivedAt] [datetime] NULL,
	[ConfirmedBy] [varchar](20) NULL,
	[Status] [varchar](50) NULL,
	[Note] [nvarchar](max) NULL,
PRIMARY KEY CLUSTERED 
(
	[TransferID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[UserRoles]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[UserRoles](
	[UserID] [varchar](20) NOT NULL,
	[RoleID] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[UserID] ASC,
	[RoleID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Users]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Users](
	[UserID] [varchar](20) NOT NULL,
	[FullName] [nvarchar](150) NOT NULL,
	[Email] [varchar](150) NOT NULL,
	[PasswordHash] [varchar](255) NOT NULL,
	[Phone] [varchar](20) NULL,
	[CampusID] [int] NOT NULL,
	[RoleID] [int] NOT NULL,
	[Status] [varchar](20) NOT NULL,
	[BorrowingLocked] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[UserID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[Email] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Waitlists]    Script Date: 6/14/2026 8:24:17 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Waitlists](
	[WaitlistID] [int] IDENTITY(1,1) NOT NULL,
	[BookID] [int] NOT NULL,
	[PatronID] [varchar](20) NOT NULL,
	[CampusID] [int] NOT NULL,
	[RequestedAt] [datetime] NOT NULL,
	[NotifiedAt] [datetime] NULL,
	[Status] [varchar](20) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[WaitlistID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Index [IX_BookCopies_BookID]    Script Date: 6/14/2026 8:24:17 PM ******/
CREATE NONCLUSTERED INDEX [IX_BookCopies_BookID] ON [dbo].[BookCopies]
(
	[BookID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [IX_BookCopies_Status]    Script Date: 6/14/2026 8:24:17 PM ******/
CREATE NONCLUSTERED INDEX [IX_BookCopies_Status] ON [dbo].[BookCopies]
(
	[CopyStatus] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
/****** Object:  Index [IX_BorrowTicketDetails_DueDate]    Script Date: 6/14/2026 8:24:17 PM ******/
CREATE NONCLUSTERED INDEX [IX_BorrowTicketDetails_DueDate] ON [dbo].[BorrowTicketDetails]
(
	[DueDate] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [IX_BorrowTicketDetails_Status]    Script Date: 6/14/2026 8:24:17 PM ******/
CREATE NONCLUSTERED INDEX [IX_BorrowTicketDetails_Status] ON [dbo].[BorrowTicketDetails]
(
	[Status] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [IX_FineInvoices_Status]    Script Date: 6/14/2026 8:24:17 PM ******/
CREATE NONCLUSTERED INDEX [IX_FineInvoices_Status] ON [dbo].[FineInvoices]
(
	[PaidStatus] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [IX_Notifications_Status]    Script Date: 6/14/2026 8:24:17 PM ******/
CREATE NONCLUSTERED INDEX [IX_Notifications_Status] ON [dbo].[Notifications]
(
	[Status] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [IX_Reservations_Status]    Script Date: 6/14/2026 8:24:17 PM ******/
CREATE NONCLUSTERED INDEX [IX_Reservations_Status] ON [dbo].[Reservations]
(
	[Status] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [IX_Waitlist_Active]    Script Date: 6/14/2026 8:24:17 PM ******/
CREATE UNIQUE NONCLUSTERED INDEX [IX_Waitlist_Active] ON [dbo].[Waitlists]
(
	[BookID] ASC,
	[PatronID] ASC
)
WHERE ([Status] IN ('Waiting', 'Notified'))
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [IX_Waitlists_Status]    Script Date: 6/14/2026 8:24:17 PM ******/
CREATE NONCLUSTERED INDEX [IX_Waitlists_Status] ON [dbo].[Waitlists]
(
	[Status] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
ALTER TABLE [dbo].[BookCopies] ADD  DEFAULT ('Good') FOR [ConditionStatus]
GO
ALTER TABLE [dbo].[BookCopies] ADD  DEFAULT ('Available') FOR [CopyStatus]
GO
ALTER TABLE [dbo].[Books] ADD  DEFAULT ('Vietnamese') FOR [Language]
GO
ALTER TABLE [dbo].[Books] ADD  DEFAULT (getdate()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[BorrowTicketDetails] ADD  DEFAULT ((0)) FOR [RenewalCount]
GO
ALTER TABLE [dbo].[BorrowTicketDetails] ADD  DEFAULT ('Borrowing') FOR [Status]
GO
ALTER TABLE [dbo].[BorrowTickets] ADD  DEFAULT (getdate()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[FineInvoices] ADD  DEFAULT (getdate()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[FineInvoices] ADD  DEFAULT ('Unpaid') FOR [PaidStatus]
GO
ALTER TABLE [dbo].[MaterialRequests] ADD  DEFAULT ('Pending') FOR [Status]
GO
ALTER TABLE [dbo].[MaterialRequests] ADD  DEFAULT (getdate()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Notifications] ADD  DEFAULT ('Pending') FOR [Status]
GO
ALTER TABLE [dbo].[Notifications] ADD  DEFAULT (getdate()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Reservations] ADD  DEFAULT (getdate()) FOR [ReservedAt]
GO
ALTER TABLE [dbo].[Reservations] ADD  DEFAULT ('Holding') FOR [Status]
GO
ALTER TABLE [dbo].[RoomBookings] ADD  DEFAULT ('Confirmed') FOR [Status]
GO
ALTER TABLE [dbo].[RoomBookings] ADD  DEFAULT (getdate()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[StudyRooms] ADD  DEFAULT ((1)) FOR [Capacity]
GO
ALTER TABLE [dbo].[StudyRooms] ADD  DEFAULT ('Available') FOR [Status]
GO
ALTER TABLE [dbo].[SystemConfig] ADD  DEFAULT (getdate()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[TransferRequests] ADD  DEFAULT (getdate()) FOR [RequestedAt]
GO
ALTER TABLE [dbo].[TransferRequests] ADD  DEFAULT ('Pending') FOR [Status]
GO
ALTER TABLE [dbo].[Users] ADD  DEFAULT ('Active') FOR [Status]
GO
ALTER TABLE [dbo].[Users] ADD  DEFAULT ((0)) FOR [BorrowingLocked]
GO
ALTER TABLE [dbo].[Waitlists] ADD  DEFAULT (getdate()) FOR [RequestedAt]
GO
ALTER TABLE [dbo].[Waitlists] ADD  DEFAULT ('Waiting') FOR [Status]
GO
ALTER TABLE [dbo].[BookAuthors]  WITH CHECK ADD FOREIGN KEY([AuthorID])
REFERENCES [dbo].[Authors] ([AuthorID])
GO
ALTER TABLE [dbo].[BookAuthors]  WITH CHECK ADD FOREIGN KEY([BookID])
REFERENCES [dbo].[Books] ([BookID])
GO
ALTER TABLE [dbo].[BookCategories]  WITH CHECK ADD FOREIGN KEY([BookID])
REFERENCES [dbo].[Books] ([BookID])
GO
ALTER TABLE [dbo].[BookCategories]  WITH CHECK ADD FOREIGN KEY([CategoryID])
REFERENCES [dbo].[Categories] ([CategoryID])
GO
ALTER TABLE [dbo].[BookCopies]  WITH CHECK ADD FOREIGN KEY([BookID])
REFERENCES [dbo].[Books] ([BookID])
GO
ALTER TABLE [dbo].[BookCopies]  WITH CHECK ADD FOREIGN KEY([CampusID])
REFERENCES [dbo].[Campuses] ([CampusID])
GO
ALTER TABLE [dbo].[BookCopies]  WITH CHECK ADD FOREIGN KEY([ShelfCode])
REFERENCES [dbo].[Shelves] ([ShelfCode])
GO
ALTER TABLE [dbo].[Books]  WITH CHECK ADD FOREIGN KEY([PublisherID])
REFERENCES [dbo].[Publishers] ([PublisherID])
GO
ALTER TABLE [dbo].[Books]  WITH CHECK ADD FOREIGN KEY([SubjectCode])
REFERENCES [dbo].[Subjects] ([SubjectCode])
GO
ALTER TABLE [dbo].[BorrowTicketDetails]  WITH CHECK ADD FOREIGN KEY([CopyID])
REFERENCES [dbo].[BookCopies] ([CopyID])
GO
ALTER TABLE [dbo].[BorrowTicketDetails]  WITH CHECK ADD FOREIGN KEY([ReturnCampusID])
REFERENCES [dbo].[Campuses] ([CampusID])
GO
ALTER TABLE [dbo].[BorrowTicketDetails]  WITH CHECK ADD FOREIGN KEY([TicketID])
REFERENCES [dbo].[BorrowTickets] ([TicketID])
GO
ALTER TABLE [dbo].[BorrowTickets]  WITH CHECK ADD FOREIGN KEY([CampusID])
REFERENCES [dbo].[Campuses] ([CampusID])
GO
ALTER TABLE [dbo].[BorrowTickets]  WITH CHECK ADD FOREIGN KEY([LibrarianID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[BorrowTickets]  WITH CHECK ADD FOREIGN KEY([PatronID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[FineInvoices]  WITH CHECK ADD FOREIGN KEY([PatronID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[FineInvoices]  WITH CHECK ADD FOREIGN KEY([ProcessedBy])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[FineInvoices]  WITH CHECK ADD FOREIGN KEY([TicketDetailID])
REFERENCES [dbo].[BorrowTicketDetails] ([TicketDetailID])
GO
ALTER TABLE [dbo].[MajorSubjects]  WITH CHECK ADD  CONSTRAINT [FKb7gngx5imxvv08ivg8wtdeu5i] FOREIGN KEY([MajorID])
REFERENCES [dbo].[Majors] ([MajorID])
GO
ALTER TABLE [dbo].[MajorSubjects] CHECK CONSTRAINT [FKb7gngx5imxvv08ivg8wtdeu5i]
GO
ALTER TABLE [dbo].[MaterialRequests]  WITH CHECK ADD  CONSTRAINT [FK_NewBookRequests_User] FOREIGN KEY([PatronID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[MaterialRequests] CHECK CONSTRAINT [FK_NewBookRequests_User]
GO
ALTER TABLE [dbo].[Notifications]  WITH CHECK ADD FOREIGN KEY([UserID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[Reservations]  WITH CHECK ADD FOREIGN KEY([BookID])
REFERENCES [dbo].[Books] ([BookID])
GO
ALTER TABLE [dbo].[Reservations]  WITH CHECK ADD FOREIGN KEY([CopyID])
REFERENCES [dbo].[BookCopies] ([CopyID])
GO
ALTER TABLE [dbo].[Reservations]  WITH CHECK ADD FOREIGN KEY([PatronID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[Reservations]  WITH CHECK ADD FOREIGN KEY([PickupCampusID])
REFERENCES [dbo].[Campuses] ([CampusID])
GO
ALTER TABLE [dbo].[RoomBookings]  WITH CHECK ADD FOREIGN KEY([PatronID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[RoomBookings]  WITH CHECK ADD FOREIGN KEY([RoomID])
REFERENCES [dbo].[StudyRooms] ([RoomID])
GO
ALTER TABLE [dbo].[Shelves]  WITH CHECK ADD FOREIGN KEY([CampusID])
REFERENCES [dbo].[Campuses] ([CampusID])
GO
ALTER TABLE [dbo].[StudyRooms]  WITH CHECK ADD FOREIGN KEY([CampusID])
REFERENCES [dbo].[Campuses] ([CampusID])
GO
ALTER TABLE [dbo].[SystemConfig]  WITH CHECK ADD FOREIGN KEY([UpdatedBy])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[TransferDetails]  WITH CHECK ADD FOREIGN KEY([CopyID])
REFERENCES [dbo].[BookCopies] ([CopyID])
GO
ALTER TABLE [dbo].[TransferDetails]  WITH CHECK ADD FOREIGN KEY([TransferID])
REFERENCES [dbo].[TransferRequests] ([TransferID])
GO
ALTER TABLE [dbo].[TransferRequests]  WITH CHECK ADD FOREIGN KEY([ConfirmedBy])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[TransferRequests]  WITH CHECK ADD FOREIGN KEY([FromCampusID])
REFERENCES [dbo].[Campuses] ([CampusID])
GO
ALTER TABLE [dbo].[TransferRequests]  WITH CHECK ADD FOREIGN KEY([RequestedBy])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[TransferRequests]  WITH CHECK ADD FOREIGN KEY([ToCampusID])
REFERENCES [dbo].[Campuses] ([CampusID])
GO
ALTER TABLE [dbo].[UserRoles]  WITH CHECK ADD FOREIGN KEY([RoleID])
REFERENCES [dbo].[Roles] ([RoleID])
GO
ALTER TABLE [dbo].[UserRoles]  WITH CHECK ADD FOREIGN KEY([UserID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[Users]  WITH CHECK ADD FOREIGN KEY([CampusID])
REFERENCES [dbo].[Campuses] ([CampusID])
GO
ALTER TABLE [dbo].[Users]  WITH CHECK ADD FOREIGN KEY([RoleID])
REFERENCES [dbo].[Roles] ([RoleID])
GO
ALTER TABLE [dbo].[Waitlists]  WITH CHECK ADD FOREIGN KEY([BookID])
REFERENCES [dbo].[Books] ([BookID])
GO
ALTER TABLE [dbo].[Waitlists]  WITH CHECK ADD FOREIGN KEY([CampusID])
REFERENCES [dbo].[Campuses] ([CampusID])
GO
ALTER TABLE [dbo].[Waitlists]  WITH CHECK ADD FOREIGN KEY([PatronID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[BookCopies]  WITH CHECK ADD  CONSTRAINT [CHK_CopyStatus] CHECK  (([CopyStatus]='Lost' OR [CopyStatus]='Maintenance' OR [CopyStatus]='InTransfer' OR [CopyStatus]='Reserved' OR [CopyStatus]='Borrowed' OR [CopyStatus]='Available'))
GO
ALTER TABLE [dbo].[BookCopies] CHECK CONSTRAINT [CHK_CopyStatus]
GO
ALTER TABLE [dbo].[BookCopies]  WITH CHECK ADD CHECK  (([ConditionStatus]='Lost' OR [ConditionStatus]='Damaged' OR [ConditionStatus]='Fair' OR [ConditionStatus]='Good' OR [ConditionStatus]='New'))
GO
ALTER TABLE [dbo].[BorrowTicketDetails]  WITH CHECK ADD CHECK  (([Status]='Damaged' OR [Status]='Lost' OR [Status]='Overdue' OR [Status]='Returned' OR [Status]='Borrowing'))
GO
ALTER TABLE [dbo].[FineInvoices]  WITH CHECK ADD CHECK  (([PaidStatus]='Waived' OR [PaidStatus]='Partial' OR [PaidStatus]='Paid' OR [PaidStatus]='Unpaid'))
GO
ALTER TABLE [dbo].[FineInvoices]  WITH CHECK ADD CHECK  (([ViolationType]='Damaged' OR [ViolationType]='Lost' OR [ViolationType]='Overdue'))
GO
ALTER TABLE [dbo].[Notifications]  WITH CHECK ADD CHECK  (([Status]='Failed' OR [Status]='Sent' OR [Status]='Pending'))
GO
ALTER TABLE [dbo].[Reservations]  WITH CHECK ADD CHECK  (([Status]='Expired' OR [Status]='Cancelled' OR [Status]='Completed' OR [Status]='Holding'))
GO
ALTER TABLE [dbo].[RoomBookings]  WITH CHECK ADD  CONSTRAINT [CHK_RoomBooking_Time] CHECK  (([EndTime]>[StartTime]))
GO
ALTER TABLE [dbo].[RoomBookings] CHECK CONSTRAINT [CHK_RoomBooking_Time]
GO
ALTER TABLE [dbo].[RoomBookings]  WITH CHECK ADD CHECK  (([Status]='NoShow' OR [Status]='Cancelled' OR [Status]='CheckedOut' OR [Status]='CheckedIn' OR [Status]='Confirmed'))
GO
ALTER TABLE [dbo].[StudyRooms]  WITH CHECK ADD CHECK  (([Status]='Closed' OR [Status]='Maintenance' OR [Status]='Available'))
GO
ALTER TABLE [dbo].[TransferRequests]  WITH CHECK ADD CHECK  (([Status]='Cancelled' OR [Status]='Received' OR [Status]='InTransit' OR [Status]='Pending'))
GO
ALTER TABLE [dbo].[Users]  WITH CHECK ADD CHECK  (([Status]='Suspended' OR [Status]='Inactive' OR [Status]='Active'))
GO
ALTER TABLE [dbo].[Waitlists]  WITH CHECK ADD CHECK  (([Status]='Cancelled' OR [Status]='Converted' OR [Status]='Notified' OR [Status]='Waiting'))
GO
USE [master]
GO
ALTER DATABASE [LMSVer2] SET  READ_WRITE 
GO
