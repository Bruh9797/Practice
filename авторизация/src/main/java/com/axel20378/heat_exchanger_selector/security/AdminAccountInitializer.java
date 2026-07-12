package com.axel20378.heat_exchanger_selector.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

/**
 * При первом запуске системы создает учетную запись администратора,
 * если ни одного пользователя с ролью ADMIN еще не существует.
 * Логин/пароль задаются через свойства app.security.admin.* (см. application.yaml),
 * пароль по умолчанию нужно сменить после первого входа в проде.
 */
@Configuration
public class AdminAccountInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    @Bean
    @ConfigurationProperties(prefix = "app.security.admin")
    public AdminProperties adminProperties() {
        return new AdminProperties();
    }

    @Bean
    public CommandLineRunner seedAdminAccount(UserRepository userRepository,
                                               PasswordEncoder passwordEncoder,
                                               AdminProperties adminProperties) {
        return args -> {
            if (userRepository.findByUsername(adminProperties.getUsername()).isPresent()) {
                return;
            }
            User admin = User.builder()
                    .username(adminProperties.getUsername())
                    .passwordHash(passwordEncoder.encode(adminProperties.getPassword()))
                    .email(adminProperties.getEmail())
                    .role(Role.ADMIN)
                    .enabled(true)
                    .createdAt(Instant.now())
                    .build();
            userRepository.save(admin);
            log.warn("Создана учетная запись администратора по умолчанию: '{}'. " +
                    "Смените пароль после первого входа!", adminProperties.getUsername());
        };
    }

    public static class AdminProperties {
        private String username = "admin";
        private String password = "admin12345";
        private String email = "admin@heat-exchanger.local";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
