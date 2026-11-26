package app.config;

import app.controllers.impl.ExceptionController;
import app.controllers.impl.SecurityController;
import app.exceptions.Message;
import app.exceptions.ApiException;
import app.routes.Routes;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinJackson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ApplicationConfig {
    private static Routes routes;
    private static SecurityController securityController;
    private static final ExceptionController exceptionController = new ExceptionController();

    public static void configuration(JavalinConfig config) {
        config.router.contextPath = "/api/v1";
        config.startup.showJavalinBanner = false;
        config.http.defaultContentType = "application/json";
        config.jsonMapper(new JavalinJackson(new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS), false));
        config.routes.beforeMatched(securityController::authenticate);
        config.routes.apiBuilder(routes.getRoutes());
        config.routes.exception(ApiException.class, exceptionController::apiExceptionHandler);
        config.routes.exception(io.javalin.http.HttpResponseException.class, exceptionController::httpExceptionHandler);
        config.routes.exception(io.javalin.validation.ValidationException.class, (ex, ctx) ->
                ctx.status(400).json(new Message(400, "Invalid request: " + ex.getErrors())));
        config.routes.exception(Exception.class, exceptionController::exceptionHandler);
        config.bundledPlugins.enableRouteOverview("/routes");
        config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> {
            rule.allowHost("http://localhost:5173", "http://127.0.0.1:5173");
        }));
    }

    public static Javalin startServer(int port) {
        SecurityConfig.initializeMembers(HibernateConfig.getEntityManagerFactory("library"));
        securityController = new SecurityController();
        routes = new Routes(securityController);
        var app = Javalin.create(ApplicationConfig::configuration);
        app.start("localhost", port);
        return app;
    }

    public static void stopServer(Javalin app) { app.stop(); }
}
