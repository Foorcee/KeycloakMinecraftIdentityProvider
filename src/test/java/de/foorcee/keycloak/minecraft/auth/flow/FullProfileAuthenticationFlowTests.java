package de.foorcee.keycloak.minecraft.auth.flow;

import de.foorcee.keycloak.minecraft.AbstractKeycloakTest;
import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.result.FullProfile;
import de.foorcee.keycloak.minecraft.auth.steps.MsaDeviceCodeAuthenticationStep;
import de.foorcee.keycloak.minecraft.auth.steps.MsaDeviceCodeVerificationBlockingStep;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

public class FullProfileAuthenticationFlowTests extends AbstractKeycloakTest {

    @Test
    public void testFullProfile() {
        AuthenticationFlow<Void, FullProfile> flow = AuthenticationFlow.initialize(new MsaDeviceCodeAuthenticationStep())
                .accept(msaDeviceCode -> {
                    System.out.println("Visit: " + msaDeviceCode.directVerificationUri());
                })
                .then(new MsaDeviceCodeVerificationBlockingStep(Duration.ofMinutes(2)))
                .chain(AuthenticationFlows.FULL_PROFILE);

        FullProfile fullProfile = Assertions.assertDoesNotThrow(() -> flow.execute(session, AuthApplication.minecraftLauncher(), null));

        assertThat(fullProfile).isNotNull();

        System.out.println("Full Profile: " + fullProfile);
    }
}
