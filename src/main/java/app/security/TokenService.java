package app.security;

import app.entities.Member;
import app.exceptions.ApiException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.io.IOException;
import java.time.Instant;
import java.util.Properties;

public class TokenService {
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final String issuer;
    private final String audience;
    private final long expirationSeconds;

    public TokenService() {
        Properties props = new Properties();
        try (var input = TokenService.class.getResourceAsStream("/security.properties")) {
            if (input == null) throw new IllegalStateException("Missing security.properties");
            props.load(input);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load JWT configuration", ex);
        }
        String secret = System.getenv("JWT_SECRET");
        if (secret == null) secret = props.getProperty("jwt.secret");
        if (secret == null || secret.length() < 32) throw new IllegalStateException("JWT_SECRET must have at least 32 characters");
        algorithm = Algorithm.HMAC256(secret);
        issuer = props.getProperty("jwt.issuer");
        audience = props.getProperty("jwt.audience");
        expirationSeconds = Long.parseLong(props.getProperty("jwt.expiration.seconds"));
        verifier = JWT.require(algorithm).withIssuer(issuer).withAudience(audience)
                .withClaimPresence("sub").withClaimPresence("exp").withClaimPresence("iat")
                .withClaimPresence("roles").build();
    }

    public String createToken(Member member) {
        Instant now = Instant.now();
        return JWT.create().withIssuer(issuer).withAudience(audience)
                .withSubject(member.getId()).withClaim("username", member.getUsername())
                .withArrayClaim("roles", member.getRolesAsStrings().stream().sorted().toArray(String[]::new))
                .withIssuedAt(now).withExpiresAt(now.plusSeconds(expirationSeconds))
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException | IllegalArgumentException ex) {
            throw new ApiException(401, "Invalid or expired token");
        }
    }

    public long getExpirationSeconds() { return expirationSeconds; }
}
