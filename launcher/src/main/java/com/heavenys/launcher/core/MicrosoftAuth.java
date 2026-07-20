package com.heavenys.launcher.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.heavenys.launcher.model.Account;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * Microsoft (Xbox Live) authentication via the OAuth 2.0 <b>device-code</b> flow, followed by
 * the Xbox Live &rarr; XSTS &rarr; Minecraft Services token chain.
 *
 * <p>The flow needs an Azure AD application client id, supplied via the
 * {@code HEAVENYS_MSA_CLIENT_ID} environment variable (or constructor). When it is absent the
 * module reports itself {@link #isAvailable() unavailable} and the UI disables Microsoft login
 * gracefully — the rest of the launcher keeps working with offline accounts.
 *
 * <p>This class is intentionally self-contained and modular so it can be exercised as soon as
 * a client id is provided, without touching the rest of the launcher.
 */
public class MicrosoftAuth {
    private static final String DEVICE_CODE_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
    private static final String TOKEN_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String SCOPE = "XboxLive.signin offline_access";

    private final String clientId;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15)).build();

    public MicrosoftAuth() {
        this(System.getenv("HEAVENYS_MSA_CLIENT_ID"));
    }

    public MicrosoftAuth(String clientId) {
        this.clientId = clientId;
    }

    public boolean isAvailable() {
        return clientId != null && !clientId.isBlank();
    }

    public static class AuthUnavailableException extends Exception {
        public AuthUnavailableException(String m) { super(m); }
    }

    public static class AuthException extends Exception {
        public AuthException(String m) { super(m); }
        public AuthException(String m, Throwable c) { super(m, c); }
    }

    public record DeviceCode(String userCode, String verificationUri, String deviceCode,
                             int intervalSeconds, int expiresInSeconds) {
    }

    /** Step 1: request a device code to show the user (they visit the URL and enter the code). */
    public DeviceCode requestDeviceCode() throws AuthUnavailableException, AuthException {
        if (!isAvailable()) {
            throw new AuthUnavailableException(
                    "Microsoft login is not configured. Set HEAVENYS_MSA_CLIENT_ID to your Azure app client id.");
        }
        String body = form("client_id", clientId, "scope", SCOPE);
        JsonObject json = postForm(DEVICE_CODE_URL, body);
        return new DeviceCode(
                json.get("user_code").getAsString(),
                json.get("verification_uri").getAsString(),
                json.get("device_code").getAsString(),
                json.has("interval") ? json.get("interval").getAsInt() : 5,
                json.has("expires_in") ? json.get("expires_in").getAsInt() : 900);
    }

    /**
     * Step 2: poll until the user finishes signing in, then run the Xbox token chain and return
     * a ready-to-use Minecraft {@link Account}. {@code onStatus} receives human-readable progress.
     */
    public Account completeLogin(DeviceCode dc, Consumer<String> onStatus)
            throws AuthUnavailableException, AuthException, InterruptedException {
        if (!isAvailable()) {
            throw new AuthUnavailableException("Microsoft login is not configured.");
        }
        onStatus.accept("Waiting for Microsoft sign-in...");
        String msToken = pollToken(dc);
        onStatus.accept("Authenticating with Xbox Live...");
        String xblToken = xblAuthenticate(msToken);
        String[] xsts = xstsAuthorize(xblToken); // [token, userhash]
        onStatus.accept("Logging into Minecraft services...");
        String mcToken = minecraftLogin(xsts[0], xsts[1]);
        onStatus.accept("Fetching profile...");
        JsonObject profile = minecraftProfile(mcToken);
        return Account.microsoft(profile.get("name").getAsString(),
                profile.get("id").getAsString(), mcToken,
                System.currentTimeMillis() / 1000 + 86_400);
    }

    private String pollToken(DeviceCode dc) throws AuthException, InterruptedException {
        long deadline = System.currentTimeMillis() + dc.expiresInSeconds() * 1000L;
        String body = form("grant_type", "urn:ietf:params:oauth:grant-type:device_code",
                "client_id", clientId, "device_code", dc.deviceCode());
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(Math.max(1, dc.intervalSeconds()) * 1000L);
            JsonObject json = postFormAllowError(TOKEN_URL, body);
            if (json.has("access_token")) {
                return json.get("access_token").getAsString();
            }
            String error = json.has("error") ? json.get("error").getAsString() : "";
            if (!error.equals("authorization_pending") && !error.equals("slow_down")) {
                throw new AuthException("Microsoft sign-in failed: " + error);
            }
        }
        throw new AuthException("Microsoft sign-in timed out.");
    }

    private String xblAuthenticate(String msToken) throws AuthException {
        String payload = "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\","
                + "\"RpsTicket\":\"d=" + msToken + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\","
                + "\"TokenType\":\"JWT\"}";
        return postJson("https://user.auth.xboxlive.com/user/authenticate", payload).get("Token").getAsString();
    }

    private String[] xstsAuthorize(String xblToken) throws AuthException {
        String payload = "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" + xblToken
                + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}";
        JsonObject json = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", payload);
        String uhs = json.getAsJsonObject("DisplayClaims").getAsJsonArray("xui")
                .get(0).getAsJsonObject().get("uhs").getAsString();
        return new String[]{json.get("Token").getAsString(), uhs};
    }

    private String minecraftLogin(String xstsToken, String userHash) throws AuthException {
        String payload = "{\"identityToken\":\"XBL3.0 x=" + userHash + ";" + xstsToken + "\"}";
        return postJson("https://api.minecraftservices.com/authentication/login_with_xbox", payload)
                .get("access_token").getAsString();
    }

    private JsonObject minecraftProfile(String mcToken) throws AuthException {
        try {
            HttpResponse<String> r = http.send(HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                    .header("Authorization", "Bearer " + mcToken).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            return JsonParser.parseString(r.body()).getAsJsonObject();
        } catch (Exception e) {
            throw new AuthException("Failed to fetch Minecraft profile", e);
        }
    }

    // ---- HTTP helpers ----

    private JsonObject postForm(String url, String body) throws AuthException {
        JsonObject json = postFormAllowError(url, body);
        if (json.has("error")) {
            throw new AuthException("Request failed: " + json.get("error").getAsString());
        }
        return json;
    }

    private JsonObject postFormAllowError(String url, String body) throws AuthException {
        try {
            HttpResponse<String> r = http.send(HttpRequest.newBuilder().uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.ofString());
            return JsonParser.parseString(r.body()).getAsJsonObject();
        } catch (Exception e) {
            throw new AuthException("HTTP request failed: " + url, e);
        }
    }

    private JsonObject postJson(String url, String jsonBody) throws AuthException {
        try {
            HttpResponse<String> r = http.send(HttpRequest.newBuilder().uri(URI.create(url))
                    .header("Content-Type", "application/json").header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build(),
                    HttpResponse.BodyHandlers.ofString());
            return JsonParser.parseString(r.body()).getAsJsonObject();
        } catch (Exception e) {
            throw new AuthException("HTTP request failed: " + url, e);
        }
    }

    private static String form(String... kv) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < kv.length; i += 2) {
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(kv[i], StandardCharsets.UTF_8)).append('=')
                    .append(URLEncoder.encode(kv[i + 1], StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
