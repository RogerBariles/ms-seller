package com.pasteleria.pos.config;

import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Permite usar DATABASE_URL de Render/Railway (formato postgres://).
 */
@Configuration
@ConditionalOnProperty(name = "DATABASE_URL")
public class DatabaseFromUrlConfig {

    @Bean
    @Primary
    public DataSource dataSource(@Value("${DATABASE_URL}") String databaseUrl) {
        URI uri = URI.create(databaseUrl.replace("postgres://", "postgresql://"));
        String username = extractUserInfo(uri.getUserInfo(), true);
        String password = extractUserInfo(uri.getUserInfo(), false);
        String jdbcUrl = "jdbc:postgresql://%s:%d%s?sslmode=require".formatted(
                uri.getHost(),
                uri.getPort() == -1 ? 5432 : uri.getPort(),
                uri.getPath());

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    private static String extractUserInfo(String userInfo, boolean username) {
        if (userInfo == null || userInfo.isBlank()) {
            return "";
        }
        int separator = userInfo.indexOf(':');
        if (separator < 0) {
            return username ? decode(userInfo) : "";
        }
        return username
                ? decode(userInfo.substring(0, separator))
                : decode(userInfo.substring(separator + 1));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
