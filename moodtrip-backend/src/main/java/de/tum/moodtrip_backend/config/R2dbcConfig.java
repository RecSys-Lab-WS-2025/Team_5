package de.tum.moodtrip_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

@Configuration
@EnableR2dbcAuditing
public class R2dbcConfig {
    // R2DBC configuration for reactive database operations
    // @EnableR2dbcAuditing enables automatic auditing of entities
}
