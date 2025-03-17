package us.kenny;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.option.KeyBinding;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.util.InputUtil;
import us.kenny.mixin.KeyBindingAccessor;


import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class ConfigManager {
    public static final int CONFIG_VERSION = 2;
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("multi-key-bindings.json");
    private static final Gson GSON = new Gson();

    public static void saveConfigFile() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            JsonObject json = new JsonObject();

            // Save ID to KeyBinding
            JsonArray keyBindingsArray = new JsonArray();
            for (Map.Entry<UUID, KeyBinding> entry : MultiKeyBindingManager.getKeyBindings()) {
                JsonObject keyBindingJson = new JsonObject();
                keyBindingJson.addProperty("id", entry.getKey().toString());
                keyBindingJson.addProperty("action", entry.getValue().getTranslationKey());
                keyBindingJson.addProperty("key", ((KeyBindingAccessor) entry.getValue()).getBoundKey().toString());

                keyBindingsArray.add(keyBindingJson);
            }
            json.add("bindings", keyBindingsArray);
            json.addProperty("version", CONFIG_VERSION);

            GSON.toJson(json, writer);
        } catch (IOException e) {
            MultiKeyBindingManager.LOGGER.error("Failed to save keybindings config", e);
        }
    }

    public static void loadConfigFile() {
        if (!Files.exists(CONFIG_PATH)) return;

        MultiKeyBindingManager.isLoading = true;
        boolean migrated = false;

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) return;

            int version = json.has("config_version") ? json.get("config_version").getAsInt() : 1;
            if (version < CONFIG_VERSION) {
                MultiKeyBindingManager.LOGGER.info("Config version outdated (found v{}, expected v{}). Upgrading...", version, CONFIG_VERSION);
                json = migrateConfig(json, version);
                migrated = true;
            }

            if (!json.has("bindings")) return;

            JsonArray keyBindingsArray = json.getAsJsonArray("bindings");
            for (JsonElement element : keyBindingsArray) {
                JsonObject keyBindingJson = element.getAsJsonObject();
                UUID id = UUID.fromString(keyBindingJson.get("id").getAsString());
                String action = keyBindingJson.get("action").getAsString();
                String translationKey = keyBindingJson.get("key").getAsString();

                MultiKeyBindingManager.addKeyBindingToMap(action, translationKey, id);
            }

            if (migrated) {
                try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                    GSON.toJson(json, writer);
                } catch (IOException e) {
                    MultiKeyBindingManager.LOGGER.error("Failed to save migrated keybindings config", e);
                }
            }
        } catch (IOException e) {
            MultiKeyBindingManager.LOGGER.error("Failed to load config", e);
        } finally {
            MultiKeyBindingManager.isLoading = false;
        }
    }

    private static JsonObject migrateConfig(JsonObject json, int version) {
        JsonObject newConfig = new JsonObject();
        newConfig.addProperty("version", CONFIG_VERSION);
        JsonArray newKeyBindings = new JsonArray();

        JsonArray oldKeyBindings = json.getAsJsonArray(version == 1 ? "keyBindings" : "bindings");
        for (JsonElement element : oldKeyBindings) {
            JsonObject oldBinding = element.getAsJsonObject();
            JsonObject newBinding = new JsonObject();

            newBinding.addProperty("id", oldBinding.get("id").getAsString());
            newBinding.addProperty("action", oldBinding.get("action").getAsString());

            if (version == 1 && oldBinding.has("keyCode")) {
                int keyCode = oldBinding.get("keyCode").getAsInt();
                String keyName = convertKeyCodeToKeyName(keyCode);
                newBinding.addProperty("key", keyName);
            } else {
                newBinding.addProperty("key", oldBinding.get("key").getAsString()); // Already v2 format
            }

            newKeyBindings.add(newBinding);
        }

        newConfig.add("bindings", newKeyBindings);
        return newConfig;
    }


    private static String convertKeyCodeToKeyName(int keyCode) {
        if (keyCode <= 6) {
            return "key.mouse." + getMouseButtonName(keyCode);
        } else {
            InputUtil.Key key = InputUtil.fromKeyCode(keyCode, 0);
            return key.getTranslationKey();
        }
    }

    private static String getMouseButtonName(int button) {
        return switch (button) {
            case 0 -> "left";
            case 1 -> "right";
            case 2 -> "middle";
            case 3 -> "button.4";
            case 4 -> "button.5";
            case 5 -> "button.6";
            case 6 -> "button.7";
            default -> "button." + (button + 1);
        };
    }
}
