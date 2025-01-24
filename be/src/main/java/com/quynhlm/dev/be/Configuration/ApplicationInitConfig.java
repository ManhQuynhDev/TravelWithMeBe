package com.quynhlm.dev.be.Configuration;

import java.util.HashSet;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.quynhlm.dev.be.enums.Role;
import com.quynhlm.dev.be.model.entity.User;
import com.quynhlm.dev.be.repositories.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class ApplicationInitConfig {
    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository) {
        return args -> {
            if (userRepository.findByEmail("admin@gmail.com").isEmpty()) {
                User user = new User();
                user.setFullname("admin");
                user.setEmail("admin@gmail.com");
                HashSet<String> roles = new HashSet<>();
                roles.add(Role.ADMIN.name());
                user.setRoles(roles);

                PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
                String hashPassword = passwordEncoder.encode("admin");
                user.setPassword(hashPassword);

                userRepository.save(user);
                log.warn("admin user has been create with default email : admin@gmail.com and password : admin");
            }
        };
    }
}
