package de.foorcee.keycloak.minecraft.auth.result;

public record MsaDeviceCode(long expiresAt, long interval,
                            String deviceCode, String userCode,
                            String verificationUri) {

    public String directVerificationUri() {
        return verificationUri + "?otc=" + userCode;
    }
}
