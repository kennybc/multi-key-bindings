package us.kenny;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import us.kenny.mixin.KeyBindingAccessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MultiKeyBindingManager {
    private static final Map<String, List<KeyBinding>> ACTION_TO_KEY_BINDINGS = new HashMap<>();
    private static final Map<InputUtil.Key, List<KeyBinding>> KEY_TO_KEY_BINDINGS = new HashMap<>();
    private static final Map<UUID, KeyBinding> ID_TO_KEY_BINDING = new HashMap<>();

    /**
     * Create a new key binding and save it to the config file.
     *
     * @see us.kenny.MultiKeyBindingManager#addKeyBinding(String, String, UUID)
     */
    public static KeyBinding addKeyBinding(String action, String translationKey) {
        KeyBinding keyBinding = addKeyBinding(action, translationKey, UUID.randomUUID());
        ConfigManager.saveConfigFile();

        return keyBinding;
    }

    /**
     * Create a new key binding and add it directly to the binding maps.
     *
     * @param action         The name of the in-game action.
     * @param translationKey The string representing the bound key (e.g.
     *                       "key.keyboard.w").
     * @param newId          The ID to set the binding to.
     */
    public static KeyBinding addKeyBinding(String action, String translationKey, UUID newId) {
        InputUtil.Key key = InputUtil.fromTranslationKey(translationKey);
        KeyBinding keyBinding = new KeyBinding(action, -1, newId.toString());
        keyBinding.setBoundKey(key);

        ID_TO_KEY_BINDING.put(newId, keyBinding);
        ACTION_TO_KEY_BINDINGS.computeIfAbsent(action, k -> new ArrayList<>()).add(keyBinding);
        KEY_TO_KEY_BINDINGS.computeIfAbsent(key, k -> new ArrayList<>()).add(keyBinding);

        return keyBinding;
    }

    /**
     * Get the key bindings associated with an action.
     *
     * @param action The name of the in-game action.
     */
    public static Collection<KeyBinding> getKeyBindings(String action) {
        return ACTION_TO_KEY_BINDINGS.getOrDefault("multi." + action, new ArrayList<>());
    }

    /**
     * Get the key bindings associated with a key.
     *
     * @param key The key.
     */
    public static Collection<KeyBinding> getKeyBindings(InputUtil.Key key) {
        return KEY_TO_KEY_BINDINGS.getOrDefault(key, new ArrayList<>());
    }

    public static Set<Map.Entry<UUID, KeyBinding>> getKeyBindings() {
        return ID_TO_KEY_BINDING.entrySet();
    }

    /**
     * Set an existing custom key binding to a new key.
     * -----
     * NOTE: This intentionally does not save the config as that must be done
     * reactively to prevent recursive behavior.
     * 
     * @see us.kenny.mixin.KeyBindingMixin#afterSetBoundKey
     *
     * @param keyBindingId The UUID of the key binding to update.
     * @param newKey       The new key to associate the binding with.
     */
    public static void setKeyBinding(UUID keyBindingId, InputUtil.Key newKey) {
        KeyBinding keyBinding = ID_TO_KEY_BINDING.get(keyBindingId);
        if (keyBinding == null)
            return;

        InputUtil.Key oldKey = ((KeyBindingAccessor) keyBinding).getBoundKey();
        if (oldKey == newKey)
            return;

        // Remove from old key to key binding, add to new one
        List<KeyBinding> keyToKeyBindings = KEY_TO_KEY_BINDINGS.get(oldKey);
        if (keyToKeyBindings != null) {
            keyToKeyBindings.remove(keyBinding);
        }
        KEY_TO_KEY_BINDINGS.computeIfAbsent(newKey, k -> new ArrayList<>()).add(keyBinding);
    }

    /**
     * Remove an existing key binding.
     *
     * @param keyBindingId The UUID of the key binding to remove.
     */
    public static void removeKeyBinding(UUID keyBindingId) {
        KeyBinding keyBinding = ID_TO_KEY_BINDING.remove(keyBindingId);
        if (keyBinding != null) {
            List<KeyBinding> actionToKeyBindings = ACTION_TO_KEY_BINDINGS.get(keyBinding.getTranslationKey());
            if (actionToKeyBindings != null) {
                actionToKeyBindings.remove(keyBinding);
            }
            List<KeyBinding> keyToKeyBindings = KEY_TO_KEY_BINDINGS
                    .get(((KeyBindingAccessor) keyBinding).getBoundKey());
            if (keyToKeyBindings != null) {
                keyToKeyBindings.remove(keyBinding);
            }
        }
        ConfigManager.saveConfigFile();
    }
}