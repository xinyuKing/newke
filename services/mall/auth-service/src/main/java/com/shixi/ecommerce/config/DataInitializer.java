package com.shixi.ecommerce.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Startup initializer placeholder for the mall auth module.
 *
 * <p>Historical versions of the mall project created seed accounts locally. After the forum and
 * mall user systems were merged, that behavior became unsafe because local bootstrap data could
 * consume IDs that should belong to forum users. The initializer is therefore kept as a no-op to
 * document the decision explicitly and to reserve a stable extension point for future startup
 * tasks.</p>
 */
@Configuration
public class DataInitializer {
    /**
     * Returns a no-op startup runner.
     *
     * @return runner that intentionally performs no bootstrap user creation
     */
    @Bean
    public CommandLineRunner initUsers() {
        return args -> {
            // Intentionally left blank. User data must originate from community-user-service.
        };
    }
}
