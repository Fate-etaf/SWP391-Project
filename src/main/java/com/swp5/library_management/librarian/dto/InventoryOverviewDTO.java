package com.swp5.library_management.librarian.dto;

import java.util.List;

public class InventoryOverviewDTO {
    private List<CampusInventoryDTO> campuses;

    public InventoryOverviewDTO() {}

    public InventoryOverviewDTO(List<CampusInventoryDTO> campuses) {
        this.campuses = campuses;
    }

    public List<CampusInventoryDTO> getCampuses() {
        return campuses;
    }

    public void setCampuses(List<CampusInventoryDTO> campuses) {
        this.campuses = campuses;
    }
}
