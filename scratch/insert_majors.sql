USE [LMSVer2]
GO

ALTER TABLE [dbo].[Majors] ALTER COLUMN [MajorName] [nvarchar](200) NOT NULL;
GO

INSERT [dbo].[Majors] ([MajorCode], [MajorName]) VALUES 
('SE', N'Kỹ thuật phần mềm (Software Engineering)'),
('AI', N'Trí tuệ nhân tạo (AI)'),
('IA', N'An toàn thông tin (Information Security)'),
('GD', N'Thiết kế đồ họa và Mỹ thuật số (Digital Art & Design)'),
('IS', N'Hệ thống thông tin (Information Systems)'),
('SC', N'Vi mạch bán dẫn'),
('AU', N'Công nghệ ô tô số (Automotive)'),
('DS', N'Khoa học dữ liệu ứng dụng (Applied Data Science)'),
('AIDS', N'Trí tuệ nhân tạo & Khoa học dữ liệu'),
('CS', N'An ninh mạng & An toàn số'),
('BA', N'Quản trị kinh doanh'),
('DM', N'Digital Marketing'),
('IB', N'Kinh doanh quốc tế (International Business)'),
('EC', N'Thương mại điện tử (E-Commerce)'),
('LO', N'Logistics và Quản lý chuỗi cung ứng toàn cầu'),
('BAA', N'Phân tích kinh doanh (Business Analytics)'),
('HM', N'Quản trị khách sạn (Hospitality Management)'),
('THM', N'Quản trị Dịch vụ Du lịch và Lữ hành'),
('EM', N'Quản trị giải trí và sự kiện'),
('CX', N'Quản trị trải nghiệm khách hàng'),
('PM', N'Quản trị thu mua (Procurement Management)'),
('FT', N'Công nghệ tài chính (FinTech)'),
('CF', N'Tài chính doanh nghiệp'),
('SF', N'Tài chính thông minh'),
('MC', N'Truyền thông đa phương tiện'),
('PR', N'Quan hệ công chúng (PR)'),
('IMC', N'Truyền thông Marketing tích hợp (IMC)'),
('BC', N'Truyền thông thương hiệu'),
('EN', N'Ngôn ngữ Anh'),
('JP', N'Ngôn ngữ Nhật'),
('KR', N'Ngôn ngữ Hàn Quốc'),
('CN', N'Ngôn ngữ Trung Quốc');
GO
