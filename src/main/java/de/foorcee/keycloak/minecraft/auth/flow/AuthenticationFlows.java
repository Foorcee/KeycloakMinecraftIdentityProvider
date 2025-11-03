package de.foorcee.keycloak.minecraft.auth.flow;

import de.foorcee.keycloak.minecraft.auth.result.*;
import de.foorcee.keycloak.minecraft.auth.steps.MinecraftProfileAuthenticationStep;
import de.foorcee.keycloak.minecraft.auth.steps.MinecraftTokenAuthenticationStep;
import de.foorcee.keycloak.minecraft.auth.steps.MsaDeviceCodeAuthenticationStep;
import de.foorcee.keycloak.minecraft.auth.steps.MsaDeviceCodeVerificationBlockingStep;
import de.foorcee.keycloak.minecraft.auth.steps.XblTokenAuthenticationStep;
import de.foorcee.keycloak.minecraft.auth.steps.MinecraftXstsTokenAuthenticationStep;
import de.foorcee.keycloak.minecraft.auth.steps.XboxLiveFamilyInfoAuthenticationStep;
import de.foorcee.keycloak.minecraft.auth.steps.XboxLiveXstsTokenAuthentication;

import java.time.Duration;

public class AuthenticationFlows {

    public static final AuthenticationFlow<Void, MsaAccessToken> MSA_DEVICE_CODE
            = AuthenticationFlow.initialize(new MsaDeviceCodeAuthenticationStep())
            .then(new MsaDeviceCodeVerificationBlockingStep(Duration.ofMinutes(2)));

    public static final AuthenticationFlow<Void, MinecraftProfile> JAVA_MINECRAFT_PROFILE_DEVICE_CODE = AuthenticationFlow.initialize(new MsaDeviceCodeAuthenticationStep())
            .then(new MsaDeviceCodeVerificationBlockingStep(Duration.ofMinutes(2)))
            .then(new XblTokenAuthenticationStep())
            .merge(new MinecraftXstsTokenAuthenticationStep(), XboxTokenPair::new)
            .then(new MinecraftTokenAuthenticationStep())
            .then(new MinecraftProfileAuthenticationStep());


    private static final AuthenticationFlow<XblToken, XboxProfile> SUBFLOW_XBOX_PROFILE =
            AuthenticationFlow.initialize(new XboxLiveXstsTokenAuthentication(), XboxTokenPair::new)
                    .then(new XboxLiveFamilyInfoAuthenticationStep());

    private static final AuthenticationFlow<XblToken, MinecraftProfile> SUBFLOW_MINECRAFT_PROFILE =
            AuthenticationFlow.initialize(new MinecraftXstsTokenAuthenticationStep(), XboxTokenPair::new)
                    .then(new MinecraftTokenAuthenticationStep())
                    .then(new MinecraftProfileAuthenticationStep());

    public static final AuthenticationFlow<MsaAccessToken, MinecraftProfile> JAVA_MINECRAFT_PROFILE =
            AuthenticationFlow.initialize(new XblTokenAuthenticationStep())
                    .chain(SUBFLOW_MINECRAFT_PROFILE);

    public static final AuthenticationFlow<MsaAccessToken, XboxProfile> XBOX_LIVE_PROFILE =
            AuthenticationFlow.initialize(new XblTokenAuthenticationStep())
                    .chain(SUBFLOW_XBOX_PROFILE);

    public static final AuthenticationFlow<MsaAccessToken, FullProfile> FULL_PROFILE =
            AuthenticationFlow.initialize(new XblTokenAuthenticationStep())
                    .parallelize(SUBFLOW_XBOX_PROFILE, SUBFLOW_MINECRAFT_PROFILE, FullProfile::new);
}
