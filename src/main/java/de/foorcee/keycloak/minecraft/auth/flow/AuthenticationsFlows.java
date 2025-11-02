package de.foorcee.keycloak.minecraft.auth.flow;

import de.foorcee.keycloak.minecraft.auth.RelyingParty;
import de.foorcee.keycloak.minecraft.auth.result.MinecraftProfile;
import de.foorcee.keycloak.minecraft.auth.result.MsaAccessToken;
import de.foorcee.keycloak.minecraft.auth.result.XboxTokenPair;
import de.foorcee.keycloak.minecraft.auth.steps.MinecraftProfileAuthenticationStep;
import de.foorcee.keycloak.minecraft.auth.steps.MinecraftTokenAuthenticationStep;
import de.foorcee.keycloak.minecraft.auth.steps.MsaDeviceCodeAuthenticationStep;
import de.foorcee.keycloak.minecraft.auth.steps.MsaDeviceCodeVerificationBlockingStep;
import de.foorcee.keycloak.minecraft.auth.steps.XblTokenAuthenticationStep;
import de.foorcee.keycloak.minecraft.auth.steps.XstsTokenAuthenticationStep;

import java.time.Duration;

public class AuthenticationsFlows {

    public static final AuthenticationFlow<Void, MinecraftProfile> JAVA_MINECRAFT_PROFILE_DEVICE_CODE = AuthenticationFlowBuilder.initialize(new MsaDeviceCodeAuthenticationStep())
            .then(new MsaDeviceCodeVerificationBlockingStep(Duration.ofMinutes(2)))
            .then(new XblTokenAuthenticationStep())
            .merge(new XstsTokenAuthenticationStep(RelyingParty.MINECRAFT_SERVICES), XboxTokenPair::new)
            .then(new MinecraftTokenAuthenticationStep())
            .then(new MinecraftProfileAuthenticationStep())
            .build();


    public static final AuthenticationFlow<MsaAccessToken, MinecraftProfile> JAVA_MINECRAFT_PROFILE = AuthenticationFlowBuilder.initialize(new XblTokenAuthenticationStep())
            .merge(new XstsTokenAuthenticationStep(RelyingParty.MINECRAFT_SERVICES), XboxTokenPair::new)
            .then(new MinecraftTokenAuthenticationStep())
            .then(new MinecraftProfileAuthenticationStep())
            .build();
}
