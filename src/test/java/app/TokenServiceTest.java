package app;

import app.entities.Member;
import app.entities.Role;
import app.exceptions.ApiException;
import app.security.TokenService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {
    private final TokenService service = new TokenService();

    private Algorithm algorithm() throws Exception {
        var props = new Properties();
        try (var input = getClass().getResourceAsStream("/security.properties")) { props.load(input); }
        return Algorithm.HMAC256(System.getenv().getOrDefault("JWT_SECRET", props.getProperty("jwt.secret")));
    }

    @Test
    void issuedTokenContainsIdentityRoleAndExpiration() {
        var member = new Member();
        member.setId("1");
        member.setUsername("1");
        member.addRole(new Role("user"));
        member.addRole(new Role("admin"));
        var jwt = service.verify(service.createToken(member));
        assertEquals("1", jwt.getSubject());
        assertEquals("1", jwt.getClaim("username").asString());
        assertEquals(java.util.Set.of("user", "admin"), new java.util.HashSet<>(jwt.getClaim("roles").asList(String.class)));
        assertEquals(3600, jwt.getExpiresAtAsInstant().getEpochSecond() - jwt.getIssuedAtAsInstant().getEpochSecond());
    }

    @Test
    void expiredTokensAreRejected() throws Exception {
        String token = JWT.create().withIssuer("bookapi").withAudience("bookdemo").withSubject("1")
                .withArrayClaim("roles", new String[]{"user"})
                .withIssuedAt(Instant.now().minusSeconds(7200)).withExpiresAt(Instant.now().minusSeconds(3600))
                .sign(algorithm());
        assertEquals(401, assertThrows(ApiException.class, () -> service.verify(token)).getStatusCode());
    }

    @Test
    void wrongSignatureAndMissingExpirationAreRejected() throws Exception {
        var builder = JWT.create().withIssuer("bookapi").withAudience("bookdemo").withSubject("1")
                .withArrayClaim("roles", new String[]{"user"}).withIssuedAt(Instant.now());
        String noExpiration = builder.sign(algorithm());
        assertThrows(ApiException.class, () -> service.verify(noExpiration));
        String forged = builder.withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(Algorithm.HMAC256("different-signing-key-at-least-32-characters"));
        assertThrows(ApiException.class, () -> service.verify(forged));
    }

    @Test
    void wrongIssuerOrAudienceAreRejected() throws Exception {
        for (String[] claims : new String[][]{{"other", "bookdemo"}, {"bookapi", "other"}}) {
            String token = JWT.create().withIssuer(claims[0]).withAudience(claims[1]).withSubject("1")
                    .withArrayClaim("roles", new String[]{"user"}).withIssuedAt(Instant.now())
                    .withExpiresAt(Instant.now().plusSeconds(3600)).sign(algorithm());
            assertThrows(ApiException.class, () -> service.verify(token));
        }
    }
}
