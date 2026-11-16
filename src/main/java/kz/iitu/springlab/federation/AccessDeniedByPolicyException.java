package kz.iitu.springlab.federation;

public class AccessDeniedByPolicyException extends RuntimeException {
    public AccessDeniedByPolicyException(String reason) { super(reason); }
}
