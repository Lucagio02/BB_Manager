package org.example.backendo;

import org.example.backendo.model.User;
import org.example.backendo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BackendoApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendoApplication.class, args);
    }

    // Questo codice viene eseguito in automatico all'avvio del server
    @Bean
    public CommandLineRunner testDatabase(UserRepository userRepository) {
        return args -> {
            // Controlla se l'utente esiste già, altrimenti lo crea
            if (userRepository.findByEmail("mario.rossi@email.com").isEmpty()) {
                User utenteTest = User.builder()
                        .Nome("Mario")
                        .Cognome("Rossi")
                        .email("mario.rossi@email.com")
                        .password("password123") // Per ora in chiaro, poi la cripteremo
                        .role_guest("role_guest")
                        .build();

                userRepository.save(utenteTest);
                System.out.println("✅ UTENTE DI TEST SALVATO CON SUCCESSO NEL DATABASE!");
            }
        };
    }
}
