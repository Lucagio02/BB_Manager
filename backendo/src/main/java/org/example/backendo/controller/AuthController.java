package org.example.backendo.controller;

import jakarta.validation.Valid;
import org.example.backendo.dto.AuthResponse;
import org.example.backendo.dto.LoginRequest;
import org.example.backendo.dto.RegisterRequest;
import org.example.backendo.model.User;
import org.example.backendo.security.CustomUserDetails;
import org.example.backendo.security.JwtTokenProvider;
import org.example.backendo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthenticationManager authenticationManager,
                          UserService userService,
                          JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User registered = userService.registerUser(request);

        String token = jwtTokenProvider.generateToken(
                registered.getEmail(),
                registered.getId(),
                registered.getRole_guest()
        );

        AuthResponse response = AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(registered.getId())
                .email(registered.getEmail())
                .nome(registered.getNome())
                .cognome(registered.getCognome())
                .role(registered.getRole_guest())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().trim().toLowerCase(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("ROLE_GUEST");

        String token = jwtTokenProvider.generateToken(authentication, userDetails.getId());

        AuthResponse response = AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(userDetails.getId())
                .email(userDetails.getUsername())
                .nome(userDetails.getNome())
                .cognome(userDetails.getCognome())
                .role(role)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("ROLE_GUEST");

        AuthResponse response = AuthResponse.builder()
                .userId(userDetails.getId())
                .email(userDetails.getUsername())
                .nome(userDetails.getNome())
                .cognome(userDetails.getCognome())
                .role(role)
                .build();

        return ResponseEntity.ok(response);
    }
}
