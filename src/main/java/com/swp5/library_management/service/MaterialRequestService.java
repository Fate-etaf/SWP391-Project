package com.swp5.library_management.service;

import com.swp5.library_management.entity.MaterialRequest;

public interface MaterialRequestService {
    MaterialRequest createMaterialRequest(String patronId, MaterialRequest request);
    MaterialRequest approveRequest(Integer requestId, String librarianId);
}
