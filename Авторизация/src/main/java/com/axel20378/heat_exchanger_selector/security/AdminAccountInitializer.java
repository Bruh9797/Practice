package com.axel20378.heat_exchanger_selector.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Locale;

/** Создает стартовые учетные записи, заданные активным профилем. */
@Configuration
public class AdminAccountInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    @Bean
    @ConfigurationProperties(prefix = "app.security.admin")
    public AccountProperties adminProperties() {
        return new AccountProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.security.demo-user")
    public DemoAccountProperties demoAccountProperties() {
        return new DemoAccountProperties();
    }

    @Bean
    public CommandLineRunner seedSecurityAccounts(UserRepository userRepository,
                                                  PasswordEncoder passwordEncoder,
                                                  @Qualifier("adminProperties") AccountProperties adminProperties,
                                                  @Qualifier("demoAccountProperties") DemoAccountProperties demoAccountProperties) {
        return args -> {
            seed(userRepository, passwordEncoder, adminProperties, Role.ADMIN);
            if (demoAccountProperties.isEnabled()) {
                seed(userRepository, passwordEncoder, demoAccountProperties, Role.USER);
            }
        };
    }

    private void seed(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      AccountProperties properties,
                      Role role) {
        requireConfigured(properties, role);
        String username = properties.getUsername().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }

        User account = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(properties.getPassword()))
                .email(properties.getEmail().trim().toLowerCase(Locale.ROOT))
                .role(role)
                .enabled(true)
                .createdAt(Instant.now())
                .build();
        userRepository.save(account);
        log.info("Создана стартовая учетная запись {}: '{}'", role, username);
    }

    private void requireConfigured(AccountProperties properties, Role role) {
        if (isBlank(properties.getUsername()) || isBlank(properties.getPassword()) || isBlank(properties.getEmail())) {
            throw new IllegalStateException("Не заданы обязательные параметры стартовой учетной записи " + role);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static class AccountProperties {
        private String username;
        private String password;
        private String email;

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

    public static class DemoAccountProperties extends AccountProperties {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
