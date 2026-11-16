package kz.iitu.springlab.federation;

public record Decision(boolean allowed, String reason) {
    static Decision allow(String reason) { return new Decision(true, reason); }
    static Decision deny(String reason) { return new Decision(false, reason); }
}
