package com.smartcampus.backend.controller;

import com.smartcampus.backend.entity.User;
import com.smartcampus.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal OAuth2User principal,
                                                              Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        System.out.println("=== /api/me called ===");
        System.out.println("Principal: " + principal);
        System.out.println("Authentication: " + authentication);

        if (principal == null) {
            response.put("authenticated", false);
            response.put("message", "No authenticated user found - please login first");
            return ResponseEntity.ok(response);
        }

        Long userId = principal.getAttribute("id");
        String email = principal.getAttribute("email");
        String firstName = principal.getAttribute("firstName");
        String lastName = principal.getAttribute("lastName");

        response.put("authenticated", true);
        response.put("id", userId);
        response.put("email", email);
        response.put("firstName", firstName);
        response.put("lastName", lastName);

        // Get authorities from Spring Security session
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        response.put("authorities", authorities);
        System.out.println("Spring Security authorities: " + authorities);

        // Get roles from DB if user exists
        if (userId != null) {
            userRepository.findById(userId).ifPresent(user -> {
                List<String> roles = user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toList());
                response.put("roles", roles);
                System.out.println("DB roles: " + roles);
            });
        }

        return ResponseEntity.ok(response);
    }
}
