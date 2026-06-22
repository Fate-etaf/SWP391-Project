package com.swp5.library_management.service;

import com.swp5.library_management.dto.PasswordChangeDTO;

public interface UserService {
    // ... các hàm có sẵn của bạn (ví dụ: findByEmail, v.v.)
    
    // THÊM DÒNG NÀY VÀO ĐÂY:
    boolean updatePassword(String username, PasswordChangeDTO dto);
}