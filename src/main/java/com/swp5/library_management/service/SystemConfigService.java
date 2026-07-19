package com.swp5.library_management.service;

import com.swp5.library_management.entity.SystemConfig;
import com.swp5.library_management.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import jakarta.annotation.PostConstruct;

@Service
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    public SystemConfigService(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    @PostConstruct
    public void initDefaultConfigs() {
        if (!systemConfigRepository.existsById("MAX_BOOKS_LECTURER")) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey("MAX_BOOKS_LECTURER");
            config.setConfigValue("5"); // as shown in user screenshot
            config.setDescription("Số cuốn tối đa Giảng viên có thể mượn cùng lúc");
            config.setUpdatedAt(java.time.LocalDateTime.now());
            systemConfigRepository.save(config);
        }
        
        systemConfigRepository.findById("MAX_BOOKS_STUDENT").ifPresent(config -> {
            if (config.getDescription() != null && config.getDescription().contains("Student")) {
                config.setDescription("Số cuốn tối đa Sinh viên có thể mượn cùng lúc");
                config.setUpdatedAt(java.time.LocalDateTime.now());
                systemConfigRepository.save(config);
            }
        });
    }

    public String getConfigValue(String key, String defaultValue) {
        Optional<SystemConfig> configOpt = systemConfigRepository.findById(key);
        return configOpt.map(SystemConfig::getConfigValue).orElse(defaultValue);
    }

    public int getIntConfig(String key, int defaultValue) {
        try {
            String val = getConfigValue(key, String.valueOf(defaultValue));
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
