package com.mycompany.keycloak.authenticator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Custom2FAAuthenticatorTest {

    private Custom2FAAuthenticator authenticator;

    @Mock
    private AuthenticationFlowContext context;

    @Mock
    private AuthenticationSessionModel authSession;

    @Mock
    private LoginFormsProvider form;

    @Mock
    private UserModel user;

    @Mock
    private HttpRequest httpRequest;

    @BeforeEach
    void setUp() {
        authenticator = new Custom2FAAuthenticator();
        // Setup common mock behavior
        lenient().when(context.getAuthenticationSession()).thenReturn(authSession);
        lenient().when(context.form()).thenReturn(form);
        lenient().when(context.getUser()).thenReturn(user);
        lenient().when(context.getHttpRequest()).thenReturn(httpRequest);
    }

    @Test
    void testAuthenticate_GeneratesNewOtpWhenNoneExists() {
        // Arrange: Session has no OTP
        when(authSession.getAuthNote("OTP")).thenReturn(null);
        when(form.setAttribute(anyString(), any())).thenReturn(form);
        when(form.createForm(anyString())).thenReturn(Response.ok().build());

        // Act
        authenticator.authenticate(context);

        // Assert: Verify OTP was generated and saved to session
        verify(authSession).setAuthNote(eq("OTP"), anyString());
        verify(authSession).setAuthNote(eq("OTP_TIME"), anyString());
        verify(context).challenge(any());
    }

    @Test
    void testAction_FailsOnWrongOtp() {
        // Arrange: Stored OTP is 123456, User enters 000000
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add("otp", "000000");

        when(httpRequest.getDecodedFormParameters()).thenReturn(formData);
        when(authSession.getAuthNote("OTP")).thenReturn("123456");
        when(authSession.getAuthNote("OTP_TIME")).thenReturn(String.valueOf(System.currentTimeMillis()));
        when(form.setError(anyString())).thenReturn(form);

        // Act
        authenticator.action(context);

        // Assert: Failure challenge triggered with INVALID_CREDENTIALS
        verify(context).failureChallenge(eq(AuthenticationFlowError.INVALID_CREDENTIALS), any());
    }

    @Test
    void testAction_FailsOnExpiredOtp() {
        // Arrange: OTP is 2 minutes old (Expired)
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add("otp", "123456");

        long oldTime = System.currentTimeMillis() - 120000; // 120 seconds ago

        when(httpRequest.getDecodedFormParameters()).thenReturn(formData);
        when(authSession.getAuthNote("OTP")).thenReturn("123456");
        when(authSession.getAuthNote("OTP_TIME")).thenReturn(String.valueOf(oldTime));
        when(form.setError(anyString())).thenReturn(form);

        // Act
        authenticator.action(context);

        // Assert: Verify EXPIRED_CODE error
        verify(context).failureChallenge(eq(AuthenticationFlowError.EXPIRED_CODE), any());
    }
}