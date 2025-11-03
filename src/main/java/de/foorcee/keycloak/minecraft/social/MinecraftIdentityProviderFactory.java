package de.foorcee.keycloak.minecraft.social;

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

public class MinecraftIdentityProviderFactory extends AbstractIdentityProviderFactory<MinecraftIdentityProvider> implements SocialIdentityProviderFactory<MinecraftIdentityProvider> {

    public static final String PROVIDER_ID = "minecraft";

    @Override
    public String getName() {
        return "Minecraft";
    }

    @Override
    public MinecraftIdentityProvider create(KeycloakSession keycloakSession, IdentityProviderModel identityProviderModel) {
        return new MinecraftIdentityProvider(keycloakSession, new MinecraftIdentityProviderConfig(identityProviderModel));
    }

    @Override
    public IdentityProviderModel createConfig() {
        return new MinecraftIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
