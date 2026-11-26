package app.security;

import io.javalin.security.RouteRole;

public enum SecurityRole implements RouteRole {
    USER("user"), ADMIN("admin");

    private final String name;
    SecurityRole(String name) { this.name = name; }
    public String getRoleName() { return name; }
}
