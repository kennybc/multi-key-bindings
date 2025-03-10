package us.kenny;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.kenny.mixin.KeyBindingAccessor;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MultiKeyBindingManager implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("multi-key-bindings");
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("multi-key-bindings.json");
    private static final Gson GSON = new Gson();

    private static final Map<String, List<KeyBinding>> ACTION_TO_KEY_BINDINGS = new HashMap<>();
    private static final Map<InputUtil.Key, List<KeyBinding>> KEY_TO_KEY_BINDINGS = new HashMap<>();
    private static final Map<UUID, KeyBinding> ID_TO_KEY_BINDING = new HashMap<>();

    private static boolean isLoading = false;

    public static boolean isLoading() {
        return isLoading;
    }

    public static KeyBinding addKeyBinding(String action, int keyCode) {
        UUID newId = UUID.randomUUID();
        action = "multi." + action;

        InputUtil.Key key = InputUtil.Type.KEYSYM.createFromCode(keyCode);
        KeyBinding keyBinding = new KeyBinding(action, -1, newId.toString());
        keyBinding.setBoundKey(key);

        LOGGER.info("Added key binding with key type" + key.getCode());

        ID_TO_KEY_BINDING.put(newId, keyBinding);
        ACTION_TO_KEY_BINDINGS.computeIfAbsent(action, k -> new ArrayList<>()).add(keyBinding);
        KEY_TO_KEY_BINDINGS.computeIfAbsent(key, k -> new ArrayList<>()).add(keyBinding);
        save();

        return keyBinding;
    }

    public static Collection<KeyBinding> getKeyBindings(String action) {
        return ACTION_TO_KEY_BINDINGS.getOrDefault("multi." + action, new ArrayList<>());
    }

    public static Collection<KeyBinding> getKeyBindings(InputUtil.Key key) {
        return KEY_TO_KEY_BINDINGS.getOrDefault(key, new ArrayList<>());
    }

    public static void setKeyBinding(UUID keyBindingId, int newKeyCode) {
        KeyBinding keyBinding = ID_TO_KEY_BINDING.get(keyBindingId);
        if (keyBinding == null) return;

        InputUtil.Key oldKey = ((KeyBindingAccessor) keyBinding).getBoundKey();
        if (oldKey.getCode() == newKeyCode) return;

        // Remove from old key to key binding, add to new one
        List<KeyBinding> keyToKeyBindings = KEY_TO_KEY_BINDINGS.get(oldKey);
        if (keyToKeyBindings != null) {
            keyToKeyBindings.remove(keyBinding);
        }
        KEY_TO_KEY_BINDINGS.computeIfAbsent(InputUtil.Type.KEYSYM.createFromCode(newKeyCode), k -> new ArrayList<>()).add(keyBinding);
    }

    public static void removeKeyBinding(UUID keyBindingId) {
        KeyBinding keyBinding = ID_TO_KEY_BINDING.remove(keyBindingId);
        if (keyBinding != null) {
            List<KeyBinding> actionToKeyBindings = ACTION_TO_KEY_BINDINGS.get(keyBinding.getTranslationKey());
            if (actionToKeyBindings != null) {
                actionToKeyBindings.remove(keyBinding);
            }
            List<KeyBinding> keyToKeyBindings = KEY_TO_KEY_BINDINGS.get(((KeyBindingAccessor) keyBinding).getBoundKey());
            if (keyToKeyBindings != null) {
                keyToKeyBindings.remove(keyBinding);
            }
        }
        save();
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            JsonObject json = new JsonObject();

            // Save ID to KeyBinding
            JsonArray keyBindingsArray = new JsonArray();
            for (Map.Entry<UUID, KeyBinding> entry : ID_TO_KEY_BINDING.entrySet()) {
                JsonObject keyBindingJson = new JsonObject();
                keyBindingJson.addProperty("id", entry.getKey().toString());
                keyBindingJson.addProperty("action", entry.getValue().getTranslationKey());
                keyBindingJson.addProperty("key", ((KeyBindingAccessor) entry.getValue()).getBoundKey().toString());

                keyBindingsArray.add(keyBindingJson);
            }
            json.add("keyBindings", keyBindingsArray);

            GSON.toJson(json, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save keybindings config", e);
        }
    }

    private static void load() {
        if (!Files.exists(CONFIG_PATH)) return;

        isLoading = true;
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null || !json.has("keyBindings")) return;

            JsonArray keyBindingsArray = json.getAsJsonArray("keyBindings");
            for (JsonElement element : keyBindingsArray) {
                JsonObject keyBindingJson = element.getAsJsonObject();
                UUID id = UUID.fromString(keyBindingJson.get("id").getAsString());
                String action = keyBindingJson.get("action").getAsString();
                String translationKey = keyBindingJson.get("key").getAsString();

                InputUtil.Key key = InputUtil.fromTranslationKey(translationKey);
                KeyBinding keyBinding = new KeyBinding(action, -1, id.toString());
                keyBinding.setBoundKey(key);

                ID_TO_KEY_BINDING.put(id, keyBinding);
                ACTION_TO_KEY_BINDINGS.computeIfAbsent(action, k -> new ArrayList<>()).add(keyBinding);
                KEY_TO_KEY_BINDINGS.computeIfAbsent(key, k -> new ArrayList<>()).add(keyBinding);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load config", e);
        } finally {
            isLoading = false;
        }
    }

    @Override
    public void onInitializeClient() {
        load();
    }
}