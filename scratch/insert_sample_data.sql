USE [LMSVer2]
GO
SET IDENTITY_INSERT [dbo].[Campuses] ON 

INSERT [dbo].[Campuses] ([CampusID], [CampusName], [Address], [Phone]) VALUES (1, N'FPT University Hà Nội', N'Khu Công nghệ cao Hòa Lạc, Thạch Thất, Hà Nội', N'02473005588')
INSERT [dbo].[Campuses] ([CampusID], [CampusName], [Address], [Phone]) VALUES (2, N'FPT University Đà Nẵng', N'Khu đô thị FPT City, Ngũ Hành Sơn, Đà Nẵng', N'02367300999')
INSERT [dbo].[Campuses] ([CampusID], [CampusName], [Address], [Phone]) VALUES (3, N'FPT University TP. Hồ Chí Minh', N'Lô E2a-7, Đường D1 Khu Công nghệ cao, Quận 9, TP.HCM', N'02873005588')
INSERT [dbo].[Campuses] ([CampusID], [CampusName], [Address], [Phone]) VALUES (4, N'FPT University Cần Thơ', N'Số 600 Đường Nguyễn Văn Cừ nối dài, An Bình, Ninh Kiều, Cần Thơ', N'02927303636')
INSERT [dbo].[Campuses] ([CampusID], [CampusName], [Address], [Phone]) VALUES (5, N'FPT University Quy Nhơn', N'Khu đô thị An Phú Thịnh, Quy Nhơn, Bình Định', N'02567301817')
SET IDENTITY_INSERT [dbo].[Campuses] OFF
GO
SET IDENTITY_INSERT [dbo].[Roles] ON 

