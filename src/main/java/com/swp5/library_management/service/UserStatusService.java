package com.swp5.library_management.service;

import com.swp5.library_management.entity.User;
import java.util.List;

public interface UserStatusService {
    void enrichStatuses(List<User> users);
    String calculateSingleStatus(String userId, String dbStatus);
}
