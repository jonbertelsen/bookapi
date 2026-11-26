package app;

import app.config.ApplicationConfig;
import app.config.HibernateConfig;
import app.config.Populate;

public class Main {
    public static void main(String[] args) {
        var emf = HibernateConfig.getEntityManagerFactory("library");
        Populate.populate(emf);
        var app = ApplicationConfig.startServer(7070);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ApplicationConfig.stopServer(app);
            HibernateConfig.close();
        }));
    }
}
