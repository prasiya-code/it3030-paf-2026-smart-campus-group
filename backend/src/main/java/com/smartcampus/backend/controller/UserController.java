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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(response);
        }

        String email = authentication.getName();

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Try OAuth2 user
            if (authentication.getPrincipal() instanceof OAuth2User) {
                OAuth2User oauth2User =
                    (OAuth2User) authentication.getPrincipal();
                response.put("authenticated", true);
                response.put("id", oauth2User.getAttribute("id"));
                response.put("email", oauth2User.getAttribute("email"));
                response.put("firstName",
                    oauth2User.getAttribute("firstName"));
                response.put("lastName",
                    oauth2User.getAttribute("lastName"));
                List<String> authorities = authentication.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
                response.put("authorities", authorities);
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(401).body(response);
        }

        User user = userOpt.get();
        List<String> roles = user.getRoles().stream()
            .map(role -> role.getName())
            .collect(Collectors.toList());

        response.put("authenticated", true);
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("roles", roles);
        response.put("role", roles.isEmpty() ? "USER" : roles.get(0));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/user/profile")
    public ResponseEntity<Map<String, Object>> updateUserProfile(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(response);
        }

        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found");
            return ResponseEntity.status(404).body(response);
        }

        try {
            User user = userOpt.get();

            String firstName = request.get("firstName");
            String lastName = request.get("lastName");

            if (firstName != null && !firstName.trim().isEmpty()) {
                user.setFirstName(firstName);
            }
            if (lastName != null && !lastName.trim().isEmpty()) {
                user.setLastName(lastName);
            }

            userRepository.save(user);

            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("email", user.getEmail());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to update profile: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
