package org.example.backendo.service;

import org.example.backendo.dto.RegisterRequest;
import org.example.backendo.model.User;
import org.example.backendo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("L'email è obbligatoria");
        }
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            throw new RuntimeException("Esiste già un utente registrato con l'email: " + user.getEmail());
        }

        // Cifratura sicura password con BCrypt
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getRole_guest() == null || user.getRole_guest().isBlank()) {
            user.setRole_guest("ROLE_GUEST");
        } else if (!user.getRole_guest().startsWith("ROLE_")) {
            user.setRole_guest("ROLE_" + user.getRole_guest().toUpperCase());
        }

        return userRepository.save(user);
    }

    public User registerUser(RegisterRequest request) {
        String role = (request.getRole() != null && !request.getRole().isBlank())
                ? request.getRole()
                : "ROLE_GUEST";

        User user = User.builder()
                .Nome(request.getNome())
                .Cognome(request.getCognome())
                .email(request.getEmail().toLowerCase().trim())
                .password(request.getPassword())
                .role_guest(role)
                .build();

        return registerUser(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
}
