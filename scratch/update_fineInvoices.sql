USE [LMSVer2]
GO
-- 1. Thêm 2 cột mới
ALTER TABLE [dbo].[FineInvoices] 
ADD [PaymentMethod] [varchar](255) NULL,
    [TransactionCode] [varchar](255) NULL;
GO
-- 2. Thêm ràng buộc constraint (chỉ cho phép các giá trị hợp lệ)
ALTER TABLE [dbo].[FineInvoices]  
WITH CHECK ADD CONSTRAINT [CK_FineInvoices_PaymentMethod] 
CHECK (([PaymentMethod]='QRCode' OR [PaymentMethod]='BankTransfer' OR [PaymentMethod]='Cash' OR [PaymentMethod] IS NULL));
GO
ALTER TABLE [dbo].[FineInvoices] CHECK CONSTRAINT [CK_FineInvoices_PaymentMethod]
GO