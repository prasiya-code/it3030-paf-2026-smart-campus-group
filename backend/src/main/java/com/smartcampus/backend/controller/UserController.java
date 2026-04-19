package com.smartcampus.backend.controller;

import com.smartcampus.backend.entity.Role;
import com.smartcampus.backend.entity.User;
import com.smartcampus.backend.repository.RoleRepository;
import com.smartcampus.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).body(response);
        }

        String email = null;

        // Handle OAuth2 user
        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User =
                (OAuth2User) authentication.getPrincipal();

            Long userId = oauth2User.getAttribute("id");
            email = oauth2User.getAttribute("email");
            String firstName = oauth2User.getAttribute("firstName");
            String lastName = oauth2User.getAttribute("lastName");

            List<String> authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

            // Always try to get user data from DB by email for updated values
            Optional<User> userByEmailOpt = userRepository.findByEmail(email);
            if (userByEmailOpt.isPresent()) {
                User user = userByEmailOpt.get();
                List<String> roles = user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toList());
                // Use firstName/lastName from DB if they exist, otherwise use OAuth2 attributes
                response.put("authenticated", true);
                response.put("id", user.getId());
                response.put("email", user.getEmail());
                response.put("firstName", user.getFirstName() != null && !user.getFirstName().isEmpty() ? user.getFirstName() : firstName);
                response.put("lastName", user.getLastName() != null && !user.getLastName().isEmpty() ? user.getLastName() : lastName);
                response.put("roles", roles);
                response.put("role",
                    roles.isEmpty() ? "USER" : roles.get(0));
                response.put("authProvider", "GOOGLE");
                return ResponseEntity.ok(response);
            }

            response.put("authenticated", true);
            response.put("email", email);
            response.put("firstName", firstName);
            response.put("lastName", lastName);
            response.put("role", "USER");
            response.put("authProvider", "GOOGLE");
            return ResponseEntity.ok(response);
        }

        // Handle email/password user
        email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
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
        response.put("authProvider", "LOCAL");

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

        String email = null;

        // Handle OAuth2 user
        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            email = oauth2User.getAttribute("email");
        } else {
            // Handle email/password user
            email = authentication.getName();
        }

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

    @GetMapping("/users/role/{roleName}")
    public ResponseEntity<List<Map<String, Object>>> getUsersByRole(@PathVariable String roleName) {
        List<User> users = userRepository.findAll().stream()
            .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase(roleName)))
            .collect(Collectors.toList());

        List<Map<String, Object>> response = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("firstName", u.getFirstName());
            map.put("lastName", u.getLastName());
            map.put("email", u.getEmail());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // Admin user management endpoints
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> userResponses = users.stream().map(user -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("firstName", user.getFirstName());
            userMap.put("lastName", user.getLastName());
            userMap.put("email", user.getEmail());
            List<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList());
            userMap.put("role", roles.isEmpty() ? "USER" : roles.get(0));
            userMap.put("authProvider", user.getAuthProvider());
            userMap.put("createdAt", user.getCreatedAt());
            return userMap;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(userResponses);
    }

    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String email = request.get("email");
            String firstName = request.get("firstName");
            String lastName = request.get("lastName");
            String password = request.get("password");
            String role = request.get("role");

            if (email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (userRepository.findByEmail(email).isPresent()) {
                response.put("success", false);
                response.put("message", "User with this email already exists");
                return ResponseEntity.badRequest().body(response);
            }

            User user = new User();
            user.setEmail(email.trim());
            user.setFirstName(firstName != null ? firstName.trim() : "");
            user.setLastName(lastName != null ? lastName.trim() : "");
            user.setPassword(passwordEncoder.encode(password));
            user.setAuthProvider("LOCAL");

            // Assign role to user
            String userRole = (role != null && !role.trim().isEmpty()) ? role : "USER";
            Optional<Role> roleOpt = roleRepository.findByName(userRole);
            if (roleOpt.isEmpty()) {
                Role newRole = new Role(userRole);
                roleRepository.save(newRole);
                user.getRoles().add(newRole);
            } else {
                user.getRoles().add(roleOpt.get());
            }

            User savedUser = userRepository.save(user);

            response.put("success", true);
            response.put("message", "User created successfully");
            response.put("user", Map.of(
                "id", savedUser.getId(),
                "email", savedUser.getEmail(),
                "firstName", savedUser.getFirstName(),
                "lastName", savedUser.getLastName()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create user: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PutMapping("/admin/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.status(404).body(response);
            }

            User user = userOpt.get();
            String newRole = request.get("role");

            if (newRole == null || newRole.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Role is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Find the role by name
            Optional<Role> roleOpt = roleRepository.findByName(newRole);
            if (roleOpt.isEmpty()) {
                // Create the role if it doesn't exist
                Role role = new Role(newRole);
                roleRepository.save(role);
                user.getRoles().clear();
                user.getRoles().add(role);
            } else {
                user.getRoles().clear();
                user.getRoles().add(roleOpt.get());
            }

            userRepository.save(user);

            response.put("success", true);
            response.put("message", "Role updated successfully");
            response.put("role", newRole);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to update role: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.status(404).body(response);
            }

            User user = userOpt.get();

            // Clear user's roles first
            user.getRoles().clear();
            userRepository.save(user);

            // Now delete the user
            userRepository.deleteById(userId);

            response.put("success", true);
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to delete user: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
