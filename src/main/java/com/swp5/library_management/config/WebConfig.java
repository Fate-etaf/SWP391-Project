package com.swp5.library_management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private final AdminRestrictionInterceptor adminRestrictionInterceptor;

    public WebConfig(AdminRestrictionInterceptor adminRestrictionInterceptor) {
        this.adminRestrictionInterceptor = adminRestrictionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminRestrictionInterceptor).addPathPatterns("/**");
    }
}
