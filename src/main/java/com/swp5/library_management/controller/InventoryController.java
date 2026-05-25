package com.swp5.library_management.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.swp5.library_management.entity.Campus;
import com.swp5.library_management.entity.Category;
import com.swp5.library_management.repository.CampusRepository;
import com.swp5.library_management.repository.CategoryRepository;
import com.swp5.library_management.service.InventoryService;
import com.swp5.library_management.service.dto.InventoryOverviewDTO;

@Controller
public class InventoryController {

    private final InventoryService inventoryService;
    private final CampusRepository campusRepository;
    private final CategoryRepository categoryRepository;

    public InventoryController(InventoryService inventoryService,
                               CampusRepository campusRepository,
                               CategoryRepository categoryRepository) {
        this.inventoryService = inventoryService;
        this.campusRepository = campusRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/inventory/dashboard")
    public String dashboard(Model model) {
        return "inventory/dashboard";
    }

    @GetMapping("/api/inventory/overview")
    @ResponseBody
    public InventoryOverviewDTO overview() {
        return inventoryService.getOverview();
    }

    @GetMapping("/api/inventory/stats")
    @ResponseBody
    public InventoryOverviewDTO stats(@RequestParam(required = false) Integer campusId,
                                      @RequestParam(required = false) Integer categoryId,
                                      @RequestParam(required = false) String from,
                                      @RequestParam(required = false) String to) {
        return inventoryService.getStats(campusId, categoryId, from, to);
    }

    @GetMapping("/api/campuses")
    @ResponseBody
    public List<Campus> campuses() {
        return campusRepository.findAll();
    }

    @GetMapping("/api/categories")
    @ResponseBody
    public List<Category> categories() {
        return categoryRepository.findAll();
    }
}
