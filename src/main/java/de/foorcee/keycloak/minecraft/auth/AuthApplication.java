package de.foorcee.keycloak.minecraft.auth;

public record AuthApplication(String clientId, String clientSecret, String scope) {

    private static final String JAVA_CLIENT_ID = "00000000402b5328";
    private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";

    public static AuthApplication minecraftLauncher() {
        return new AuthApplication(JAVA_CLIENT_ID, null, SCOPE);
    }
}