INSERT [dbo].[Roles] ([RoleID], [RoleName]) VALUES (4, N'Admin')
INSERT [dbo].[Roles] ([RoleID], [RoleName]) VALUES (2, N'Lecturer')
INSERT [dbo].[Roles] ([RoleID], [RoleName]) VALUES (3, N'Librarian')
INSERT [dbo].[Roles] ([RoleID], [RoleName]) VALUES (1, N'Student')
SET IDENTITY_INSERT [dbo].[Roles] OFF
GO
INSERT [dbo].[Users] ([UserID], [FullName], [Email], [PasswordHash], [Phone], [CampusID], [RoleID], [Status], [BorrowingLocked]) VALUES (N'HE190001', N'Nguyễn Văn An', N'an@fpt.edu.vn', N'hash123', N'0901111111', 1, 1, N'Active', 0)
INSERT [dbo].[Users] ([UserID], [FullName], [Email], [PasswordHash], [Phone], [CampusID], [RoleID], [Status], [BorrowingLocked]) VALUES (N'HE190002', N'Trần Thị Bình', N'binh@fpt.edu.vn', N'hash123', N'0902222222', 1, 2, N'Active', 0)
INSERT [dbo].[Users] ([UserID], [FullName], [Email], [PasswordHash], [Phone], [CampusID], [RoleID], [Status], [BorrowingLocked]) VALUES (N'HE190003', N'Lê Minh Cường', N'cuong@fpt.edu.vn', N'hash123', N'0903333333', 2, 3, N'Active', 0)
INSERT [dbo].[Users] ([UserID], [FullName], [Email], [PasswordHash], [Phone], [CampusID], [RoleID], [Status], [BorrowingLocked]) VALUES (N'HE190004', N'Phạm Thu Dung', N'dung@fpt.edu.vn', N'hash123', N'0904444444', 2, 3, N'Active', 0)
INSERT [dbo].[Users] ([UserID], [FullName], [Email], [PasswordHash], [Phone], [CampusID], [RoleID], [Status], [BorrowingLocked]) VALUES (N'HE190005', N'Hoàng Quốc Em', N'em@fpt.edu.vn', N'hash123', N'0905555555', 1, 4, N'Active', 0)
GO
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'003', 1, 1, N'Tin học, thông tin, tác phẩm tổng quát', N'Lý thuyết hệ thống')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'004', 1, 1, N'Tin học, thông tin, tác phẩm tổng quát', N'Phần cứng máy tính')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'005', 1, 1, N'Tin học, thông tin, tác phẩm tổng quát', N'Lập trình, chương trình máy tính và dữ liệu')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'005.133', 1, 1, N'Tin học, thông tin, tác phẩm tổng quát', N'Ngôn ngữ lập trình')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'005.74', 1, 1, N'Tin học, thông tin, tác phẩm tổng quát', N'Cấu trúc dữ liệu')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'005.8', 1, 1, N'Tin học, thông tin, tác phẩm tổng quát', N'An toàn dữ liệu')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'006.3', 1, 1, N'Tin học, thông tin, tác phẩm tổng quát', N'Trí tuệ nhân tạo')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'006.6', 1, 1, N'Tin học, thông tin, tác phẩm tổng quát', N'Đồ họa trên máy tính')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'006.7', 1, 1, N'Tin học, thông tin, tác phẩm tổng quát', N'Thiết kế Web')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'010', 2, 1, N'Triết học, Tâm lý học, Tôn giáo', N'Thư mục học')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'020', 2, 1, N'Triết học, Tâm lý học, Tôn giáo', N'Thư viện học và Thông tin học')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'030', 2, 1, N'Triết học, Tâm lý học, Tôn giáo', N'Bách khoa thư')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'150', 2, 1, N'Triết học, Tâm lý học, Tôn giáo', N'Tâm lý học')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'153.4', 2, 1, N'Triết học, Tâm lý học, Tôn giáo', N'Ý nghĩ, tư duy, trực giác')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'153.9', 2, 1, N'Triết học, Tâm lý học, Tôn giáo', N'Trí thông minh và năng khiếu')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'158.1', 2, 1, N'Triết học, Tâm lý học, Tôn giáo', N'Phân tích và hoàn thiện nhân cách')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'160', 2, 1, N'Triết học, Tâm lý học, Tôn giáo', N'Logic học')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'170', 2, 1, N'Triết học, Tâm lý học, Tôn giáo', N'Đạo đức học')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'200', 2, 1, N'Triết học, Tâm lý học, Tôn giáo', N'Tôn giáo')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'294.3', 2, 1, N'Triết học, Tâm lý học, Tôn giáo', N'Phật giáo')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'302.2', 3, 1, N'Khoa học xã hội', N'Giao tiếp')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'320', 3, 1, N'Khoa học xã hội', N'Khoa học chính trị')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'330', 3, 1, N'Khoa học xã hội', N'Kinh tế học')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'332', 3, 1, N'Khoa học xã hội', N'Tài chính')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'337', 3, 1, N'Khoa học xã hội', N'Kinh tế quốc tế')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'340', 3, 1, N'Khoa học xã hội', N'Luật pháp')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'360', 3, 1, N'Khoa học xã hội', N'Dịch vụ xã hội và các hiệp hội')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'370', 3, 1, N'Khoa học xã hội', N'Giáo dục')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'390', 3, 1, N'Khoa học xã hội', N'Phong tục, nghi lễ và văn hóa dân gian')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'410', 4, 1, N'Ngôn ngữ', N'Ngôn ngữ học')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'420', 4, 1, N'Ngôn ngữ', N'Tiếng Anh và ngôn ngữ Anh cổ')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'428', 4, 1, N'Ngôn ngữ', N'Cách sử dụng tiếng Anh chuẩn')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'495', 4, 1, N'Ngôn ngữ', N'Ngôn ngữ Đông Nam Á')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'495.1', 4, 1, N'Ngôn ngữ', N'Tiếng Trung Quốc')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'495.6', 4, 1, N'Ngôn ngữ', N'Tiếng Nhật')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'495.7', 4, 1, N'Ngôn ngữ', N'Tiếng Hàn')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'495.9', 4, 1, N'Ngôn ngữ', N'Ngôn ngữ Đông Nam Á hỗn hợp')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'510', 5, 1, N'Khoa học tự nhiên và ứng dụng', N'Toán học')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'530', 5, 1, N'Khoa học tự nhiên và ứng dụng', N'Vật lý')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'540', 5, 1, N'Khoa học tự nhiên và ứng dụng', N'Hóa học')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'610', 5, 1, N'Khoa học tự nhiên và ứng dụng', N'Y học và sức khỏe')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'621', 5, 1, N'Khoa học tự nhiên và ứng dụng', N'Vật lý ứng dụng')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'657', 5, 1, N'Khoa học tự nhiên và ứng dụng', N'Kế toán')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'658.1', 5, 1, N'Khoa học tự nhiên và ứng dụng', N'Tài chính doanh nghiệp')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'658.15', 5, 1, N'Khoa học tự nhiên và ứng dụng', N'Quản lý tài chính')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'658.3', 5, 1, N'Khoa học tự nhiên và ứng dụng', N'Quản lý nhân sự')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'658.4', 5, 1, N'Khoa học tự nhiên và ứng dụng', N'Quản lý điều hành')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'658.5', 6, 1, N'Kinh doanh và quản lý', N'Quản lý sản xuất')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'658.8', 6, 1, N'Kinh doanh và quản lý', N'Quản lý tiếp thị')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'659', 6, 1, N'Kinh doanh và quản lý', N'Quảng cáo và quan hệ công chúng')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'686', 6, 1, N'Kinh doanh và quản lý', N'In và hoạt động liên quan')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'688', 6, 1, N'Kinh doanh và quản lý', N'Thành phẩm khác và bao bì')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'720', 7, 1, N'Nghệ thuật', N'Kiến trúc')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'730', 7, 1, N'Nghệ thuật', N'Nghệ thuật tạo hình và điêu khắc')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'740', 7, 1, N'Nghệ thuật', N'Vẽ và nghệ thuật trang trí')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'750', 7, 1, N'Nghệ thuật', N'Hội họa')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'760', 7, 1, N'Nghệ thuật', N'Đồ họa và thiết kế nghệ thuật in ấn')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'770', 7, 1, N'Nghệ thuật', N'Nhiếp ảnh và nghệ thuật máy tính')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'780', 7, 1, N'Nghệ thuật', N'Âm nhạc')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'790', 7, 1, N'Nghệ thuật', N'Giải trí và biểu diễn')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'808.5', 8, 1, N'Văn học', N'Diễn thuyết')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'813', 8, 1, N'Văn học', N'Văn học Mỹ bằng tiếng Anh')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'823', 8, 1, N'Văn học', N'Văn học Anh')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'830', 8, 1, N'Văn học', N'Văn học Đức')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'843', 8, 1, N'Văn học', N'Tiểu thuyết Pháp')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'895.1', 8, 1, N'Văn học', N'Văn học Trung Quốc')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'895.6', 8, 1, N'Văn học', N'Văn học Nhật Bản')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'895.7', 8, 1, N'Văn học', N'Văn học Hàn Quốc')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'895.922', 8, 1, N'Văn học', N'Văn học Việt Nam')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'900', 7, 1, N'Lịch sử và Địa lý', N'Lịch sử địa lý')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'930', 7, 1, N'Lịch sử và Địa lý', N'Lịch sử thế giới cổ đại')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'940', 7, 1, N'Lịch sử và Địa lý', N'Lịch sử Châu Âu')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'950', 7, 1, N'Lịch sử và Địa lý', N'Lịch sử Châu Á và Phương Đông')
INSERT [dbo].[Shelves] ([ShelfCode], [ShelfNumber], [CampusID], [ShelfName], [ShelfCodeTopic]) VALUES (N'960-990', 7, 1, N'Lịch sử và Địa lý', N'Lịch sử các khu vực còn lại của thế giới')
GO
SET IDENTITY_INSERT [dbo].[Subjects] ON 

