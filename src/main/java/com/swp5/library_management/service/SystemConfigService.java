package com.swp5.library_management.service;

import com.swp5.library_management.entity.SystemConfig;
import com.swp5.library_management.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    public SystemConfigService(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
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
