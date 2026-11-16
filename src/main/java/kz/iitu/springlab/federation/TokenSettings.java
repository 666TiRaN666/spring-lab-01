package kz.iitu.springlab.federation;

/** Values from the handout's synthetic token. The local training issuer stands in for a real identity provider. */
public final class TokenSettings {
    public static final String ISSUER = "https://identity.example.edu";
    public static final String AUDIENCE = "https://api.example.edu";
    public static final String KEY_ID = "training-key-01";
    /** RFC 9068: access tokens are typed at+jwt, so an OIDC ID Token (typ JWT) cannot be replayed as one. */
    public static final String ACCESS_TOKEN_TYPE = "at+jwt";
    public static final String POLICY_VERSION = "lab11-policy-v1";

    private TokenSettings() { }
}
