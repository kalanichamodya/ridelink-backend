package com.ridelink.account;

import java.nio.charset.StandardCharsets;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jwt.*;
@Configuration
public class AccountConfig {
    @Bean JwtEncoder encoder(@Value("${ridelink.jwt-secret}") String secret) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8)));
    }
    @Bean CommandLineRunner bootstrap(AccountService service, AccountRepository repository,
            @Value("${ADMIN_EMAIL:}") String email, @Value("${ADMIN_PASSWORD:}") String password) {
        return args -> {
            if (!email.isBlank() && repository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT)).isEmpty()) {
                if (password.length() < 12) throw new IllegalArgumentException("ADMIN_PASSWORD must contain at least 12 characters");
                service.create(email,password,"Local administrator",Account.Role.ADMIN);
            }
        };
    }
}
