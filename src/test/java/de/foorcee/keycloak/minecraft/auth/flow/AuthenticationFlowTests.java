package de.foorcee.keycloak.minecraft.auth.flow;

import de.foorcee.keycloak.minecraft.AbstractKeycloakTest;
import de.foorcee.keycloak.minecraft.auth.flow.steps.ChainAuthenticationFlowStep;
import de.foorcee.keycloak.minecraft.auth.result.MinecraftProfile;
import de.foorcee.keycloak.minecraft.auth.result.MsaAccessToken;
import de.foorcee.keycloak.minecraft.auth.steps.MinecraftProfileAuthenticationStep;
import de.foorcee.keycloak.minecraft.auth.steps.MsaDeviceCodeAuthenticationStep;
import de.foorcee.keycloak.minecraft.auth.steps.XblTokenAuthenticationStep;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthenticationFlowTests extends AbstractKeycloakTest {

    @Test
    @Order(1)
    public void testBuilder() {
        var flow = AuthenticationFlows.JAVA_MINECRAFT_PROFILE_DEVICE_CODE;

        assertThat(flow).isNotNull();

        AuthenticationFlowExecutor<MsaAccessToken, MinecraftProfile> executor
                = new AuthenticationFlowExecutor<>(AuthenticationFlows.JAVA_MINECRAFT_PROFILE);
        assertThat(executor.getSteps()).hasSize(2);

        assertThat(executor.getSteps().getFirst()).isInstanceOf(XblTokenAuthenticationStep.class);
        assertThat(executor.getSteps().getLast()).isInstanceOf(ChainAuthenticationFlowStep.class);
    }

    @Test
    @Order(2)
    public void testRunFlow() throws Exception {
        var flow = AuthenticationFlows.JAVA_MINECRAFT_PROFILE_DEVICE_CODE;
        MinecraftProfile execute = flow.execute(session, MINECRAFT_LAUNCHER, null);
        assertThat(execute).isNotNull();
    }
}
