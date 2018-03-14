package org.meds;

import org.meds.data.dao.DAOFactory;
import org.meds.data.hibernate.dao.HibernateDAOFactory;
import org.meds.server.Server;
import org.meds.server.impl.SocketIOServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:server.properties")
@ComponentScan("org.meds")
public class ApplicationConfig {

    @Bean
    public DAOFactory getDaoFactory() {
        return new HibernateDAOFactory();
    }

    @Bean
    public Server getServer() {
        return new SocketIOServer();
    }
}
