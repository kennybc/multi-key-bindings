package us.kenny;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ModifierManager {
    private static final Map<String, List<InputConstants.Key>> KEY_MODIFIERS = new HashMap<>();

    /**
     * Checks if a key is a modifier (can modify a key binding, e.g. Shift+Click).
     *
     * @param key The key to check if it is a modifier.
     */
    public static boolean isModifierKey(InputConstants.Key key) {
        if (key.getType() != InputConstants.Type.KEYSYM)
            return false;
        int code = key.getValue();
        return code == GLFW.GLFW_KEY_LEFT_SHIFT || code == GLFW.GLFW_KEY_RIGHT_SHIFT
                || code == GLFW.GLFW_KEY_LEFT_CONTROL || code == GLFW.GLFW_KEY_RIGHT_CONTROL
                || code == GLFW.GLFW_KEY_LEFT_ALT || code == GLFW.GLFW_KEY_RIGHT_ALT;
    }

    /**
     * Checks if all modifiers in the list are currently held down.
     * Returns true if the list is empty.
     *
     * @param modifiers The modifier keys to check.
     */
    public static boolean areModifiersActive(List<InputConstants.Key> modifiers) {
        if (modifiers.isEmpty())
            return true;
        var window = Minecraft.getInstance().getWindow();
        for (InputConstants.Key modifier : modifiers) {
            if (!InputConstants.isKeyDown(window, modifier.getValue()))
                return false;
        }
        return true;
    }

    /**
     * Checks if all required modifiers for the given key are currently held down.
     *
     * @param id The action name or binding UUID string to look up.
     */
    public static boolean areModifiersActive(String id) {
        return areModifiersActive(KEY_MODIFIERS.getOrDefault(id, List.of()));
    }

    /**
     * Set the required modifier keys for a key binding.
     * An empty list removes the modifier requirement.
     *
     * @param id        The action name (e.g. "key.jump") or binding UUID string.
     * @param modifiers The modifier keys to require.
     */
    public static void setModifiers(String id, List<InputConstants.Key> modifiers) {
        List<InputConstants.Key> filtered = modifiers.stream().filter(ModifierManager::isModifierKey).toList();
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
