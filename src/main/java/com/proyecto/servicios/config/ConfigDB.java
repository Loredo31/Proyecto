package com.proyecto.servicios.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
                "com.proyecto.servicios.repositorys.sf",
                "com.proyecto.servicios.repositorys.gestopago",
                "com.proyecto.servicios.repository"
        },
        transactionManagerRef = "sfTransactionManager",
        entityManagerFactoryRef = "sfEntityManagerFactory"
)
public class ConfigDB {

    @Bean(name = "sfDatasource")
    public DataSource sfDatasource(Environment env) {
        HikariConfig config = new HikariConfig();
        try {
            config.setDriverClassName("org.postgresql.Driver");

            String databaseUrl = env.getProperty("DATABASE_URL");
            String jdbcUrl = env.getProperty("spring.datasource.url");
            String username = env.getProperty("spring.datasource.username");
            String password = env.getProperty("spring.datasource.password");

            // Si DATABASE_URL está definida (formato de Render postgres://user:pass@host/db), aplicarla con prioridad
            if (databaseUrl != null && !databaseUrl.isBlank()) {
                aplicarDatabaseUrl(config, databaseUrl);
            } else if (jdbcUrl != null && !jdbcUrl.isBlank()) {
                config.setUsername(username);
                config.setPassword(password);
                config.setJdbcUrl(normalizarJdbcUrl(jdbcUrl));
                log.info("Conexión configurada desde spring.datasource.url");
            }

            config.setMaximumPoolSize(10);
            config.setMaxLifetime(1800000);
            config.setConnectionTimeout(10000);
            config.setValidationTimeout(5000);
            config.setMinimumIdle(2);
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("sfDatasource");

        } catch (Exception e) {
            log.error("Ha ocurrido un error en la configuración de la base de datos:", e);
            throw new IllegalStateException("No se pudo inicializar sfDatasource", e);
        }
        return new HikariDataSource(config);
    }

    private String normalizarJdbcUrl(String url) {
        String res = url.trim();
        if (res.startsWith("postgres://")) {
            res = "jdbc:postgresql://" + res.substring("postgres://".length());
        } else if (res.startsWith("postgresql://")) {
            res = "jdbc:postgresql://" + res.substring("postgresql://".length());
        } else if (!res.startsWith("jdbc:")) {
            res = "jdbc:" + res;
        }

        boolean isRender = res.contains("render.com");
        if (isRender) {
            if (!res.contains("ssl=")) {
                res += (res.contains("?") ? "&" : "?") + "ssl=true";
            }
            if (!res.contains("sslmode=")) {
                res += (res.contains("?") ? "&" : "?") + "sslmode=require";
            }
        }
        return res;
    }

    private void aplicarDatabaseUrl(HikariConfig config, String databaseUrl) {
        try {
            String cleanUrl = databaseUrl.trim().replace("postgres://", "postgresql://");
            URI dbUri = new URI(cleanUrl);
            if (dbUri.getUserInfo() != null) {
                String[] userInfo = dbUri.getUserInfo().split(":", 2);
                config.setUsername(java.net.URLDecoder.decode(userInfo[0], java.nio.charset.StandardCharsets.UTF_8));
                if (userInfo.length > 1) {
                    config.setPassword(java.net.URLDecoder.decode(userInfo[1], java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            String host = dbUri.getHost();
            if (host != null && host.startsWith("dpg-") && !host.contains(".")) {
                host = host + ".oregon-postgres.render.com";
                log.info("Host interno de Render detectado, ajustado para acceso externo: {}", host);
            }
            int port = dbUri.getPort() != -1 ? dbUri.getPort() : 5432;
            String path = dbUri.getPath();
            String query = dbUri.getQuery();
            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;

            boolean isRender = host != null && host.contains("render.com");
            boolean isRailwayInternal = host != null && host.contains("railway.internal");

            if (isRender) {
                config.addDataSourceProperty("ssl", "true");
                config.addDataSourceProperty("sslmode", "require");
                if (query != null && !query.isBlank()) {
                    jdbcUrl += "?" + query;
                    if (!query.contains("ssl=")) {
                        jdbcUrl += "&ssl=true";
                    }
                    if (!query.contains("sslmode=")) {
                        jdbcUrl += "&sslmode=require";
                    }
                } else {
                    jdbcUrl += "?ssl=true&sslmode=require";
                }
            } else if (query != null && !query.isBlank()) {
                jdbcUrl += "?" + query;
            }
            config.setJdbcUrl(jdbcUrl);
            log.info("Conexión configurada automáticamente para host: {} (isRender={}, isRailwayInternal={})", host, isRender, isRailwayInternal);
        } catch (Exception ex) {
            log.warn("No se pudo parsear DATABASE_URL ({}), intentando normalización directa", ex.getMessage());
            config.setJdbcUrl(normalizarJdbcUrl(databaseUrl));
        }
    }

    @Bean(name = "sfEntityManagerFactory")
    @DependsOn("flyway")
    public LocalContainerEntityManagerFactoryBean sfEntityManagerFactory(@Qualifier("sfDatasource") DataSource sfDatasource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        try {
            em.setDataSource(sfDatasource);
            em.setPackagesToScan(
                    "com.proyecto.servicios.entity.sf",
                    "com.proyecto.servicios.entity.gestopago",
                    "com.proyecto.servicios.entity.cliente",
                    "com.proyecto.servicios.entity.pago",
                    "com.proyecto.servicios.entity.seguridad"
            );
            em.setPersistenceUnitName("sfDatasource");
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            em.setJpaVendorAdapter(vendorAdapter);
            Map<String, Object> properties = new HashMap<>();
            properties.put("hibernate.hbm2ddl.auto", "none");
            properties.put("hibernate.show-sql", false);
            properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            properties.put("jakarta.persistence.query.timeout", 600000);

        } catch (Exception e) {
            log.error("Ha ocurrido un error en la conexion a base de datos, a causa de:", e);
            return null;
        }
        return em;
    }

    @Bean(name = "sfTransactionManager")
    public PlatformTransactionManager sfTransactionManager(@Qualifier("sfEntityManagerFactory") EntityManagerFactory sfEntityManagerFactory) {
        return new JpaTransactionManager(sfEntityManagerFactory);
    }
}
