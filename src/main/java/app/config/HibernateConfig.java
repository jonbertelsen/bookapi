package app.config;

import app.entities.Book;
import app.entities.Member;
import app.entities.Role;
import app.entities.Loan;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.Configuration;
import java.util.Properties;

public class HibernateConfig {
    private static EntityManagerFactory emf;

    public static synchronized EntityManagerFactory getEntityManagerFactory(String dbName) {
        if (emf == null || !emf.isOpen()) {
            Properties props = new Properties();
            props.put("hibernate.connection.driver_class", "org.postgresql.Driver");
            props.put("hibernate.connection.url", "jdbc:postgresql://localhost:5432/" + dbName);
            props.put("hibernate.connection.username", "postgres");
            props.put("hibernate.connection.password", "postgres");
            // Preserve existing books and loans across application restarts.
            props.put("hibernate.hbm2ddl.auto", "update");
            props.put("hibernate.show_sql", "false");
            props.put("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
            props.put("hibernate.hikari.maximumPoolSize", "5");
            emf = new Configuration().setProperties(props)
                    .addAnnotatedClass(Book.class)
                    .addAnnotatedClass(Member.class)
                    .addAnnotatedClass(Role.class)
                    .addAnnotatedClass(Loan.class)
                    .buildSessionFactory();
        }
        return emf;
    }

    public static synchronized void close() {
        if (emf != null && emf.isOpen()) emf.close();
    }
}
