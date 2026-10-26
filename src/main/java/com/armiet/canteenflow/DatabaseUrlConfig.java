package com.armiet.canteenflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class DatabaseUrlConfig implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");

        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        if (databaseUrl.startsWith("postgresql://") || databaseUrl.startsWith("postgres://")) {
            URI uri = URI.create(databaseUrl);
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String database = uri.getPath();

            if (host == null || database == null || database.length() < 2) {
                throw new IllegalStateException("DATABASE_URL is missing its host or database name.");
            }

            Map<String, Object> properties = new HashMap<>();
            properties.put("spring.datasource.url", "jdbc:postgresql://" + host + ":" + port + database);

            String userInfo = uri.getRawUserInfo();
            if (userInfo != null) {
                int separator = userInfo.indexOf(':');
                if (separator >= 0) {
                    properties.put("spring.datasource.username", uri.getUserInfo().substring(0, separator));
                    properties.put("spring.datasource.password", uri.getUserInfo().substring(separator + 1));
                }
            }

            environment.getPropertySources().addFirst(new MapPropertySource("databaseUrlFix", properties));
        }
    }
}
