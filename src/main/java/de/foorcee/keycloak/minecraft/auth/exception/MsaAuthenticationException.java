package de.foorcee.keycloak.minecraft.auth.exception;

public class MsaAuthenticationException extends RuntimeException {

    private final String error;

    public MsaAuthenticationException(String error, String message) {
        super(message);
        this.error = error;
    }

    public MsaAuthenticationException(String error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public boolean isAuthorizationPending() {
        return "authorization_pending".equals(error);
    }

    public boolean isSlowDown() {
        return "slow_down".equals(error);
    }

    public boolean isExpired() {
        return "expired_token".equals(error);
    }
}
