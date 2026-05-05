package us.kenny;

import us.kenny.core.MultiKeyBinding;
import us.kenny.core.StickyMultiKeyBinding;
import us.kenny.mixin.KeyMappingAccessor;
import us.kenny.mixin.ToggleKeyMappingAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.ToggleKeyMapping;
import java.util.function.BooleanSupplier;

public class MultiKeyBindingManager {
    private static Options gameOptions;

    private static final Map<String, List<MultiKeyBinding>> ACTION_TO_BINDINGS = new HashMap<>();
    private static final Map<InputConstants.Key, List<MultiKeyBinding>> KEY_TO_BINDINGS = new HashMap<>();
    private static final Map<UUID, MultiKeyBinding> ID_TO_BINDING = new HashMap<>();

    private record StickySpec(BooleanSupplier toggleSupplier, boolean shouldRestore) {
    }

    private static Map<String, StickySpec> stickySpecs() {
        return Map.of(
                "multi.key.sneak", new StickySpec(gameOptions.toggleCrouch()::get, true),
                "multi.key.sprint", new StickySpec(gameOptions.toggleSprint()::get, true));
    }

    public static Options getGameOptions() {
        return MultiKeyBindingManager.gameOptions;
    }

    public static void setGameOptions(Options gameOptions) {
        MultiKeyBindingManager.gameOptions = gameOptions;
    }

    /**
     * Create a new key binding (prefixes the action with "multi.").
     *
     * @see MultiKeyBindingManager#addKeyBinding(String, Category, String, UUID)
     */
    public static MultiKeyBinding addKeyBinding(String action, String category, InputConstants.Key key) {
        return addKeyBinding("multi." + action, category, key.getName(), UUID.randomUUID());
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
        InputConstants.Key key = InputConstants.getKey(translationKey);
        MultiKeyBinding multiKeyBinding;

        StickySpec spec = stickySpecs().get(action);
        if (spec != null) {
            multiKeyBinding = new StickyMultiKeyBinding(
                    newId,
                    action,
                    category,
                    key,
                    spec.toggleSupplier(),
                    spec.shouldRestore());
        } else {
            multiKeyBinding = new MultiKeyBinding(newId, action, category, key);
        }

        ID_TO_BINDING.put(newId, multiKeyBinding);
        ACTION_TO_BINDINGS.computeIfAbsent(action, k -> new ArrayList<MultiKeyBinding>()).add(multiKeyBinding);
        KEY_TO_BINDINGS.computeIfAbsent(key, k -> new ArrayList<MultiKeyBinding>()).add(multiKeyBinding);

        return multiKeyBinding;
    }

    /**
     * Get the key bindings associated with an action.
     *
     * @param action The name of the in-game action.
     */
    public static Collection<MultiKeyBinding> getKeyBindings(String action) {
        return ACTION_TO_BINDINGS.getOrDefault("multi." + action, new ArrayList<MultiKeyBinding>());
    }

    /**
     * Get the key bindings associated with a key.
     *
     * @param key The key.
     */
    public static Collection<MultiKeyBinding> getKeyBindings(InputConstants.Key key) {
        return KEY_TO_BINDINGS.getOrDefault(key, new ArrayList<MultiKeyBinding>());
    }

    public static Collection<MultiKeyBinding> getKeyBindings() {
        return ID_TO_BINDING.values();
    }

    /**
     * Propagate a toggle flip across every toggle-mode binding for the given
     * action so a vanilla ToggleKeyMapping and any sticky multi-bindings sharing
     * the action stay in sync.
     *
     * @param action   The vanilla action name (e.g. "key.sneak"). The
     *                 "multi." prefix is stripped if present so callers can
     *                 pass either form.
     * @param newState The post-flip state to propagate.
     */
    public static void syncToggleState(String action, boolean newState) {
        String vanillaAction = action.startsWith("multi.") ? action.substring("multi.".length()) : action;
        KeyMapping vanilla = KeyMappingAccessor.getAll().get(vanillaAction);
        if (vanilla instanceof ToggleKeyMapping
                && ((ToggleKeyMappingAccessor) (Object) vanilla).getNeedsToggle().getAsBoolean()) {
            ((KeyMappingAccessor) vanilla).setIsDown(newState);
        }
        for (MultiKeyBinding binding : getKeyBindings(vanillaAction)) {
            if (binding instanceof StickyMultiKeyBinding sticky && sticky.isToggleMode()) {
                sticky.forceSetPressed(newState);
            }
        }
    }

    /**
     * Set an existing custom key binding to a new key.
     *
     * @param multiKeyBindingId The UUID of the key binding to update.
     * @param newKey            The new key to associate the binding with.
     */
    public static void setKeyBinding(MultiKeyBinding multiKeyBinding, InputConstants.Key newKey) {
        if (multiKeyBinding == null)
            return;

        InputConstants.Key oldKey = multiKeyBinding.getKey();
        multiKeyBinding.setKey(newKey);
        if (!oldKey.equals(newKey)) {
            KEY_TO_BINDINGS.computeIfPresent(oldKey, (k, v) -> {
                v.remove(multiKeyBinding);
                return v;
            });
            KEY_TO_BINDINGS.computeIfAbsent(newKey, k -> new ArrayList<MultiKeyBinding>()).add(multiKeyBinding);
        }
    }

    /**
     * Remove an existing key binding.
     *
     * @param multiKeyBindingId The UUID of the key binding to remove.
     */
    public static void removeKeyBinding(MultiKeyBinding multiKeyBinding) {
        if (multiKeyBinding == null)
            return;

        ModifierManager.setModifiers(multiKeyBinding.getId().toString(), List.of());
        ID_TO_BINDING.remove(multiKeyBinding.getId());
        ACTION_TO_BINDINGS.computeIfPresent(multiKeyBinding.getAction(), (k, v) -> {
            v.remove(multiKeyBinding);
            return v;
        });
        KEY_TO_BINDINGS.computeIfPresent(multiKeyBinding.getKey(), (k, v) -> {
            v.remove(multiKeyBinding);
            return v;
        });
    }
}
