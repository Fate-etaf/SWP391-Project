package com.swp5.library_management.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminRestrictionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            String uri = request.getRequestURI();
            
            // Cho phép Admin truy cập các đường dẫn hợp lệ sau
            if (uri.startsWith("/admin") || uri.startsWith("/login") || uri.startsWith("/logout")
                || uri.startsWith("/css") || uri.startsWith("/js") || uri.startsWith("/images")
                || uri.startsWith("/oauth2") || uri.startsWith("/error") || uri.startsWith("/webjars")) {
                return true;
            }
            
            // Bất kỳ đường dẫn nào khác sẽ bị chặn và ép về trang quản lý người dùng
            response.sendRedirect("/admin/users");
            return false;
        }
        return true;
    }
}
