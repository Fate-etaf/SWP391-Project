package com.swp5.library_management.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDataDTO {

    //TOP CARDS
    private long totalCopies;
    private long borrowedCopies;
    private long overdueCopies;
    private long pendingInboundTransfers;

    //CHARTS
    //Stacked Bar Chart: Map<SubjectCode, Map<CopyStatus, Count>>
    private Map<String, Map<String, Long>> stackedBarData; 
    
    //Doughnut Chart: Map<SubjectCode, Count>
    private Map<String, Long> doughnutData;

    //DATA TABLES
    private List<WaitlistHotspotDTO> waitlistHotspots;
    private List<OverdueActionDTO> overdueBooks;

    //INNER CLASSES CHO DATA TABLES
    
    @Data
    @NoArgsConstructor
    public static class WaitlistHotspotDTO {
        private Integer bookId;
        private String title;
        private String isbn;
        private long waitingCount;
        private long currentCampusStock; // Số lượng sách có sẵn tại cơ sở hiện tại
        private String crossCampusStockInfo; // Chuỗi hiển thị (VD: "Đà Nẵng: 2 cuốn, Cần Thơ: 1 cuốn")
        private boolean canRequestTransfer;  // Cờ báo hiệu UI hiện nút [Xin Luân chuyển] hay [Đề xuất Mua]

        public WaitlistHotspotDTO(Integer bookId, String title, String isbn, long waitingCount) {
            this.bookId = bookId;
            this.title = title;
            this.isbn = isbn;
            this.waitingCount = waitingCount;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverdueActionDTO {
        private String copyId;
        private String bookTitle;
        private String patronId;
        private String patronName;
        private long daysOverdue;
    }
}