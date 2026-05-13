package us.kenny;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import us.kenny.core.MultiKeyBinding;
import us.kenny.mixin.KeyMappingAccessor;

public class ModifierManager {
    private static final List<InputConstants.Key> ALL_MODIFIERS = List.of(
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_SHIFT),
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_SHIFT),
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_CONTROL),
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_CONTROL),
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_ALT),
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_ALT));
    private static final Map<String, List<InputConstants.Key>> KEY_MODIFIERS = new HashMap<>();

    /**
     * Checks if a key is a modifier (can modify a key binding, e.g. Shift+Click).
     *
     * @param key The key to check if it is a modifier.
     */
    public static boolean isModifierKey(InputConstants.Key key) {
        return ALL_MODIFIERS.contains(key);
    }

    /**
     * Checks if the binding's required modifier set is satisfied by the
     * currently-held modifiers. The binding's primary key is excluded from the
     * held set when it is itself a modifier (e.g. sneak bound to Shift).
     *
     * @param id       The action name (e.g. "key.jump") or binding UUID string.
     * @param boundKey The binding's primary key.
     * @param exact    Held must match required exactly when true; held must be
     *                 a superset of required when false.
     */
    public static boolean areModifiersActive(String id, InputConstants.Key boundKey, boolean exact) {
        List<InputConstants.Key> required = KEY_MODIFIERS.getOrDefault(id, List.of());
        var window = Minecraft.getInstance().getWindow();

        int activeCount = 0;
        for (InputConstants.Key modifier : ALL_MODIFIERS) {
            if (modifier.equals(boundKey) || !InputConstants.isKeyDown(window, modifier.getValue())) {
                continue;
            }
            if (required.contains(modifier)) {
                activeCount++;
            } else if (exact) {
                return false;
            }
        }
        return activeCount == required.size();
    }

    /**
     * Decides whether a binding should fire on a press. A binding with configured
     * modifiers fires only on exact match. A bare binding (no configured
     * modifiers) fires only when no more-specific chords are matched.
     *
     * @param id       The action name (e.g. "key.jump") or binding UUID string.
     * @param boundKey The binding's primary key.
     */
    public static boolean shouldActivate(String id, InputConstants.Key boundKey) {
        if (!getModifiers(id).isEmpty()) {
            return areModifiersActive(id, boundKey, true);
        }

        for (MultiKeyBinding binding : MultiKeyBindingManager.getKeyBindings(boundKey)) {
            if (!getModifiers(binding.getId().toString()).isEmpty()
                    && areModifiersActive(binding.getId().toString(), binding.getKey(), true)) {
                return false;
            }
        }

        List<KeyMapping> mappings = KeyMappingAccessor.getMap().get(boundKey);
        if (mappings != null) {
            for (KeyMapping mapping : mappings) {
                if (!getModifiers(mapping.getName()).isEmpty()
                        && areModifiersActive(mapping.getName(), boundKey, true)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Set the required modifier keys for a key binding.
     * An empty list removes the modifier requirement.
     *
     * @param id        The action name (e.g. "key.jump") or binding UUID string.
     * @param modifiers The modifier keys to require.
     */
    public static void setModifiers(String id, List<InputConstants.Key> modifiers) {
        List<InputConstants.Key> filtered = modifiers.stream().filter(ModifierManager::isModifierKey).distinct()
                .toList();
        if (filtered.isEmpty()) {
            KEY_MODIFIERS.remove(id);
        } else {
            KEY_MODIFIERS.put(id, new ArrayList<>(filtered));
        }
    }

    /**
     * Add a modifier key to the required modifiers for a key binding.
     * Does nothing if the modifier is already present or is not a valid modifier
     * key.
     *
     * @param id       The action name (e.g. "key.jump") or binding UUID string.
     * @param modifier The modifier key to add.
     */
    public static void addModifier(String id, InputConstants.Key modifier) {
        if (!isModifierKey(modifier))
            return;
        KEY_MODIFIERS.computeIfAbsent(id, k -> new ArrayList<>());
        List<InputConstants.Key> modifiers = KEY_MODIFIERS.get(id);
        if (!modifiers.contains(modifier)) {
            modifiers.add(modifier);
        }
    }

    /**
     * Remove a modifier key from the required modifiers for a key binding.
     * Removes the entry entirely if no modifiers remain.
     *
     * @param id       The action name (e.g. "key.jump") or binding UUID string.
     * @param modifier The modifier key to remove.
     */
    public static void removeModifier(String id, InputConstants.Key modifier) {
        List<InputConstants.Key> modifiers = KEY_MODIFIERS.get(id);
        if (modifiers == null)
            return;
        modifiers.remove(modifier);
        if (modifiers.isEmpty()) {
            KEY_MODIFIERS.remove(id);
        }
    }

    /**
     * Get the required modifier keys for a key binding.
     * Returns an empty list if no modifiers are required.
     *
     * @param id The action name (e.g. "key.jump") or binding UUID string.
     */
    public static List<InputConstants.Key> getModifiers(String id) {
        return KEY_MODIFIERS.getOrDefault(id, List.of());
    }

    /**
     * Compare two modifier lists as unordered sets. Used to decide whether
     * two bindings collide: bindings with different modifier requirements do
     * not conflict at runtime and should not be flagged as duplicates.
     */
    public static boolean modifiersEqual(List<InputConstants.Key> a, List<InputConstants.Key> b) {
        if (a.size() != b.size())
            return false;
        return a.containsAll(b);
    }

    /**
     * Build a display name for a key, prefixed with its required modifiers (e.g.
     * "Shift+Space").
     *
     * @param id       The action name (e.g. "key.jump") or binding UUID string.
     * @param baseName The display name of the bound key.
     */
    public static Component getDisplayName(String id, Component baseName) {
        List<InputConstants.Key> modifiers = getModifiers(id);
        if (modifiers.isEmpty())
            return baseName;
        MutableComponent result = Component.empty();
        for (InputConstants.Key modifier : modifiers) {
            result = result.append(modifier.getDisplayName()).append("+");
        }
        return result.append(baseName);
    }

    /**
     * Get all modifier entries (key -> modifier list) for serialization.
     */
    public static Map<String, List<InputConstants.Key>> getAllModifiers() {
        return Collections.unmodifiableMap(KEY_MODIFIERS);
    }
}
