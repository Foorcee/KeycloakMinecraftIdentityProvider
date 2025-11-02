package de.foorcee.keycloak.minecraft.auth.steps;

import de.foorcee.keycloak.minecraft.AbstractKeycloakTest;
import de.foorcee.keycloak.minecraft.auth.result.MinecraftToken;
import de.foorcee.keycloak.minecraft.auth.result.XblToken;
import de.foorcee.keycloak.minecraft.auth.result.MsaAccessToken;
import de.foorcee.keycloak.minecraft.auth.result.MsaDeviceCode;
import de.foorcee.keycloak.minecraft.auth.result.XboxTokenPair;
import de.foorcee.keycloak.minecraft.auth.result.MinecraftXstsToken;
import de.foorcee.keycloak.minecraft.auth.exception.MsaAuthenticationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MinecraftAuthenticationStepTests extends AbstractKeycloakTest {

    private MsaDeviceCode msaDeviceCode;
    private MsaAccessToken msaAuthToken;
    private XblToken xblToken;
    private MinecraftXstsToken xstsToken;
    private MinecraftToken minecraftToken;

    @Test
    @Order(1)
    public void testInitializeDeviceCode() {
        var step = new MsaDeviceCodeAuthenticationStep();
        msaDeviceCode = Assertions.assertDoesNotThrow(() -> step.execute(session, MINECRAFT_LAUNCHER, null));

        assertThat(msaDeviceCode).isNotNull();
        assertThat(msaDeviceCode.deviceCode()).isNotNull();

        assertThat(msaDeviceCode.verificationUri()).startsWith("https://www.microsoft.com/link");
        assertThat(msaDeviceCode.expiresAt()).isGreaterThan(System.currentTimeMillis());
        assertThat(msaDeviceCode.interval()).isGreaterThan(0);
        assertThat(msaDeviceCode.userCode()).isAlphanumeric().hasSize(8);

        System.out.println("Verification Url: " + msaDeviceCode.directVerificationUri());
    }

    @Test
    @Order(2)
    public void testVerifyDeviceCode() {
        assertThat(msaDeviceCode).isNotNull();

        var step = new MsaDeviceCodeVerificationStep();
        assertThatExceptionOfType(MsaAuthenticationException.class)
                .isThrownBy(() -> step.execute(session, MINECRAFT_LAUNCHER, msaDeviceCode))
                .matches(MsaAuthenticationException::isAuthorizationPending);
    }

    @Test
    @Order(3)
    public void testVerifyDeviceCodeAndUserCode() {
        var step = new MsaDeviceCodeVerificationStep();
        msaAuthToken = await().pollInterval(Duration.ofSeconds(msaDeviceCode.interval()))
                .timeout(3, TimeUnit.MINUTES).until(() -> {
                    try {
                        return step.execute(session, MINECRAFT_LAUNCHER, msaDeviceCode);
                    } catch (MsaAuthenticationException e) {
                        if (e.isAuthorizationPending()) {
                            return null;
                        }
                        throw e;
                    }
                }, Objects::nonNull);

        assertThat(msaAuthToken).isNotNull();
        assertThat(msaAuthToken.accessToken()).isNotEmpty();

        System.out.println("Msa Access Token: " + msaAuthToken.accessToken());
    }

    @Test
    @Order(4)
    public void testXblToken() {
        var step = new XblTokenAuthenticationStep();
        xblToken = Assertions.assertDoesNotThrow(() -> step.execute(session, MINECRAFT_LAUNCHER, msaAuthToken));

        assertThat(xblToken).isNotNull();
        assertThat(xblToken.token()).isNotNull();
        assertThat(xblToken.userHash()).isNotEmpty();

        System.out.println("Xbl Token: " + xblToken);
    }

    @Test
    @Order(5)
    public void testXstsToken() {
        var step = new MinecraftXstsTokenAuthenticationStep();
        xstsToken = Assertions.assertDoesNotThrow(() -> step.execute(session, MINECRAFT_LAUNCHER, xblToken));

        assertThat(xstsToken).isNotNull();
        assertThat(xstsToken.token()).isNotEqualTo(xblToken.token());

        System.out.println("Xsts Token: " + xstsToken);
    }



    @Test
    @Order(6)
    public void testMinecraftToken() {
        var step = new MinecraftTokenAuthenticationStep();
        XboxTokenPair xboxTokenPair = new XboxTokenPair(xblToken, xstsToken);
        minecraftToken = Assertions.assertDoesNotThrow(() -> step.execute(session, MINECRAFT_LAUNCHER, xboxTokenPair));

        assertThat(minecraftToken).isNotNull();
        assertThat(minecraftToken.accessToken()).isNotEmpty();
    }

    @Test
    @Order(7)
    public void testMinecraftProfile() {
        var step = new MinecraftProfileAuthenticationStep();
        var profile = Assertions.assertDoesNotThrow(() -> step.execute(session, MINECRAFT_LAUNCHER, minecraftToken));
        assertThat(profile).isNotNull();

        assertThat(profile.id()).isNotNull();
        assertThat(profile.username()).isNotEmpty();
    }
}
