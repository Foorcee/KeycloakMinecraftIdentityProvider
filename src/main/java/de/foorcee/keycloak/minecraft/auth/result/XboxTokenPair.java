package de.foorcee.keycloak.minecraft.auth.result;

public record XboxTokenPair<TXsts>(XblToken xblToken, TXsts xstsToken) {
}
