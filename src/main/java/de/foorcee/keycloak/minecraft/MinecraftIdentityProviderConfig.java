package de.foorcee.keycloak.minecraft;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class MinecraftIdentityProviderConfig extends OAuth2IdentityProviderConfig {

    public MinecraftIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public MinecraftIdentityProviderConfig() {
        super();
    }

    @Override
    public String getPrompt() {
        return "select_account";
    }
}
