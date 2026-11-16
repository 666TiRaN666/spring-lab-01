package kz.iitu.springlab.federation;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A stand-in for the authorization server's token endpoint, used only to produce synthetic tokens for the
 * experiments. Presets build the negative cases of Task B; overrides set the subject, roles, department and scope.
 */
@Service
public class TrainingTokenIssuer {
    public enum Preset { VALID, BAD_SIGNATURE, WRONG_ISSUER, WRONG_AUDIENCE, EXPIRED, ID_TOKEN }

    public record Request(Preset preset, String subject, List<String> roles, String department, String scope) { }

    private final JwtEncoder trusted;
    private final JwtEncoder untrusted;

    public TrainingTokenIssuer(TrainingKeys keys) {
        this.trusted = encoder(keys.trusted());
        this.untrusted = encoder(keys.untrusted());
    }

    private static JwtEncoder encoder(RSAKey key) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    public String issue(Request r) {
        Instant now = Instant.now();
        String issuer = r.preset() == Preset.WRONG_ISSUER ? "https://evil-identity.example.com" : TokenSettings.ISSUER;
        String audience = r.preset() == Preset.WRONG_AUDIENCE ? "https://hr-api.example.edu" : TokenSettings.AUDIENCE;
        Instant issuedAt = r.preset() == Preset.EXPIRED ? now.minus(Duration.ofHours(2)) : now;
        Instant expiresAt = r.preset() == Preset.EXPIRED ? now.minus(Duration.ofHours(1)) : now.plus(Duration.ofMinutes(15));
        // An OIDC ID Token is a plain JWT for the client; to isolate the purpose check, everything else stays valid.
        String type = r.preset() == Preset.ID_TOKEN ? "JWT" : TokenSettings.ACCESS_TOKEN_TYPE;

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(r.subject())
                .audience(List.of(audience))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("scope", r.scope())
                .claim("roles", r.roles())
                .claim("department", r.department());
        if (r.preset() == Preset.ID_TOKEN) claims.claim("nonce", "n-0S6_WzA2Mj").claim("auth_time", now.getEpochSecond());

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(TokenSettings.KEY_ID).type(type).build();
        JwtEncoder encoder = r.preset() == Preset.BAD_SIGNATURE ? untrusted : trusted;
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
