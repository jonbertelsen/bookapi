package app.routes;

import app.controllers.impl.LoanController;
import app.security.SecurityRole;
import io.javalin.apibuilder.EndpointGroup;
import static io.javalin.apibuilder.ApiBuilder.*;

public class LoanRoute {
    private final LoanController controller = new LoanController();

    protected EndpointGroup getRoutes() {
        return () -> {
            get("/", controller::readAll);
            get("/{id}", controller::read);
            post("/", controller::create, SecurityRole.USER);
            put("/{id}", controller::update);
            patch("/{id}", controller::patch);
            delete("/{id}", controller::delete);
        };
    }
}
