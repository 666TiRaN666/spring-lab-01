package kz.iitu.springlab.federation;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.springframework.stereotype.Component;

/**
 * RSA key pairs generated at startup, kept only in memory.
 * trusted — the training issuer's signing key; the resource server trusts only its public half.
 * untrusted — an attacker's key that even reuses the same kid, to show that a signature check is not a kid check.
 * A real resource server would download the issuer's public keys from its JWKS URI instead.
 */
@Component
public class TrainingKeys {
    private final RSAKey trusted;
    private final RSAKey untrusted;

    public TrainingKeys() throws JOSEException {
        this.trusted = new RSAKeyGenerator(2048).keyID(TokenSettings.KEY_ID).generate();
        this.untrusted = new RSAKeyGenerator(2048).keyID(TokenSettings.KEY_ID).generate();
    }

    public RSAKey trusted() { return trusted; }
    public RSAKey untrusted() { return untrusted; }
}
