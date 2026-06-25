package com.swp5.library_management.security;

import com.swp5.library_management.entity.User;
import com.swp5.library_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
            OAuth2User oAuth2User = delegate.loadUser(userRequest);

            String email = oAuth2User.getAttribute("email");
            if (email == null) {
                throw new OAuth2AuthenticationException(new OAuth2Error("invalid_email"), "Không tìm thấy email từ Google");
            }

            // Kiểm tra xem email có tồn tại trong hệ thống (đã import qua Excel) chưa
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                throw new OAuth2AuthenticationException(new OAuth2Error("unauthorized_email"), "Tài khoản Google của bạn chưa được kích hoạt/chưa có trên hệ thống.");
            }

            User user = userOpt.get();
            if (!"Active".equalsIgnoreCase(user.getStatus())) {
                throw new OAuth2AuthenticationException(new OAuth2Error("inactive_account"), "Tài khoản của bạn đã bị khóa.");
            }

            return new CustomUserDetails(user, oAuth2User.getAttributes());
        } catch (OAuth2AuthenticationException e) {
            System.out.println("OAuth2 Exception in CustomOAuth2UserService: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.out.println("Unknown Exception in CustomOAuth2UserService: " + e.getMessage());
            e.printStackTrace();
            throw new OAuth2AuthenticationException(new OAuth2Error("internal_error"), e.getMessage());
        }
    }
}
