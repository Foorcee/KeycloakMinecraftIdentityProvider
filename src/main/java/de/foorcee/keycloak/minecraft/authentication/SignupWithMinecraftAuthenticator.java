package de.foorcee.keycloak.minecraft.authentication;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.foorcee.keycloak.minecraft.auth.AuthApplication;
import de.foorcee.keycloak.minecraft.auth.AuthenticationFlowStep;
import de.foorcee.keycloak.minecraft.auth.exception.MsaAuthenticationException;
import de.foorcee.keycloak.minecraft.auth.flow.AuthenticationFlows;
import de.foorcee.keycloak.minecraft.auth.result.*;
import de.foorcee.keycloak.minecraft.auth.steps.MsaDeviceCodeAuthenticationStep;
import de.foorcee.keycloak.minecraft.auth.steps.MsaDeviceCodeVerificationStep;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

public class SignupWithMinecraftAuthenticator implements Authenticator {

    private static final String DEVICE_CODE_NOTE = "MINECRAFT_DEVICE_CODE";

    private final KeycloakSession session;
    private final AuthApplication application = AuthApplication.minecraftLauncher();

    public SignupWithMinecraftAuthenticator(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void authenticate(AuthenticationFlowContext flow) {
        AuthenticationSessionModel clientSession = flow.getAuthenticationSession();
        MsaDeviceCodeAuthenticationStep step = new MsaDeviceCodeAuthenticationStep();
        try {
            MsaDeviceCode deviceCode = step.execute(session, application, null);
            writeDeviceCodeNote(clientSession, deviceCode);

            flow.challenge(createForm(flow, deviceCode));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        AuthenticationSessionModel clientSession = context.getAuthenticationSession();
        MsaDeviceCode deviceCode = null;
        try {
            deviceCode = readDeviceCodeNote(clientSession);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to deserialize context in clientSession", e);
        }

        if (deviceCode == null) {
            throw new AuthenticationFlowException("Not found serialized context in clientSession", AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
        }

        long now = System.currentTimeMillis();
        long expiresAt = deviceCode.expiresAt();

        if (expiresAt > 0 && now >= expiresAt) {
            context.resetFlow();
            return;
        }

        // Verify token status
        MsaAccessToken msaAccessToken;
        try {
            MsaDeviceCodeVerificationStep verificationStep = new MsaDeviceCodeVerificationStep();
            msaAccessToken = verificationStep.execute(session, application, deviceCode);
        } catch (MsaAuthenticationException e) {
            if (e.isAuthorizationPending() || e.isSlowDown()) {
                context.challenge(createForm(context, deviceCode));
                return;
            } else if (e.isExpired()) {
                context.resetFlow();
                return;
            } else {
                throw new AuthenticationFlowException("Device code verification failed: " + e.getError(), AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
            }
        } catch (Exception e) {
            throw new AuthenticationFlowException("Device code verification failed", AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
        }

        FullProfile profile;
        try {
            profile = AuthenticationFlows.FULL_PROFILE.execute(session, application, msaAccessToken);
        } catch (Exception e) {
            throw new AuthenticationFlowException("Xbox/Minecraft token authentication failed", e, AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
        }
        MinecraftProfile minecraftProfile = profile.minecraftProfile();
        XboxProfile xboxProfile = profile.xboxProfile();

        if (session.users().getUserByUsername(context.getRealm(), minecraftProfile.username()) != null) {
            Response challenge = context.form()
                    .setError(Messages.USERNAME_EXISTS)
                    .createErrorPage(Response.Status.CONFLICT);
            context.challenge(challenge);
            return;
        }

        if (session.users().getUserByEmail(context.getRealm(), xboxProfile.email()) != null) {
            Response challenge = context.form()
                    .setError(Messages.EMAIL_EXISTS)
                    .createErrorPage(Response.Status.CONFLICT);
            context.challenge(challenge);
            return;
        }

        //Minecraft profile
        UserModel user = session.users().addUser(context.getRealm(), minecraftProfile.id(),
                minecraftProfile.username(), true, true);
        user.setEnabled(true);
        user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);

        //Xbox profile
        user.setEmail(xboxProfile.email());
        user.setFirstName(xboxProfile.firstname());
        user.setLastName(xboxProfile.lastname());

        context.setUser(user);
        context.success();
    }

    private Response createForm(AuthenticationFlowContext flow, MsaDeviceCode deviceCode) {
        LoginFormsProvider form = flow.form().setExecution(flow.getExecution().getId())
                .setAttribute("verificationUrl", deviceCode.directVerificationUri())
                .setAttribute("interval", deviceCode.interval())
                .setAttribute("expiresAt", deviceCode.expiresAt())
                .setAttribute("user_code", deviceCode.userCode());

        return form.createForm("signup-with-minecraft.ftl");
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    private static void writeDeviceCodeNote(AuthenticationSessionModel clientSession, MsaDeviceCode deviceCode) throws JsonProcessingException {
        String json = AuthenticationFlowStep.MAPPER.writeValueAsString(deviceCode);
        clientSession.setAuthNote(DEVICE_CODE_NOTE, json);
    }

    private static MsaDeviceCode readDeviceCodeNote(AuthenticationSessionModel clientSession) throws JsonProcessingException {
        String authNote = clientSession.getAuthNote(DEVICE_CODE_NOTE);
        if (authNote == null) {
            return null;
        }
        return AuthenticationFlowStep.MAPPER.readValue(authNote, MsaDeviceCode.class);
    }
}
