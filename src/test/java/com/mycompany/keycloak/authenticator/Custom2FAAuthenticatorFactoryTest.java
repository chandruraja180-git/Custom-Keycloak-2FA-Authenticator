package com.mycompany.keycloak.authenticator;

import org.junit.jupiter.api.Test;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Custom2FAAuthenticatorFactoryTest {

    private final Custom2FAAuthenticatorFactory factory =
            new Custom2FAAuthenticatorFactory();

    @Test
    void testCreate_ShouldReturnAuthenticator() {
        KeycloakSession session = mock(KeycloakSession.class);

        Authenticator authenticator = factory.create(session);

        assertNotNull(authenticator);
        assertTrue(authenticator instanceof Custom2FAAuthenticator);
    }

    @Test
    void testGetId() {
        assertEquals("custom-2fa-authenticator", factory.getId());
    }

    @Test
    void testGetDisplayType() {
        assertEquals("Custom 2FA Authenticator", factory.getDisplayType());
    }

    @Test
    void testGetHelpText() {
        assertEquals("Custom two factor authentication", factory.getHelpText());
    }

    @Test
    void testGetReferenceCategory() {
        assertEquals("otp", factory.getReferenceCategory());
    }

    @Test
    void testGetRequirementChoices() {
        AuthenticationExecutionModel.Requirement[] requirements =
                factory.getRequirementChoices();

        assertNotNull(requirements);
        assertEquals(3, requirements.length);

        assertArrayEquals(
                new AuthenticationExecutionModel.Requirement[]{
                        AuthenticationExecutionModel.Requirement.REQUIRED,
                        AuthenticationExecutionModel.Requirement.ALTERNATIVE,
                        AuthenticationExecutionModel.Requirement.DISABLED
                },
                requirements
        );
    }

    @Test
    void testIsConfigurable() {
        assertFalse(factory.isConfigurable());
    }

    @Test
    void testIsUserSetupAllowed() {
        assertFalse(factory.isUserSetupAllowed());
    }

    @Test
    void testGetConfigProperties() {
        List<?> config = factory.getConfigProperties();

        assertNotNull(config);
        assertTrue(config.isEmpty());
    }

    @Test
    void testLifecycleMethods_ShouldNotThrow() {
        assertDoesNotThrow(() -> {
            factory.init(null);
            factory.postInit(null);
            factory.close();
        });
    }
}