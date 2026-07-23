package com.swp5.library_management.service;

import com.swp5.library_management.service.UserService;
import com.swp5.library_management.dto.PasswordChangeDTO;
import com.swp5.library_management.repository.UserRepository;
import com.swp5.library_management.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
// XÓA bỏ dòng @RequiredArgsConstructor đi
public class UserServiceImpl implements UserService {

    // Giữ nguyên thuộc tính này là final
    private final UserRepository userRepository;

    // TỰ VIẾT CONSTRUCTOR THỦ CÔNG ĐỂ THAY THẾ LOMBOK (Xóa sạch lỗi Implicit subclass closure)
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public boolean updatePassword(String username, PasswordChangeDTO dto) {
        // 1. Tìm user theo Email từ database
        Optional<User> userOptional = userRepository.findByEmail(username);
        if (userOptional.isEmpty()) {
            return false; // Không tìm thấy tài khoản
        }

        User user = userOptional.get();

        // 2. Kiểm tra mật khẩu cũ 
       if (!user.getPasswordHash().equals(dto.getOldPassword())) {
            return false; // Sai mật khẩu cũ
        }

        // 3. Kiểm tra mật khẩu mới và mật khẩu xác nhận có khớp nhau không
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            return false; // Mật khẩu mới không trùng khớp
        }

        // 4. Cập nhật mật khẩu mới và lưu vào database
        user.setPasswordHash(dto.getNewPassword());
        userRepository.save(user);
        
        return true;
    }
}