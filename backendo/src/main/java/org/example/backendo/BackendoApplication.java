package org.example.backendo;

import org.example.backendo.model.PricingConfig;
import org.example.backendo.model.User;
import org.example.backendo.repository.PricingConfigRepository;
import org.example.backendo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableScheduling
public class BackendoApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendoApplication.class, args);
    }

    // Inizializzazione dati di default all'avvio
    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository,
                                          PricingConfigRepository pricingConfigRepository,
                                          PasswordEncoder passwordEncoder) {
        return args -> {
            // Utente Guest di test
            if (userRepository.findByEmail("mario.rossi@email.com").isEmpty()) {
                User guest = User.builder()
                        .Nome("Mario")
                        .Cognome("Rossi")
                        .email("mario.rossi@email.com")
                        .password(passwordEncoder.encode("password123"))
                        .role_guest("ROLE_GUEST")
                        .build();
                userRepository.save(guest);
                System.out.println("✅ UTENTE GUEST DI TEST CREATO (mario.rossi@email.com / password123)");
            }

            // Utente Admin di test
            if (userRepository.findByEmail("admin@bbmanager.it").isEmpty()) {
                User admin = User.builder()
                        .Nome("Amministratore")
                        .Cognome("BB-Manager")
                        .email("admin@bbmanager.it")
                        .password(passwordEncoder.encode("admin123"))
                        .role_guest("ROLE_ADMIN")
                        .build();
                userRepository.save(admin);
                System.out.println("✅ UTENTE ADMIN DI TEST CREATO (admin@bbmanager.it / admin123)");
            }

            // Configurazione tariffe globali di default
            if (pricingConfigRepository.findById(1L).isEmpty()) {
                PricingConfig defaultConfig = PricingConfig.builder()
                        .supplementoOspiteExtra(20.0)
                        .moltiplicatoreGiugnoSettembre(1.15)
                        .moltiplicatoreLuglio(1.30)
                        .moltiplicatoreAgosto(1.50)
                        .build();
                pricingConfigRepository.save(defaultConfig);
                System.out.println("✅ PRICING CONFIG DI DEFAULT INIZIALIZZATA (ID: 1)");
            }
        };
    }
}
