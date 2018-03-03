package org.meds.server;

import org.meds.data.dao.DAOFactory;
import org.meds.data.hibernate.dao.HibernateDAOFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Spring Framework application configuration class
 */
@Configuration
@PropertySource("classpath:server.properties")
@ComponentScan("org.meds")
public class ServerConfiguration {

    @Bean
    public DAOFactory getDaoFactory() {
        return new HibernateDAOFactory();
    }
}
