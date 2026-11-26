package app.dtos;

public record LoginDTO(String username, String password) {
    // Validation errors and logs must never print the supplied password.
    @Override
    public String toString() { return "LoginDTO[credentials redacted]"; }
}
