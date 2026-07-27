package com.swp5.library_management.service;

import com.swp5.library_management.entity.SystemConfig;
import com.swp5.library_management.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Optional;

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
            config.setConfigValue("10");
            config.setDescription("Số cuốn tối đa Giảng viên có thể mượn cùng lúc");
            config.setUpdatedAt(java.time.LocalDateTime.now());
            systemConfigRepository.save(config);
        }
        
        Optional<SystemConfig> studentConfigOpt = systemConfigRepository.findById("MAX_BOOKS_STUDENT");
        if (studentConfigOpt.isPresent()) {
            SystemConfig studentConfig = studentConfigOpt.get();
            if ("Student".equals(studentConfig.getDescription()) || "Số cuốn tối đa có thể mượn cùng lúc".equals(studentConfig.getDescription())) {
                studentConfig.setDescription("Số cuốn tối đa Sinh viên có thể mượn cùng lúc");
                studentConfig.setUpdatedAt(java.time.LocalDateTime.now());
                systemConfigRepository.save(studentConfig);
            }
        }
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