INSERT [dbo].[Subjects] ([SubjectID], [SubjectCode], [SubjectName], [Description]) VALUES (1, N'PRJ301', N'Java Web Application Development', N'Covers web application development using Java technologies such as Servlets, JSP, and Spring Framework.')
INSERT [dbo].[Subjects] ([SubjectID], [SubjectCode], [SubjectName], [Description]) VALUES (2, N'DBI202', N'Database Systems', N'Introduces relational database concepts, SQL, database design, and normalization.')
INSERT [dbo].[Subjects] ([SubjectID], [SubjectCode], [SubjectName], [Description]) VALUES (3, N'SWP391', N'Software Development Project', N'Focuses on software project development, teamwork, requirements analysis, design, implementation, and testing.')
SET IDENTITY_INSERT [dbo].[Subjects] OFF
GO
SET IDENTITY_INSERT [dbo].[Publishers] ON 

INSERT [dbo].[Publishers] ([PublisherID], [PublisherName], [Address], [Phone]) VALUES (1, N'Prentice Hall', N'221 River Street, Hoboken, NJ, USA', N'+1-201-555-1001')
INSERT [dbo].[Publishers] ([PublisherID], [PublisherName], [Address], [Phone]) VALUES (2, N'Addison-Wesley', N'75 Arlington Street, Boston, MA, USA', N'+1-617-555-1002')
INSERT [dbo].[Publishers] ([PublisherID], [PublisherName], [Address], [Phone]) VALUES (3, N'McGraw-Hill Education', N'1325 Avenue of the Americas, New York, NY, USA', N'+1-212-555-1003')
INSERT [dbo].[Publishers] ([PublisherID], [PublisherName], [Address], [Phone]) VALUES (6, N'Pearson', NULL, NULL)
SET IDENTITY_INSERT [dbo].[Publishers] OFF
GO
SET IDENTITY_INSERT [dbo].[Books] ON 

