package com.bluejoin.addon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class AccountService {

    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;
    private static final int ITERATIONS = 120_000;
    private static final Type ACCOUNT_MAP_TYPE = new TypeToken<Map<String, AccountRecord>>() {
    }.getType();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SessionRecord> sessions = new ConcurrentHashMap<>();

    private final Path accountsPath;
    private final Path profilesPath;
    private final int sessionHours;

    private Map<String, AccountRecord> accountsByName = new HashMap<>();
    private Map<String, String> usernamesByEmail = new HashMap<>();
    private Map<String, ProfileRecord> profilesByName = new HashMap<>();

    public AccountService(Path dataDirectory, int sessionHours) throws IOException {
        this.accountsPath = dataDirectory.resolve("accounts.json");
        this.profilesPath = dataDirectory.resolve("profiles.json");
        this.sessionHours = sessionHours <= 0 ? 24 : sessionHours;
        load();
    }

    public synchronized RegisterResult register(String username, String email, String password) {
        String normalized = normalize(username);
        String normalizedEmail = normalizeEmail(email);
        if (normalized == null) return RegisterResult.invalid("username");
        if (normalizedEmail == null) return RegisterResult.invalid("email");
        if (password == null || password.length() < 6) return RegisterResult.invalid("password");
        if (accountsByName.containsKey(normalized)) return RegisterResult.conflict();
        if (usernamesByEmail.containsKey(normalizedEmail)) return RegisterResult.conflictEmail();

        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        byte[] hash = hash(password.toCharArray(), salt);

        AccountRecord account = new AccountRecord(
                normalized,
                normalizedEmail,
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(hash),
                Instant.now().toString()
        );
        accountsByName.put(normalized, account);
        usernamesByEmail.put(normalizedEmail, normalized);
        profilesByName.putIfAbsent(normalized, new ProfileRecord(normalized, "", ""));
        save();
        return RegisterResult.ok();
    }

    public synchronized String login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || password == null) return null;
        String username = usernamesByEmail.get(normalizedEmail);
        if (username == null) return null;
        AccountRecord record = accountsByName.get(username);
        if (record == null) return null;

        byte[] salt = Base64.getDecoder().decode(record.saltBase64());
        byte[] expected = Base64.getDecoder().decode(record.hashBase64());
        byte[] actual = hash(password.toCharArray(), salt);
        if (!MessageDigest.isEqual(expected, actual)) return null;

        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plusSeconds(sessionHours * 3600L);
        sessions.put(token, new SessionRecord(username, expiresAt));
        return token;
    }

    public synchronized void logout(String token) {
        if (token != null) sessions.remove(token);
    }

    public synchronized AuthenticatedUser authenticate(String token) {
        if (token == null || token.isBlank()) return null;
        SessionRecord session = sessions.get(token);
        if (session == null) return null;
        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return null;
        }
        ProfileRecord profile = profilesByName.getOrDefault(session.username(), new ProfileRecord(session.username(), "", ""));
        AccountRecord account = accountsByName.get(session.username());
        String email = account == null ? "" : account.email();
        return new AuthenticatedUser(session.username(), email, profile.skinUrl(), profile.displayName());
    }

    public synchronized void updateProfile(String username, String skinUrl, String displayName) {
        if (username == null || username.isBlank()) return;
        ProfileRecord profile = new ProfileRecord(
                username,
                sanitizeSkin(skinUrl),
                sanitizeDisplayName(displayName)
        );
        profilesByName.put(username, profile);
        save();
    }

    private String sanitizeSkin(String skinUrl) {
        if (skinUrl == null || skinUrl.isBlank()) return "";
        String trimmed = skinUrl.trim();
        if (!(trimmed.startsWith("http://") || trimmed.startsWith("https://"))) return "";
        return trimmed;
    }

    private String sanitizeDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) return "";
        String trimmed = displayName.trim();
        return trimmed.length() > 32 ? trimmed.substring(0, 32) : trimmed;
    }

    private String normalize(String username) {
        if (username == null) return null;
        String normalized = username.trim().toLowerCase();
        if (normalized.length() < 3 || normalized.length() > 16) return null;
        for (char c : normalized.toCharArray()) {
            boolean valid = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.';
            if (!valid) return null;
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        if (email == null) return null;
        String normalized = email.trim().toLowerCase();
        if (normalized.length() < 5 || normalized.length() > 254) return null;
        if (!EMAIL_PATTERN.matcher(normalized).matches()) return null;
        return normalized;
    }

    private byte[] hash(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, HASH_LENGTH * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash password", e);
        }
    }

    private synchronized void load() throws IOException {
        if (Files.exists(accountsPath)) {
            String content = Files.readString(accountsPath, StandardCharsets.UTF_8);
            Map<String, AccountRecord> loaded = gson.fromJson(content, ACCOUNT_MAP_TYPE);
            if (loaded != null) {
                accountsByName = new HashMap<>(loaded);
                usernamesByEmail = new HashMap<>();
                for (AccountRecord value : accountsByName.values()) {
                    String normalizedEmail = normalizeEmail(value.email());
                    if (normalizedEmail != null) usernamesByEmail.put(normalizedEmail, value.username());
                }
            }
        }
        if (Files.exists(profilesPath)) {
            String content = Files.readString(profilesPath, StandardCharsets.UTF_8);
            Type profileType = new TypeToken<Map<String, ProfileRecord>>() {
            }.getType();
            Map<String, ProfileRecord> loaded = gson.fromJson(content, profileType);
            if (loaded != null) profilesByName = new HashMap<>(loaded);
        }
    }

    private synchronized void save() {
        try {
            Files.createDirectories(accountsPath.getParent());
            Files.writeString(accountsPath, gson.toJson(accountsByName), StandardCharsets.UTF_8);
            Files.writeString(profilesPath, gson.toJson(profilesByName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save account data", e);
        }
    }

    public record RegisterResult(boolean success, String error) {
        static RegisterResult ok() {
            return new RegisterResult(true, "");
        }

        static RegisterResult conflict() {
            return new RegisterResult(false, "username_exists");
        }

        static RegisterResult conflictEmail() {
            return new RegisterResult(false, "email_exists");
        }

        static RegisterResult invalid(String field) {
            return new RegisterResult(false, "invalid_" + field);
        }
    }

    private record AccountRecord(String username, String email, String saltBase64, String hashBase64, String createdAt) {
    }

    private record ProfileRecord(String username, String skinUrl, String displayName) {
    }

    private record SessionRecord(String username, Instant expiresAt) {
    }

    public record AuthenticatedUser(String username, String email, String skinUrl, String displayName) {
    }
}
