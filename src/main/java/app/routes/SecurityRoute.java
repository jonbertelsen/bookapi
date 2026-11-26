package app.routes;

import app.controllers.impl.SecurityController;
import app.security.SecurityRole;
import io.javalin.apibuilder.EndpointGroup;
import static io.javalin.apibuilder.ApiBuilder.*;

public class SecurityRoute {
    private final SecurityController controller;

    public SecurityRoute(SecurityController controller) { this.controller = controller; }

    protected EndpointGroup getRoutes() {
        return () -> {
            post("/login", controller::login);
            get("/me", controller::me, SecurityRole.USER, SecurityRole.ADMIN);
        };
    }
}
