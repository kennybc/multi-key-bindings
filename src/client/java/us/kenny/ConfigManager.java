package us.kenny;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import us.kenny.core.MultiKeyBinding;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConfigManager {
    public static final int CONFIG_VERSION = 3;
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("multi-key-bindings.json");
    private static final Gson GSON = new Gson();

    public static boolean isLoading = false;

    /**
     * Save all custom key bindings to a config file.
     */
    public static void saveConfigFile() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            JsonObject json = new JsonObject();
            json.addProperty("config_version", CONFIG_VERSION);
            json.add("bindings", getFormattedKeyBindings());
            json.add("modifiers", getFormattedModifiers());
            GSON.toJson(json, writer);
        } catch (IOException e) {
            MultiKeyBindingClient.LOGGER.error("Failed to save keybindings config", e);
        }
    }

    /**
     * Load custom key bindings from a config file. If config version is outdated,
     * attempt to migrate to latest config file format.
     */
    public static void loadConfigFile() {
        if (!Files.exists(CONFIG_PATH))
            return;

        boolean migrated = false;

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null)
                return;

            int version = json.has("config_version") ? json.get("config_version").getAsInt() : 1;
            if (version < CONFIG_VERSION) {
                MultiKeyBindingClient.LOGGER.info("Config version outdated (found v{}, expected v{}). Upgrading...",
                        version, CONFIG_VERSION);
                json = migrateConfig(json, version);
                migrated = true;
            }

            // Load modifiers before bindings so MultiKeyBinding constructors see their full
            // modifier state via ModifierManager.
            if (json.has("modifiers")) {
                for (var entry : json.getAsJsonObject("modifiers").entrySet()) {
                    List<InputConstants.Key> modifiers = parseModifiers(entry.getValue().getAsJsonArray());
                    if (!modifiers.isEmpty()) {
                        ModifierManager.setModifiers(entry.getKey(), modifiers);
                    }
                }
            }

            if (json.has("bindings")) {
                for (JsonElement element : json.getAsJsonArray("bindings")) {
                    JsonObject keyBindingJson = element.getAsJsonObject();
                    UUID id = UUID.fromString(keyBindingJson.get("id").getAsString());
                    String action = keyBindingJson.get("action").getAsString();
                    String translationKey = keyBindingJson.get("key").getAsString();

                    // Empty category since unknown at startup, it will be filled in later
                    MultiKeyBindingManager.addKeyBinding(action, null, translationKey, id);
                }
            }

            if (migrated) {
                try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                    GSON.toJson(json, writer);
                } catch (IOException e) {
                    MultiKeyBindingClient.LOGGER.error("Failed to save migrated keybindings config", e);
                }
            }
        } catch (IOException e) {
            MultiKeyBindingClient.LOGGER.error("Failed to load config", e);
        } finally {
            isLoading = false;
        }
    }

    /**
     * Migrate a config JSON object to latest format.
     *
     * @param json    The config to migrate.
     * @param version The version of the config we are migrating from.
     */
    private static JsonObject migrateConfig(JsonObject json, int version) {
        JsonObject newConfig = new JsonObject();
        newConfig.addProperty("config_version", CONFIG_VERSION);
        JsonArray newKeyBindings = new JsonArray();

        JsonArray oldKeyBindings = json.getAsJsonArray(version == 1 ? "keyBindings" : "bindings");
        for (JsonElement element : oldKeyBindings) {
            JsonObject oldBinding = element.getAsJsonObject();
            JsonObject newBinding = new JsonObject();

            newBinding.addProperty("id", oldBinding.get("id").getAsString());
            newBinding.addProperty("action", oldBinding.get("action").getAsString());

            if (version == 1 && oldBinding.has("keyCode")) {
                int keyCode = oldBinding.get("keyCode").getAsInt();
                newBinding.addProperty("key", convertKeyCodeToKeyName(keyCode));
            } else {
                newBinding.addProperty("key", oldBinding.get("key").getAsString()); // Already v2 format
            }

            newKeyBindings.add(newBinding);
        }

        newConfig.add("bindings", newKeyBindings);
        newConfig.add("modifiers", new JsonObject());
        return newConfig;
    }

    /**
     * Get all custom key bindings formatted as a JSON array for storage.
     */
    private static JsonArray getFormattedKeyBindings() {
        JsonArray array = new JsonArray();
        for (MultiKeyBinding binding : MultiKeyBindingManager.getKeyBindings()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", binding.getId().toString());
            obj.addProperty("action", binding.getAction());
            obj.addProperty("key", binding.getKey().getName());
            array.add(obj);
        }
        return array;
    }

    /**
     * Get all modifier entries (action name or binding UUID → modifier list) as a
     * JSON object.
     */
    private static JsonObject getFormattedModifiers() {
        JsonObject obj = new JsonObject();
        for (var entry : ModifierManager.getAllModifiers().entrySet()) {
            obj.add(entry.getKey(), serializeModifiers(entry.getValue()));
        }
        return obj;
    }

    /**
     * Serialize a list of modifier keys into a JsonArray.
     *
     * @param modifiers The list of modifier keys to serialize.
     */
    private static JsonArray serializeModifiers(List<InputConstants.Key> modifiers) {
        JsonArray array = new JsonArray();
        for (InputConstants.Key mod : modifiers) {
            array.add(mod.getName());
        }
        return array;
    }

    /**
     * Deserialize a JsonArray into a list of modifier keys.
     *
     * @param array The JSON array of key name strings.
     */
    private static List<InputConstants.Key> parseModifiers(JsonArray array) {
        List<InputConstants.Key> modifiers = new ArrayList<>();
        for (JsonElement el : array) {
            modifiers.add(InputConstants.getKey(el.getAsString()));
        }
        return modifiers;
    }

    /**
     * Convert a keyCode to a key name (e.g. 2 -> key.mouse.middle).
     *
     * @param keyCode The key code to convert.
     */
    private static String convertKeyCodeToKeyName(int keyCode) {
        InputConstants.Type keyType = keyCode <= 10 ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM;
        return keyType.getOrCreate(keyCode).getName();
    }
}
