package com.election.evm.security;

import com.election.evm.entity.User;
import com.election.evm.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final String frontendRedirectUrl;

    public GoogleOAuth2SuccessHandler(
            UserRepository userRepository,
            JwtService jwtService,
            @Lazy PasswordEncoder passwordEncoder,
            @Value("${app.oauth2.frontend-redirect-url}") String frontendRedirectUrl
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.frontendRedirectUrl = frontendRedirectUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth2Token)) {
            response.sendRedirect(UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                    .queryParam("error", "oauth_login_failed")
                    .build(true)
                    .toUriString());
            return;
        }

        OAuth2User oauth2User = oauth2Token.getPrincipal();
        String email = extractString(oauth2User.getAttributes(), "email");
        if (email == null || email.isBlank()) {
            response.sendRedirect(UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                    .queryParam("error", "missing_email")
                    .build(true)
                    .toUriString());
            return;
        }

        String name = extractString(oauth2User.getAttributes(), "name");
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElseGet(() -> {
            User created = new User();
            created.setName((name == null || name.isBlank()) ? normalizedEmail : name.trim());
            created.setEmail(normalizedEmail);
            created.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            created.setRole("citizen");
            return userRepository.save(created);
        });

        if ((user.getName() == null || user.getName().isBlank()) && name != null && !name.isBlank()) {
            user.setName(name.trim());
            userRepository.save(user);
        }

        String token = jwtService.generateToken(String.valueOf(user.getId()), user.getEmail(), user.getRole());
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("token", token)
                .build(true)
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private String extractString(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value == null ? null : value.toString();
    }
}