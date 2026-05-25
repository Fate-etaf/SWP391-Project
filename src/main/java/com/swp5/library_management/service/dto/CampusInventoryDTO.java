package com.swp5.library_management.service.dto;

public class CampusInventoryDTO {
    private Integer campusId;
    private String campusName;
    private long totalCopies;
    private long available;
    private long borrowed;
    private long overdue;

    public CampusInventoryDTO() {}

    public CampusInventoryDTO(Integer campusId, String campusName, long totalCopies, long available, long borrowed, long overdue) {
        this.campusId = campusId;
        this.campusName = campusName;
        this.totalCopies = totalCopies;
        this.available = available;
        this.borrowed = borrowed;
        this.overdue = overdue;
    }

    public Integer getCampusId() {
        return campusId;
    }

    public void setCampusId(Integer campusId) {
        this.campusId = campusId;
    }

    public String getCampusName() {
        return campusName;
    }

    public void setCampusName(String campusName) {
        this.campusName = campusName;
    }

    public long getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(long totalCopies) {
        this.totalCopies = totalCopies;
    }

    public long getAvailable() {
        return available;
    }

    public void setAvailable(long available) {
        this.available = available;
    }

    public long getBorrowed() {
        return borrowed;
    }

    public void setBorrowed(long borrowed) {
        this.borrowed = borrowed;
    }

    public long getOverdue() {
        return overdue;
    }

    public void setOverdue(long overdue) {
        this.overdue = overdue;
    }
}