INSERT [dbo].[Books] ([BookID], [SubjectCode], [ISBN], [Title], [PublisherID], [PublishYear], [Edition], [Language], [Description], [CoverImageURL], [DefaultShelfCode], [CreatedAt], [ShelfCode]) VALUES (1, N'PRJ301', N'9780132350884', N'Clean Code', 1, 2008, N'1st', N'English', N'A handbook of agile software craftsmanship.', N'https://example.com/images/clean-code.jpg', N'A1', CAST(N'2026-06-08T16:40:06.713' AS DateTime), NULL)
INSERT [dbo].[Books] ([BookID], [SubjectCode], [ISBN], [Title], [PublisherID], [PublishYear], [Edition], [Language], [Description], [CoverImageURL], [DefaultShelfCode], [CreatedAt], [ShelfCode]) VALUES (2, N'SWP391', N'9780201633610', N'Design Patterns', 2, 1994, N'1st', N'English', N'Elements of reusable object-oriented software.', N'https://example.com/images/design-patterns.jpg', N'A2', CAST(N'2026-06-08T16:40:06.713' AS DateTime), NULL)
INSERT [dbo].[Books] ([BookID], [SubjectCode], [ISBN], [Title], [PublisherID], [PublishYear], [Edition], [Language], [Description], [CoverImageURL], [DefaultShelfCode], [CreatedAt], [ShelfCode]) VALUES (3, N'DBI202', N'9780133970777', N'Database System Concepts', 3, 2019, N'7th', N'English', N'Comprehensive introduction to database systems.', N'https://example.com/images/database-system-concepts.jpg', N'B1', CAST(N'2026-06-08T16:40:06.713' AS DateTime), NULL)
INSERT [dbo].[Books] ([BookID], [SubjectCode], [ISBN], [Title], [PublisherID], [PublishYear], [Edition], [Language], [Description], [CoverImageURL], [DefaultShelfCode], [CreatedAt], [ShelfCode]) VALUES (6, NULL, N'9780135166307', N'LEARNING PYTHON', 6, 2019, N'11th', N'English', N'', N'https://m.media-amazon.com/images/I/91RcdlPx1CL._SY522_.jpg', N'SE-01', CAST(N'2026-06-08T22:58:05.663' AS DateTime), NULL)
SET IDENTITY_INSERT [dbo].[Books] OFF
GO
INSERT [dbo].[UserRoles] ([UserID], [RoleID]) VALUES (N'HE190001', 1)
INSERT [dbo].[UserRoles] ([UserID], [RoleID]) VALUES (N'HE190002', 2)
INSERT [dbo].[UserRoles] ([UserID], [RoleID]) VALUES (N'HE190003', 3)
INSERT [dbo].[UserRoles] ([UserID], [RoleID]) VALUES (N'HE190004', 3)
INSERT [dbo].[UserRoles] ([UserID], [RoleID]) VALUES (N'HE190005', 4)
GO
INSERT [dbo].[SystemConfig] ([ConfigKey], [ConfigValue], [Description], [UpdatedAt], [UpdatedBy]) VALUES (N'FINE_PER_DAY', N'5000', N'Tiền phạt mỗi ngày quá hạn (VNĐ)', CAST(N'2026-06-08T16:39:22.383' AS DateTime), NULL)
INSERT [dbo].[SystemConfig] ([ConfigKey], [ConfigValue], [Description], [UpdatedAt], [UpdatedBy]) VALUES (N'LOAN_DAYS_LECTURER', N'30', N'Số ngày mượn tiêu chuẩn cho Giảng viên', CAST(N'2026-06-08T16:39:22.383' AS DateTime), NULL)
INSERT [dbo].[SystemConfig] ([ConfigKey], [ConfigValue], [Description], [UpdatedAt], [UpdatedBy]) VALUES (N'LOAN_DAYS_STUDENT', N'14', N'Số ngày mượn tiêu chuẩn cho Sinh viên', CAST(N'2026-06-08T16:39:22.383' AS DateTime), NULL)
INSERT [dbo].[SystemConfig] ([ConfigKey], [ConfigValue], [Description], [UpdatedAt], [UpdatedBy]) VALUES (N'MAX_BOOKS_STUDENT', N'3', N'Số cuốn tối đa Student có thể mượn cùng lúc', CAST(N'2026-06-08T16:39:22.383' AS DateTime), NULL)
INSERT [dbo].[SystemConfig] ([ConfigKey], [ConfigValue], [Description], [UpdatedAt], [UpdatedBy]) VALUES (N'MAX_RENEWALS_ONLINE', N'2', N'Số lần gia hạn online tối đa', CAST(N'2026-06-08T16:39:22.383' AS DateTime), NULL)
INSERT [dbo].[SystemConfig] ([ConfigKey], [ConfigValue], [Description], [UpdatedAt], [UpdatedBy]) VALUES (N'RESERVATION_EXPIRE_HR', N'72', N'Số giờ đặt sách trực tuyến trước khi tự hủy', CAST(N'2026-06-08T16:39:22.383' AS DateTime), NULL)
GO
SET IDENTITY_INSERT [dbo].[Authors] ON 

INSERT [dbo].[Authors] ([AuthorID], [AuthorName]) VALUES (3, N'Mark Lutz')
SET IDENTITY_INSERT [dbo].[Authors] OFF
GO
INSERT [dbo].[BookAuthors] ([BookID], [AuthorID]) VALUES (6, 3)
GO
