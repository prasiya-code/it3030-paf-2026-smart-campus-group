package com.smartcampus.backend.controller;

import com.smartcampus.backend.entity.Role;
import com.smartcampus.backend.entity.User;
import com.smartcampus.backend.repository.RoleRepository;
import com.smartcampus.backend.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String email = request.get("email");
            String password = request.get("password");
            String firstName = request.get("firstName");
            String lastName = request.get("lastName");
            String roleName = request.getOrDefault("role", "USER");

            System.out.println("Signup request for email: " + email);

            // Check if email already exists
            var existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent()) {
                User existing = existingUser.get();
                // Check if user has a password (i.e., signed up locally)
                if (existing.getPassword() != null && !existing.getPassword().isEmpty()) {
                    response.put("success", false);
                    response.put("message", "Email already registered with a password. Please log in.");
                    return ResponseEntity.badRequest().body(response);
                } else {
                    // User exists but no password (likely Google OAuth2 user)
                    // Allow them to set a password
                    existing.setPassword(passwordEncoder.encode(password));
                    existing.setFirstName(firstName);
                    existing.setLastName(lastName);
                    existing.setAuthProvider("LOCAL");

                    // Update role if provided
                    Role role = roleRepository.findByName(roleName)
                        .orElseGet(() -> {
                            Role newRole = new Role();
                            newRole.setName(roleName);
                            return roleRepository.save(newRole);
                        });

                    Set<Role> roles = new HashSet<>();
                    roles.add(role);
                    existing.setRoles(roles);

                    userRepository.save(existing);

                    System.out.println("Updated existing OAuth2 user with password for email: " + email);

                    response.put("success", true);
                    response.put("message", "Account updated successfully. Please log in.");
                    return ResponseEntity.ok(response);
                }
            }

            // Create new user
            User user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setIsActive(true);
            user.setAuthProvider("LOCAL");

            // Assign role
            Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(roleName);
                    return roleRepository.save(newRole);
                });

            Set<Role> roles = new HashSet<>();
            roles.add(role);
            user.setRoles(roles);

            userRepository.save(user);

            System.out.println("User created successfully for email: " + email);

            response.put("success", true);
            response.put("message", "Account created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Signup failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> request,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        String email = request.get("email");
        String password = request.get("password");

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
            );

            SecurityContextHolder.getContext()
                .setAuthentication(authentication);
            session.setAttribute("SPRING_SECURITY_CONTEXT",
                SecurityContextHolder.getContext());

            // Get user from DB
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

            List<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(java.util.stream.Collectors.toList());

            response.put("success", true);
            response.put("id", user.getId());
            response.put("email", user.getEmail());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("roles", roles);
            response.put("role", roles.isEmpty() ? "USER" : roles.get(0));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Invalid email or password");
            return ResponseEntity.status(401).body(response);
        }
    }
}
