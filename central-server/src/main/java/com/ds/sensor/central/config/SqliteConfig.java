package com.ds.sensor.central.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Configuration
public class SqliteConfig {

    private static final Logger log = LoggerFactory.getLogger(SqliteConfig.class);

    @Bean
    public static BeanFactoryPostProcessor sqliteDirectoryInitializer(Environment env) {
        return beanFactory -> {
            String url = env.getProperty("spring.datasource.url");
            if (url != null && url.startsWith("jdbc:sqlite:")) {
                String cleanPath = url.substring("jdbc:sqlite:".length());
                int qIdx = cleanPath.indexOf('?');
                if (qIdx != -1) {
                    cleanPath = cleanPath.substring(0, qIdx);
                }
                if (!cleanPath.equals(":memory:") && !cleanPath.trim().isEmpty()) {
                    File dbFile = new File(cleanPath);
                    File parent = dbFile.getParentFile();
                    if (parent != null) {
                        if (!parent.exists()) {
                            boolean ok = parent.mkdirs();
                            log.info("SQLite database parent directory created: {} (success={})", parent.getAbsolutePath(), ok);
                        }
                        // Verify write capability
                        try {
                            File probe = new File(parent, ".forestwatch_write_probe_" + System.currentTimeMillis());
                            if (probe.createNewFile()) {
                                boolean deleted = probe.delete();
                                log.info("SQLite database directory write verification passed: {} (cleaned={})", parent.getAbsolutePath(), deleted);
                            } else {
                                log.warn("SQLite database directory write probe failed for: {}", parent.getAbsolutePath());
                            }
                        } catch (IOException e) {
                            log.error("SQLite database directory write check exception on {}: {}", parent.getAbsolutePath(), e.getMessage());
                        }
                    }
                }
            }
        };
    }

    @Bean
    public static BeanPostProcessor sqliteDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DataSource ds) {
                    // If it is HikariDataSource, set connectionInitSql to a single statement to guarantee execution on every new connection
                    if (ds instanceof HikariDataSource hikariDs) {
                        hikariDs.setConnectionInitSql("PRAGMA busy_timeout=10000;");
                    }
                    try (Connection conn = ds.getConnection();
                         Statement stmt = conn.createStatement()) {
                        // Execute each PRAGMA independently to avoid semicolon-separated failure in SQLite JDBC
                        stmt.execute("PRAGMA journal_mode=WAL;");
                        stmt.execute("PRAGMA busy_timeout=10000;");

                        try (ResultSet rs = stmt.executeQuery("PRAGMA journal_mode;")) {
                            if (rs.next()) {
                                log.info("SQLite PRAGMA journal_mode verified: {}", rs.getString(1));
                            }
                        }
                        try (ResultSet rs = stmt.executeQuery("PRAGMA busy_timeout;")) {
                            if (rs.next()) {
                                log.info("SQLite PRAGMA busy_timeout verified: {} ms", rs.getInt(1));
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error initializing SQLite PRAGMAs on DataSource: {}", e.getMessage(), e);
                    }
                }
                return bean;
            }
        };
    }
}
