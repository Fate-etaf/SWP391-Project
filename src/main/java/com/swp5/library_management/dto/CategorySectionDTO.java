package com.swp5.library_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySectionDTO {
    private String categoryName;
    private List<FeaturedBookDTO> books;
}
