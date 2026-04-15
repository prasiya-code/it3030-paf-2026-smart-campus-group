package com.smartcampus.backend.security;

import com.smartcampus.backend.entity.Role;
import com.smartcampus.backend.entity.User;
import com.smartcampus.backend.repository.RoleRepository;
import com.smartcampus.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomOidcUserService extends OidcUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) {
        System.out.println("=== CustomOidcUserService.loadUser() called ===");

        OidcUser oidcUser = super.loadUser(userRequest);

        String googleSub = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String firstName = oidcUser.getGivenName();
        String lastName = oidcUser.getFamilyName();

        System.out.println("Google sub: " + googleSub);
        System.out.println("Email: " + email);
        System.out.println("First name: " + firstName);
        System.out.println("Last name: " + lastName);

        User user = userRepository.findByGoogleSub(googleSub)
                .orElseGet(() -> createNewUser(googleSub, email, firstName, lastName));

        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        // Build authorities from DB roles
        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());

        // Log roles and authorities for debugging
        List<String> dbRoles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
        List<String> authorityStrings = authorities.stream()
                .map(SimpleGrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        System.out.println("=== CustomOidcUserService.loadUser() ===");
        System.out.println("User email: " + email);
        System.out.println("DB roles: " + dbRoles);
        System.out.println("Spring authorities created: " + authorityStrings);
        System.out.println("User ID: " + user.getId());

        return new DefaultOidcUser(
                authorities,
                oidcUser.getIdToken(),
                oidcUser.getUserInfo()
        );
    }

    private User createNewUser(String googleSub, String email, String firstName, String lastName) {
        System.out.println("=== Creating new user ===");
        System.out.println("Creating user with email: " + email);

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
                    Role savedRole = roleRepository.save(newRole);
                    System.out.println("Created role: " + savedRole.getName());
                    return savedRole;
                });

        System.out.println("Assigned role: " + userRole.getName());

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        System.out.println("New user created with ID: " + savedUser.getId());

        return savedUser;
    }
}