package com.proyecto.servicios.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private Environment env;

    @Bean(name="sfDatasource")
    public DataSource sfDatasource(){
        HikariConfig config=new HikariConfig();
        try {
            String jdbcUrl = env.getProperty("spring.datasource.url");
            String username = env.getProperty("spring.datasource.username");
            String password = env.getProperty("spring.datasource.password");

            String databaseUrl = env.getProperty("DATABASE_URL");
            if (databaseUrl != null && !databaseUrl.isBlank() && (jdbcUrl == null || jdbcUrl.contains("localhost:5432"))) {
                try {
                    String cleanUrl = databaseUrl.replace("postgres://", "postgresql://");
                    java.net.URI dbUri = new java.net.URI(cleanUrl);
                    if (dbUri.getUserInfo() != null) {
                        String[] userInfo = dbUri.getUserInfo().split(":");
                        username = userInfo[0];
                        if (userInfo.length > 1) {
                            password = userInfo[1];
                        }
                    }
                    int port = dbUri.getPort() != -1 ? dbUri.getPort() : 5432;
                    String path = dbUri.getPath();
                    jdbcUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + port + path;
                    log.info("Conexión configurada automáticamente desde DATABASE_URL de Render");
                } catch (Exception ex) {
                    log.warn("No se pudo parsear DATABASE_URL: {}", ex.getMessage());
                }
            }

            config.setJdbcUrl(jdbcUrl);
            config.setPassword(password);
            config.setUsername(username);
            config.setMaximumPoolSize(10);
            config.setMaxLifetime(18800);
            config.setConnectionTimeout(5000);
            config.setValidationTimeout(5000);
            config.setMinimumIdle(2);
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("sfDatasource");

        }catch (Exception e){
            log.error("Ha ocurrido un error en la conexcion a base de datos, a causa de:",e);
            return null;
        }
        return new HikariDataSource(config);
    }

    @Bean(name="sfEntityManagerFactory")
    @DependsOn("flyway")
    public LocalContainerEntityManagerFactoryBean sfEntityManagerFactory(){
        LocalContainerEntityManagerFactoryBean em= new LocalContainerEntityManagerFactoryBean();
        try{
          em.setDataSource(sfDatasource());
          em.setPackagesToScan(
                  "com.proyecto.servicios.entity.sf",
                  "com.proyecto.servicios.entity.gestopago",
                  "com.proyecto.servicios.entity.cliente",
                  "com.proyecto.servicios.entity.pago"
          );
          em.setPersistenceUnitName("sfDatasource");
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            em.setJpaVendorAdapter(vendorAdapter);
          Map<String, Object> properties=new HashMap<>();
          properties.put("hibernate.hbm2ddl.auto", "none");
            properties.put("hibernate.show-sql", false);
            properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            properties.put("jakarta.persistence.query.timeout", 600000);

        } catch (Exception e) {
            log.error("Ha ocurrido un error en la conexion a base de datos, a causa de:",e);
            return null;

        }
        return em;
    }
 @Bean(name="sfTransactionManager")
 public PlatformTransactionManager sfTransactionManager(@Qualifier("sfEntityManagerFactory") EntityManagerFactory sfEntityManagerFactory){
        return new JpaTransactionManager(sfEntityManagerFactory);

 }

}

