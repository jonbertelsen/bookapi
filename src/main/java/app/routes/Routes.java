package app.routes;

import io.javalin.apibuilder.EndpointGroup;
import app.controllers.impl.SecurityController;
import static io.javalin.apibuilder.ApiBuilder.*;

public class Routes {
    private final BookRoute bookRoute = new BookRoute();
    private final MemberRoute memberRoute = new MemberRoute();
    private final LoanRoute loanRoute = new LoanRoute();

    private final SecurityRoute securityRoute;

    public Routes(SecurityController controller) {
        securityRoute = new SecurityRoute(controller);
    }

    public EndpointGroup getRoutes() {
        return () -> {
            path("/auth", securityRoute.getRoutes());
            path("/books", bookRoute.getRoutes());
            path("/members", memberRoute.getRoutes());
            path("/loans", loanRoute.getRoutes());
        };
    }
}
