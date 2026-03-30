package com.mycompany.keycloak.authenticator;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.http.HttpRequest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MultivaluedMap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public class Custom2FAAuthenticator implements Authenticator {

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        String existingOtp = context.getAuthenticationSession().getAuthNote("OTP");
        String otpToShow;

        if (existingOtp == null) {
            otpToShow = String.valueOf((int)(Math.random() * 900000) + 100000);
            context.getAuthenticationSession().setAuthNote("OTP", otpToShow);
            context.getAuthenticationSession().setAuthNote("OTP_TIME", String.valueOf(System.currentTimeMillis()));
            System.out.println("Generated NEW OTP: " + otpToShow);
        } else {
            otpToShow = existingOtp;
            System.out.println("Reusing OTP: " + otpToShow);
        }

        Response challenge = context.form()
                .setAttribute("otp", otpToShow)
                .createForm("2fa-form.ftl");

        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {

        try {
            HttpRequest request = context.getHttpRequest();
            MultivaluedMap<String, String> formData = request.getDecodedFormParameters();
            String enteredOtp = formData.getFirst("otp");

            String storedOtp = context.getAuthenticationSession().getAuthNote("OTP");
            String otpTimeStr = context.getAuthenticationSession().getAuthNote("OTP_TIME");

            // 1. Validate Input
            if (enteredOtp == null || enteredOtp.isEmpty()) {
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                        context.form().setError("otp.empty").createForm("2fa-form.ftl"));
                return;
            }

            // 2. Validate Expiry
            long otpTime = Long.parseLong(otpTimeStr);
            if ((System.currentTimeMillis() - otpTime) > 60000) {
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE,
                        context.form().setError("otp.expired").createForm("2fa-form.ftl"));
                return;
            }

            // 3. Validate Match
            if (!enteredOtp.equals(storedOtp)) {
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                        context.form().setError("otp.failed").createForm("2fa-form.ftl"));
                return;
            }

            System.out.println("OTP VERIFIED SUCCESS");

            // 4. Call External API with Retry
            String username = context.getUser().getUsername();
            String apiUrl = "https://keycloak2fa.free.beeceptor.com/verify?username=" + username;

            System.out.println("Calling API: " + apiUrl);

            String responseBody = callExternalApiWithRetry(apiUrl, 3, 1000);

            System.out.println("API Response: " + responseBody);

            // 5. Validate Response
            if (responseBody == null || !responseBody.contains("cardNumber")) {
                throw new RuntimeException("Invalid API response: cardNumber missing");
            }

            // 6. Extract cardNumber safely
            String cardNumber = responseBody
                    .split("\"cardNumber\"\\s*:\\s*\"")[1]
                    .split("\"")[0];

            // 7. Store attribute
            context.getUser().setSingleAttribute("cardNumber", cardNumber);

            System.out.println("Stored cardNumber: " + cardNumber);

            // Cleanup
            context.getAuthenticationSession().removeAuthNote("OTP");
            context.getAuthenticationSession().removeAuthNote("OTP_TIME");

            context.success();

        } catch (Exception e) {
            e.printStackTrace();

            System.out.println("API failed after retries: " + e.getMessage());

            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    context.form().setError("otp.error").createForm("2fa-form.ftl"));
        }
    }

    /**
     * Retry Logic for External API Call
     */
    private String callExternalApiWithRetry(String apiUrl, int maxRetries, long delayMs) throws Exception {

        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                System.out.println("API Attempt: " + (attempt + 1));

                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                int status = conn.getResponseCode();

                // ❌ Retry for server errors
                if (status >= 500) {
                    throw new RuntimeException("Server error: " + status);
                }

                // ❌ Fail fast for client errors
                if (status >= 400) {
                    throw new RuntimeException("Client error: " + status);
                }

                // ✅ Only accept 200
                if (status != 200) {
                    throw new RuntimeException("Unexpected status: " + status);
                }

                InputStream is = conn.getInputStream();
                return new BufferedReader(new InputStreamReader(is))
                        .lines()
                        .collect(Collectors.joining());

            } catch (Exception e) {
                lastException = e;
                attempt++;

                System.out.println("API call failed (attempt " + attempt + "): " + e.getMessage());

                if (attempt < maxRetries) {
                    long waitTime = delayMs * (long) Math.pow(2, attempt);
                    System.out.println("Retrying in " + waitTime + " ms...");
                    Thread.sleep(waitTime);
                }
            }
        }

        System.out.println("Final failure after retries");
        throw lastException;
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {}

    @Override
    public void close() {}
}