package app.controllers.impl;

import app.config.HibernateConfig;
import app.controllers.ISecurityController;
import app.daos.ISecurityDAO;
import app.daos.impl.SecurityDAO;
import app.dtos.LoginDTO;
import app.dtos.MemberDTO;
import app.dtos.TokenDTO;
import app.exceptions.ApiException;
import app.security.TokenService;
import app.security.SecurityRole;
import io.javalin.http.Context;
import java.nio.charset.StandardCharsets;

public class SecurityController implements ISecurityController {
    private final ISecurityDAO dao = new SecurityDAO(HibernateConfig.getEntityManagerFactory("library"));
    private final TokenService tokens = new TokenService();

    @Override
    public void login(Context ctx) {
        LoginDTO login = ctx.bodyValidator(LoginDTO.class).required()
                .check(dto -> dto.username() != null && !dto.username().isBlank() && dto.username().length() <= 100,
                        "username must be set (max 100 characters)")
                .check(dto -> dto.password() != null && !dto.password().isEmpty()
                        && dto.password().getBytes(StandardCharsets.UTF_8).length <= 72,
                        "password must contain 1-72 UTF-8 bytes")
                .get();
        var member = dao.getVerifiedMember(login.username(), login.password());
        ctx.header("Cache-Control", "no-store");
        ctx.json(new TokenDTO(tokens.createToken(member), "Bearer", tokens.getExpirationSeconds(), new MemberDTO(member)));
    }

    @Override
    public void authenticate(Context ctx) {
        if (ctx.routeRoles().isEmpty() || ctx.method() == io.javalin.http.HandlerType.OPTIONS) return;
        String header = ctx.header("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)
                || header.substring(7).isBlank()) {
            ctx.header("WWW-Authenticate", "Bearer");
            throw new ApiException(401, "Bearer token required");
        }
        var jwt = tokens.verify(header.substring(7));
        var member = dao.getMember(jwt.getSubject());
        var tokenRoles = jwt.getClaim("roles").asList(String.class);
        boolean allowed = tokenRoles != null && ctx.routeRoles().stream()
                .filter(SecurityRole.class::isInstance).map(SecurityRole.class::cast)
                .map(SecurityRole::getRoleName)
                .anyMatch(role -> tokenRoles.contains(role) && member.getRolesAsStrings().contains(role));
        if (!allowed) throw new ApiException(403, "Insufficient role");
        ctx.attribute("member", new MemberDTO(member));
    }

    @Override
    public void me(Context ctx) {
        ctx.header("Cache-Control", "no-store");
        ctx.json((MemberDTO) ctx.attribute("member"));
    }
}
