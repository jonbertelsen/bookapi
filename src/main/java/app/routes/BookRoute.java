package app.routes;

import app.controllers.impl.BookController;
import app.security.SecurityRole;
import io.javalin.apibuilder.EndpointGroup;
import static io.javalin.apibuilder.ApiBuilder.*;

public class BookRoute {
    private final BookController controller = new BookController();

    protected EndpointGroup getRoutes() {
        return () -> {
            get("/", controller::readAll);
            get("/{id}", controller::read);
            post("/", controller::create, SecurityRole.ADMIN);
            put("/{id}", controller::update, SecurityRole.ADMIN);
            delete("/{id}", controller::delete, SecurityRole.ADMIN);
        };
    }
}
