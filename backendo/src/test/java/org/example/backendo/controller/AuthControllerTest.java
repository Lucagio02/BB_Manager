package org.example.backendo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backendo.dto.LoginRequest;
import org.example.backendo.dto.RegisterRequest;
import org.example.backendo.model.User;
import org.example.backendo.security.CustomUserDetails;
import org.example.backendo.security.JwtTokenProvider;
import org.example.backendo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void register_success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .nome("Mario")
                .cognome("Rossi")
                .email("mario@example.com")
                .password("password123")
                .build();

        User savedUser = User.builder()
                .id(1L)
                .Nome("Mario")
                .Cognome("Rossi")
                .email("mario@example.com")
                .password("encoded_pass")
                .role_guest("ROLE_GUEST")
                .build();

        when(userService.registerUser(any(RegisterRequest.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateToken(eq("mario@example.com"), eq(1L), eq("ROLE_GUEST")))
                .thenReturn("mocked-jwt-token");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token"))
                .andExpect(jsonPath("$.email").value("mario@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_GUEST"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void login_success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("mario@example.com")
                .password("password123")
                .build();

        User user = User.builder()
                .id(1L)
                .Nome("Mario")
                .Cognome("Rossi")
                .email("mario@example.com")
                .password("encoded")
                .role_guest("ROLE_GUEST")
                .build();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtTokenProvider.generateToken(any(Authentication.class), eq(1L)))
                .thenReturn("mocked-login-jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked-login-jwt-token"))
                .andExpect(jsonPath("$.email").value("mario@example.com"))
                .andExpect(jsonPath("$.userId").value(1));
    }
}
