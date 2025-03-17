package us.kenny;

import com.google.gson.Gson;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.kenny.mixin.KeyBindingAccessor;

import java.nio.file.Path;
import java.util.*;

public class MultiKeyBindingManager implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("multi-key-bindings");

    private static final Map<String, List<KeyBinding>> ACTION_TO_KEY_BINDINGS = new HashMap<>();
    private static final Map<InputUtil.Key, List<KeyBinding>> KEY_TO_KEY_BINDINGS = new HashMap<>();
    private static final Map<UUID, KeyBinding> ID_TO_KEY_BINDING = new HashMap<>();

    public static boolean isLoading = false;

    public static KeyBinding addKeyBinding(String action, String translationKey) {
        UUID newId = UUID.randomUUID();
        action = "multi." + action;

        KeyBinding keyBinding = addKeyBindingToMap(action, translationKey, newId);
        ConfigManager.saveConfigFile();

        return keyBinding;
    }

    public static KeyBinding addKeyBindingToMap(String action, String translationKey, UUID newId) {
        InputUtil.Key key = InputUtil.fromTranslationKey(translationKey);
        KeyBinding keyBinding = new KeyBinding(action, -1, newId.toString());
        keyBinding.setBoundKey(key);

        ID_TO_KEY_BINDING.put(newId, keyBinding);
        ACTION_TO_KEY_BINDINGS.computeIfAbsent(action, k -> new ArrayList<>()).add(keyBinding);
        KEY_TO_KEY_BINDINGS.computeIfAbsent(key, k -> new ArrayList<>()).add(keyBinding);

        return keyBinding;
    }

    public static Collection<KeyBinding> getKeyBindings(String action) {
        return ACTION_TO_KEY_BINDINGS.getOrDefault("multi." + action, new ArrayList<>());
    }

    public static Collection<KeyBinding> getKeyBindings(InputUtil.Key key) {
        return KEY_TO_KEY_BINDINGS.getOrDefault(key, new ArrayList<>());
    }

    public static Set<Map.Entry<UUID, KeyBinding>> getKeyBindings() {
        return ID_TO_KEY_BINDING.entrySet();
    }

    public static void setKeyBinding(UUID keyBindingId, InputUtil.Key newKey) {
        KeyBinding keyBinding = ID_TO_KEY_BINDING.get(keyBindingId);
        if (keyBinding == null) return;

        InputUtil.Key oldKey = ((KeyBindingAccessor) keyBinding).getBoundKey();
        if (oldKey == newKey) return;

        // Remove from old key to key binding, add to new one
        List<KeyBinding> keyToKeyBindings = KEY_TO_KEY_BINDINGS.get(oldKey);
        if (keyToKeyBindings != null) {
            keyToKeyBindings.remove(keyBinding);
        }
        KEY_TO_KEY_BINDINGS.computeIfAbsent(newKey, k -> new ArrayList<>()).add(keyBinding);
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
        ConfigManager.saveConfigFile();
    }

    @Override
    public void onInitializeClient() {
        ConfigManager.loadConfigFile();
    }
}