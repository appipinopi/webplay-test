package com.bluejoin.addon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.WebApp;
import de.bluecolored.bluemap.common.api.PluginImpl;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.web.RoutingRequestHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class BlueJoinAddon implements Runnable {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern API_ROUTE = Pattern.compile("^/bluejoin/api(?:/.*)?$");

    private boolean registered = false;
    private BlueJoinHttpApi apiHandler;

    @Override
    public void run() {
        BlueMapAPI.onEnable(this::onEnable);
    }

    public void onEnable(BlueMapAPI api) {
        try {
            WebApp webApp = api.getWebApp();
            Path webRoot = webApp.getWebRoot();
            Path dataDirectory = webRoot.resolveSibling("bluejoin-data");
            Path configPath = dataDirectory.resolve("bluejoin-config.json");

            Files.createDirectories(dataDirectory);
            Files.createDirectories(webRoot.resolve("bluejoin"));

            BlueJoinConfig config = loadOrCreateConfig(configPath);
            AccountService accountService = new AccountService(dataDirectory, config.getSessionHours());
            apiHandler = new BlueJoinHttpApi(accountService, config);

            writeWebAssets(webApp, webRoot, config);
            registerApiRoutes(api);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize bluejoin addon", e);
        }
    }

    private BlueJoinConfig loadOrCreateConfig(Path configPath) throws IOException {
        if (!Files.exists(configPath)) {
            BlueJoinConfig config = new BlueJoinConfig();
            Files.writeString(configPath, GSON.toJson(config), StandardCharsets.UTF_8);
            return config;
        }
        BlueJoinConfig loaded = GSON.fromJson(Files.readString(configPath, StandardCharsets.UTF_8), BlueJoinConfig.class);
        return loaded == null ? new BlueJoinConfig() : loaded;
    }

    private void registerApiRoutes(BlueMapAPI api) {
        if (registered) return;
        if (!(api.getPlugin() instanceof PluginImpl pluginImpl)) {
            throw new IllegalStateException("Could not access BlueMap plugin implementation");
        }

        Plugin plugin = pluginImpl.getPlugin();
        RoutingRequestHandler webRoutes = plugin.getWebRequestHandler();
        webRoutes.register(API_ROUTE, apiHandler);
        registered = true;
    }

    private void writeWebAssets(WebApp webApp, Path webRoot, BlueJoinConfig config) throws IOException {
        copyResource("web/bluejoin/index.html", webRoot.resolve("bluejoin/index.html"));
        copyResource("web/bluejoin/login.html", webRoot.resolve("bluejoin/login.html"));
        copyResource("web/bluejoin/register.html", webRoot.resolve("bluejoin/register.html"));
        copyResource("web/bluejoin/mypage.html", webRoot.resolve("bluejoin/mypage.html"));
        copyResource("web/bluejoin/app.css", webRoot.resolve("bluejoin/app.css"));
        copyResource("web/bluejoin/login.js", webRoot.resolve("bluejoin/login.js"));
        copyResource("web/bluejoin/register.js", webRoot.resolve("bluejoin/register.js"));
        copyResource("web/bluejoin/mypage.js", webRoot.resolve("bluejoin/mypage.js"));
        copyResource("web/bluejoin/guard.js", webRoot.resolve("bluejoin/guard.js"));
        copyResource("web/bluejoin/guard.css", webRoot.resolve("bluejoin/guard.css"));
        Files.writeString(
                webRoot.resolve("bluejoin/runtime-config.json"),
                GSON.toJson(config),
                StandardCharsets.UTF_8
        );
        // BlueMap's main index will load these, so map view requires login and spectator controls are hidden.
        webApp.registerScript("/bluejoin/guard.js");
        webApp.registerStyle("/bluejoin/guard.css");
    }

    private void copyResource(String resourcePath, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (InputStream in = BlueJoinAddon.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) throw new IOException("Missing resource: " + resourcePath);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
