package us.kenny;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.InputUtil;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.StickyMultiKeyBinding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MultiKeyBindingManager {
    private static GameOptions gameOptions;

    private static final Map<String, List<MultiKeyBinding>> ACTION_TO_BINDINGS = new HashMap<>();
    private static final Map<InputUtil.Key, List<MultiKeyBinding>> KEY_TO_BINDINGS = new HashMap<>();
    private static final Map<UUID, MultiKeyBinding> ID_TO_BINDING = new HashMap<>();

    public static GameOptions getGameOptions() {
        return MultiKeyBindingManager.gameOptions;
    }

    public static void setGameOptions(GameOptions gameOptions) {
        MultiKeyBindingManager.gameOptions = gameOptions;
    }

    /**
     * Create a new key binding and save it to the config file. (Prefixes the action
     * with "multi."")
     *
     * @see MultiKeyBindingManager#addKeyBinding(String, String, String, UUID)
     */
    public static MultiKeyBinding addKeyBinding(String action, String category, String translationKey) {
        UUID newId = UUID.randomUUID();
        MultiKeyBinding multiKeyBinding = addKeyBinding("multi." + action, category, translationKey, newId);

        ConfigManager.saveConfigFile();

        return multiKeyBinding;
    }

    /**
     * Create a new key binding and add it directly to the binding maps.
     *
     * @param action         The action (e.g. "multi.key.jump")
     * @param category       The category of the action.
     * @param translationKey The string representing the bound key (e.g.
     *                       "key.keyboard.w").
     * @param newId          The ID to set the binding to.
     */
    public static MultiKeyBinding addKeyBinding(String action, String category, String translationKey, UUID newId) {
        InputUtil.Key key = InputUtil.fromTranslationKey(translationKey);
        MultiKeyBinding multiKeyBinding;

        if (action.equals("multi.key.sneak")) {
            multiKeyBinding = new StickyMultiKeyBinding(newId, action, category, key,
                    gameOptions.getSneakToggled()::getValue);
        } else if (action.equals("multi.key.sprint")) {
            multiKeyBinding = new StickyMultiKeyBinding(newId, action, category, key,
                    gameOptions.getSneakToggled()::getValue);
        } else {
            multiKeyBinding = new MultiKeyBinding(newId, action, category, key);
        }

        MultiKeyBindingClient.LOGGER.info(multiKeyBinding.getAction());

        ID_TO_BINDING.put(newId, multiKeyBinding);
        ACTION_TO_BINDINGS.computeIfAbsent(action, k -> new ArrayList<>()).add(multiKeyBinding);
        KEY_TO_BINDINGS.computeIfAbsent(key, k -> new ArrayList<>()).add(multiKeyBinding);

        return multiKeyBinding;
    }

    /**
     * Get the key bindings associated with an action.
     *
     * @param action The name of the in-game action.
     */
    public static Collection<MultiKeyBinding> getKeyBindings(String action) {
        return ACTION_TO_BINDINGS.getOrDefault("multi." + action, new ArrayList<>());
    }

    /**
     * Get the key bindings associated with a key.
     *
     * @param key The key.
     */
    public static Collection<MultiKeyBinding> getKeyBindings(InputUtil.Key key) {
        return KEY_TO_BINDINGS.getOrDefault(key, new ArrayList<>());
    }

    public static Collection<MultiKeyBinding> getKeyBindings() {
        return ID_TO_BINDING.values();
    }

    /**
     * Set an existing custom key binding to a new key.
     *
     * @param multiKeyBindingId The UUID of the key binding to update.
     * @param newKey            The new key to associate the binding with.
     */
    public static void setKeyBinding(MultiKeyBinding multiKeyBinding, InputUtil.Key newKey) {
        if (multiKeyBinding == null)
            return;

        InputUtil.Key oldKey = multiKeyBinding.getKey();
        if (oldKey == newKey)
            return;

        multiKeyBinding.setKey(newKey);
        KEY_TO_BINDINGS.computeIfPresent(oldKey, (k, v) -> {
            v.remove(multiKeyBinding);
            return v;
        });
        KEY_TO_BINDINGS.computeIfAbsent(newKey, k -> new ArrayList<>()).add(multiKeyBinding);
        ConfigManager.saveConfigFile();
    }

    /**
     * Remove an existing key binding.
     *
     * @param multiKeyBindingId The UUID of the key binding to remove.
     */
    public static void removeKeyBinding(MultiKeyBinding multiKeyBinding) {
        if (multiKeyBinding == null)
            return;

        ID_TO_BINDING.remove(multiKeyBinding.getId());
        ACTION_TO_BINDINGS.computeIfPresent(multiKeyBinding.getAction(), (k, v) -> {
            v.remove(multiKeyBinding);
            return v;
        });
        KEY_TO_BINDINGS.computeIfPresent(multiKeyBinding.getKey(), (k, v) -> {
            v.remove(multiKeyBinding);
            return v;
        });
        ConfigManager.saveConfigFile();
    }
}