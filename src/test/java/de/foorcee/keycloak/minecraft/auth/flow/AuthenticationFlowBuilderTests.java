package de.foorcee.keycloak.minecraft.auth.flow;

import de.foorcee.keycloak.minecraft.AbstractKeycloakTest;
import de.foorcee.keycloak.minecraft.auth.result.MinecraftProfile;
import de.foorcee.keycloak.minecraft.auth.steps.MinecraftProfileAuthenticationStep;
import de.foorcee.keycloak.minecraft.auth.steps.MsaDeviceCodeAuthenticationStep;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthenticationFlowBuilderTests extends AbstractKeycloakTest {

    @Test
    @Order(1)
    public void testBuilder() {
        var flow = AuthenticationsFlows.JAVA_MINECRAFT_PROFILE_DEVICE_CODE;

        assertThat(flow).isNotNull();
        assertThat(flow.getSteps()).hasSize(6);

        assertThat(flow.getSteps().getFirst()).isInstanceOf(MsaDeviceCodeAuthenticationStep.class);
        assertThat(flow.getSteps().getLast()).isInstanceOf(MinecraftProfileAuthenticationStep.class);
    }

    @Test
    @Order(2)
    public void testRunFlow() throws Exception {
        var flow = AuthenticationsFlows.JAVA_MINECRAFT_PROFILE_DEVICE_CODE;
        MinecraftProfile execute = flow.execute(session, MINECRAFT_LAUNCHER, null);
        assertThat(execute).isNotNull();
    }
}
