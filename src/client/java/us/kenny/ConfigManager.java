package us.kenny;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import us.kenny.core.MultiKeyBinding;

import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    public static final int CONFIG_VERSION = 3;

    public static boolean isLoading = false;

    /**
     * Save the active profile's file. Short-circuits during load flows to
     * avoid re-entering save when Options#save triggers our OptionsMixin.
     */
    public static void saveConfigFile() {
        if (isLoading || ProfileManager.isLoading()) {
            return;
        }
        ProfileManager.saveActive();
    }

    /**
     * Client-init entry point. Runs the v3→v4 filesystem migration if
     * applicable, then loads the active profile.
     */
    public static void loadConfigFile() {
        isLoading = true;
        try {
            ProfileManager.bootstrap();
        } finally {
            ToggleManager.ensurePrimaries();
            isLoading = false;
        }
    }

    /**
     * Migrate a legacy JSON config to v3 shape. Preserved as a helper for
     * the v3 -> v4 migration in ProfileManager, which can still
     * inherit older on-disk configs.
     *
     * @param json    The config to migrate.
     * @param version The version of the config we are migrating from.
     */
    static JsonObject migrateJsonToV3(JsonObject json, int version) {
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
    static JsonArray getFormattedKeyBindings() {
        JsonArray array = new JsonArray();
        for (MultiKeyBinding binding : MultiKeyBindingManager.getKeyBindings()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", binding.getId().toString());
            obj.addProperty("action", binding.getAction());
            obj.addProperty("key", binding.getKey().getName());
            if (ToggleManager.isPrimary(binding)) {
                obj.addProperty("primary", true);
            }
            array.add(obj);
        }
        return array;
    }

    /**
     * Get all modifier entries (action name or binding UUID -> modifier list) as a
     * JSON object.
     */
    static JsonObject getFormattedModifiers() {
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
    static List<InputConstants.Key> parseModifiers(JsonArray array) {
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
