package com.smartcampus.backend.security;

import com.smartcampus.backend.entity.Role;
import com.smartcampus.backend.entity.User;
import com.smartcampus.backend.repository.RoleRepository;
import com.smartcampus.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String googleSub = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        String firstName = oauth2User.getAttribute("given_name");
        String lastName = oauth2User.getAttribute("family_name");

        User user = userRepository.findByGoogleSub(googleSub)
                .orElseGet(() -> createNewUser(googleSub, email, firstName, lastName));

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());

        List<String> dbRoles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
        List<String> authorityStrings = authorities.stream()
                .map(SimpleGrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        System.out.println("=== CustomOAuth2UserService.loadUser() ===");
        System.out.println("User email: " + email);
        System.out.println("DB roles: " + dbRoles);
        System.out.println("Spring authorities created: " + authorityStrings);
        System.out.println("User ID: " + user.getId());

        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
        attributes.put("id", user.getId());
        attributes.put("email", user.getEmail());
        attributes.put("firstName", user.getFirstName());
        attributes.put("lastName", user.getLastName());

        return new DefaultOAuth2User(authorities, attributes, "sub");
    }

    private User createNewUser(String googleSub, String email, String firstName, String lastName) {
        User user = new User();
        user.setGoogleSub(googleSub);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setIsActive(true);
        user.setAuthProvider("GOOGLE");

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName("USER");
                    return roleRepository.save(newRole);
                });

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        System.out.println("=== Creating new user ===");
        System.out.println("Creating user with email: " + email);
        System.out.println("Assigned role: " + userRole.getName());

        return userRepository.save(user);
    }

    @Service
    public static class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request,
                                            HttpServletResponse response,
                                            Authentication authentication) throws IOException {

            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

            String email = oauth2User.getAttribute("email");
            System.out.println("Logged in user: " + email);

            response.sendRedirect("http://localhost:5174/auth/callback");
        }
    }
}