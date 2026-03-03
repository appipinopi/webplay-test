package com.bluejoin.addon;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.bluecolored.bluemap.common.web.http.HttpRequest;
import de.bluecolored.bluemap.common.web.http.HttpRequestHandler;
import de.bluecolored.bluemap.common.web.http.HttpResponse;
import de.bluecolored.bluemap.common.web.http.HttpStatusCode;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class BlueJoinHttpApi implements HttpRequestHandler {

    private final Gson gson = new Gson();
    private final AccountService accountService;
    private final BlueJoinConfig config;

    public BlueJoinHttpApi(AccountService accountService, BlueJoinConfig config) {
        this.accountService = accountService;
        this.config = config;
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        try {
            String method = request.getMethod();
            String path = request.getPath();

            if ("/bluejoin/api/config".equals(path) && "GET".equalsIgnoreCase(method)) return handleConfig();
            if ("/bluejoin/api/register".equals(path) && "POST".equalsIgnoreCase(method)) return handleRegister(request);
            if ("/bluejoin/api/login".equals(path) && "POST".equalsIgnoreCase(method)) return handleLogin(request);
            if ("/bluejoin/api/logout".equals(path) && "POST".equalsIgnoreCase(method)) return handleLogout(request);
            if ("/bluejoin/api/me".equals(path) && "GET".equalsIgnoreCase(method)) return handleMe(request);
            if ("/bluejoin/api/profile".equals(path) && "POST".equalsIgnoreCase(method)) return handleProfile(request);

            return json(HttpStatusCode.NOT_FOUND, Map.of("ok", false, "error", "not_found"));
        } catch (Exception e) {
            return json(HttpStatusCode.INTERNAL_SERVER_ERROR, Map.of("ok", false, "error", "internal_error"));
        }
    }

    private HttpResponse handleConfig() {
        Map<String, Object> data = new HashMap<>();
        data.put("ok", true);
        data.put("webPlayerPrefix", config.getWebPlayerPrefix());
        data.put("allowRegistration", config.isAllowRegistration());
        return json(HttpStatusCode.OK, data);
    }

    private HttpResponse handleRegister(HttpRequest request) {
        if (!config.isAllowRegistration()) {
            return json(HttpStatusCode.FORBIDDEN, Map.of("ok", false, "error", "registration_disabled"));
        }

        JsonObject body = parseBody(request);
        String username = get(body, "username");
        String email = get(body, "email");
        String password = get(body, "password");
        AccountService.RegisterResult result = accountService.register(username, email, password);
        if (!result.success()) {
            if ("username_exists".equals(result.error())) {
                return json(HttpStatusCode.BAD_REQUEST, Map.of("ok", false, "error", result.error()));
            }
            return json(HttpStatusCode.BAD_REQUEST, Map.of("ok", false, "error", result.error()));
        }

        return json(HttpStatusCode.OK, Map.of("ok", true));
    }

    private HttpResponse handleLogin(HttpRequest request) {
        JsonObject body = parseBody(request);
        String email = get(body, "email");
        String password = get(body, "password");
        String token = accountService.login(email, password);
        if (token == null) {
            return json(HttpStatusCode.UNAUTHORIZED, Map.of("ok", false, "error", "invalid_credentials"));
        }

        AccountService.AuthenticatedUser user = accountService.authenticate(token);
        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        response.put("token", token);
        response.put("username", user.username());
        response.put("email", user.email());
        response.put("webPlayerName", config.getWebPlayerPrefix() + user.username());
        response.put("skinUrl", user.skinUrl());
        response.put("displayName", user.displayName());
        return json(HttpStatusCode.OK, response);
    }

    private HttpResponse handleLogout(HttpRequest request) {
        String token = extractBearerToken(request);
        accountService.logout(token);
        return json(HttpStatusCode.OK, Map.of("ok", true));
    }

    private HttpResponse handleMe(HttpRequest request) {
        String token = extractBearerToken(request);
        AccountService.AuthenticatedUser user = accountService.authenticate(token);
        if (user == null) {
            return json(HttpStatusCode.UNAUTHORIZED, Map.of("ok", false, "error", "unauthorized"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        response.put("username", user.username());
        response.put("email", user.email());
        response.put("webPlayerName", config.getWebPlayerPrefix() + user.username());
        response.put("skinUrl", user.skinUrl());
        response.put("displayName", user.displayName());
        return json(HttpStatusCode.OK, response);
    }

    private HttpResponse handleProfile(HttpRequest request) {
        String token = extractBearerToken(request);
        AccountService.AuthenticatedUser user = accountService.authenticate(token);
        if (user == null) {
            return json(HttpStatusCode.UNAUTHORIZED, Map.of("ok", false, "error", "unauthorized"));
        }

        JsonObject body = parseBody(request);
        accountService.updateProfile(user.username(), get(body, "skinUrl"), get(body, "displayName"));
        AccountService.AuthenticatedUser updated = accountService.authenticate(token);

        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        response.put("username", updated.username());
        response.put("email", updated.email());
        response.put("webPlayerName", config.getWebPlayerPrefix() + updated.username());
        response.put("skinUrl", updated.skinUrl());
        response.put("displayName", updated.displayName());
        return json(HttpStatusCode.OK, response);
    }

    private JsonObject parseBody(HttpRequest request) {
        byte[] bytes = request.getData();
        if (bytes == null || bytes.length == 0) return new JsonObject();
        String text = new String(bytes, StandardCharsets.UTF_8);
        return gson.fromJson(text, JsonObject.class);
    }

    private String get(JsonObject body, String key) {
        if (body == null || !body.has(key) || body.get(key).isJsonNull()) return "";
        return body.get(key).getAsString();
    }

    private String extractBearerToken(HttpRequest request) {
        if (request.getHeader("Authorization") == null) return null;
        String header = request.getHeader("Authorization").getValue();
        if (header == null || !header.startsWith("Bearer ")) return null;
        return header.substring("Bearer ".length()).trim();
    }

    private HttpResponse json(HttpStatusCode code, Map<String, ?> data) {
        HttpResponse response = new HttpResponse(code);
        response.addHeader("Content-Type", "application/json; charset=utf-8");
        response.addHeader("Cache-Control", "no-store");
        response.setData(gson.toJson(data));
        return response;
    }
}
