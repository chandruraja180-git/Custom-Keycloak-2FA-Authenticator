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

public class Custom2FAAuthenticator implements Authenticator {

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        // Show the 2FA form
        Response challenge = context.form()
                .createForm("2fa-form.ftl");
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        try {
            HttpRequest request = context.getHttpRequest();
            MultivaluedMap<String, String> formData = request.getDecodedFormParameters();
            String otp = formData.getFirst("otp");

            System.out.println("DEBUG: User entered OTP: " + otp);

            if (otp == null || otp.isEmpty()) {
                Response challenge = context.form()
                        .setError("otp.empty")
                        .createForm("2fa-form.ftl");
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
                return;
            }

            // Call Beeceptor API
            String apiUrl = "https://keycloak2fa.free.beeceptor.com/verify?otp=" + otp;
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            System.out.println("DEBUG: API Response Code: " + responseCode);

            // Read the response body
            InputStream stream = (responseCode >= 200 && responseCode < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            reader.close();

            String responseBody = responseBuilder.toString().toLowerCase();
            System.out.println("DEBUG: API Response Body: " + responseBody);

            //  RECTIFIED VALIDATION: Check for specific "success" status in JSON
            if (responseBody.contains("\"status\":\"success\"") || responseBody.contains("\"status\": \"success\"")) {
                System.out.println("✅ OTP VERIFIED SUCCESS");

                // Explicitly tell the session this step passed
                context.getAuthenticationSession().setAuthNote("CUSTOM_2FA_PASSED", "true");

                String cardNumber = "N/A";
                try {
                    if (responseBody.contains("cardnumber")) {
                        // Basic JSON extraction
                        cardNumber = responseBody.split("\"cardnumber\"\\s*:\\s*\"")[1].split("\"")[0];
                    }
                } catch (Exception e) {
                    System.out.println("DEBUG: Could not parse cardNumber from JSON");
                }

                // Store card number in user attribute
                UserModel user = context.getUser();
                user.setSingleAttribute("cardNumber", cardNumber);
                System.out.println("DEBUG: Stored attribute cardNumber: " + cardNumber);

                context.success();
            } else {
                //  THIS IS THE FAILURE PATH
                System.out.println(" OTP FAILED: API response did not indicate success");

                Response challenge = context.form()
                        .setError("otp.failed") // Ensure this key exists in messages_en.properties
                        .createForm("2fa-form.ftl");

                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Response challenge = context.form()
                    .setError("otp.error")
                    .createForm("2fa-form.ftl");
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge);
        }
    }

    @Override
    public boolean requiresUser() { return true; }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) { return true; }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {}

    @Override
    public void close() {}
}