package app.routes;

import app.controllers.impl.MemberController;
import io.javalin.apibuilder.EndpointGroup;
import static io.javalin.apibuilder.ApiBuilder.*;

public class MemberRoute {
    private final MemberController controller = new MemberController();

    protected EndpointGroup getRoutes() {
        return () -> {
            get("/", controller::readAll);
            get("/{id}", controller::read);
            post("/", controller::create);
            put("/{id}", controller::update);
            delete("/{id}", controller::delete);
        };
    }
}
