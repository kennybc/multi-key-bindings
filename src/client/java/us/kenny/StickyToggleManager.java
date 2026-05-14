package us.kenny;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.StickyMultiKeyBinding;
import us.kenny.mixin.KeyMappingAccessor;
import us.kenny.mixin.ToggleKeyMappingAccessor;

/**
 * Owns the "multi.toggle.key.*" bindings that flip a base Minecraft
 * sticky-toggle game option (toggle-sneak, toggle-sprint, toggle-use,
 * toggle-attack) when pressed.
 */
public class StickyToggleManager {
    /**
     * Maps a "multi.toggle.key.*" action to the UUID of the binding that
     * represents its primary row in the binds UI. Persisted so that the same
     * binding (and its modifier chord) stays primary across launches.
     */
    private static final Map<String, UUID> PRIMARIES = new HashMap<>();
    private static final String ACTION_PREFIX = "multi.toggle.";
    public static final Category STICKY_TOGGLES_CATEGORY = Category
            .register(Identifier.fromNamespaceAndPath("multi-key-bindings", "sticky_toggles"));

    /**
     * One row per base Minecraft sticky-toggle option. The setter and getter
     * flip and read the option's toggle-mode flag; shouldRestore drives
     * sticky-binding state restoration after a toggle-off cycle.
     */
    public record ToggleOption(Consumer<Boolean> setter, BooleanSupplier getter, boolean shouldRestore) {
    }

    /**
     * Action names (without the "multi." prefix) in display order.
     * Used by the binds list UI to render primaries top-to-bottom.
     */
    public static final List<String> STICKY_ACTIONS = List.of(
            "toggle.key.sneak",
            "toggle.key.sprint",
            "toggle.key.use",
            "toggle.key.attack");

    private static Map<String, ToggleOption> TOGGLE_OPTIONS;

    private static Map<String, ToggleOption> toggleOptions() {
        if (TOGGLE_OPTIONS == null) {
            Options gameOptions = MultiKeyBindingManager.getGameOptions();
            TOGGLE_OPTIONS = Map.of(
                    "key.sneak",
                    new ToggleOption(gameOptions.toggleCrouch()::set, gameOptions.toggleCrouch()::get, true),
                    "key.sprint",
                    new ToggleOption(gameOptions.toggleSprint()::set, gameOptions.toggleSprint()::get, true),
                    "key.use",
                    new ToggleOption(gameOptions.toggleUse()::set, gameOptions.toggleUse()::get, false),
                    "key.attack",
                    new ToggleOption(gameOptions.toggleAttack()::set, gameOptions.toggleAttack()::get, true));
        }
        return TOGGLE_OPTIONS;
    }

    /**
     * Look up the ToggleOption for a base KeyMapping action name (e.g.
     * "key.sneak"), or null if the action isn't one of the four sticky-toggle
     * options.
     *
     * @see ToggleOption
     */
    public static ToggleOption getToggleOption(String baseAction) {
        return toggleOptions().get(baseAction);
    }

    public static boolean isPrimary(MultiKeyBinding binding) {
        return binding != null && binding.getId().equals(PRIMARIES.get(binding.getAction()));
    }

    public static UUID getPrimaryId(String action) {
        return PRIMARIES.get(action);
    }

    public static void setPrimary(String action, UUID bindingId) {
        PRIMARIES.put(action, bindingId);
    }

    public static boolean isToggleAction(String action) {
        return action != null && action.startsWith(ACTION_PREFIX);
    }

    /**
     * Strip the "multi." prefix from an action so it matches the base
     * KeyMapping naming. Pass-through if the action is not prefixed.
     *
     * @see KeyMapping#get(String)
     * @see MultiKeyBindingManager#getKeyBindings(String)
     */
    public static String stripMultiPrefix(String action) {
        return action != null && action.startsWith("multi.") ? action.substring("multi.".length()) : action;
    }

    /**
     * Ensure each toggle action has a designated primary binding. Called after
     * config load so primaries persist across launches under their original
     * UUIDs, preserving any modifier chord stored against that UUID.
     * If PRIMARIES has no entry for an action (fresh install or stale config),
     * promotes the first existing binding rather than creating a redundant
     * empty primary alongside it.
     */
    public static void ensurePrimaries() {
        for (String baseAction : toggleOptions().keySet()) {
            String action = ACTION_PREFIX + baseAction;
            Collection<MultiKeyBinding> existing = MultiKeyBindingManager
                    .getKeyBindings(stripMultiPrefix(action));

            UUID currentPrimaryId = PRIMARIES.get(action);
            MultiKeyBinding primary = currentPrimaryId == null ? null
                    : existing.stream().filter(b -> b.getId().equals(currentPrimaryId)).findFirst().orElse(null);

            if (primary == null) {
                primary = existing.stream().findFirst()
                        .orElseGet(() -> MultiKeyBindingManager.addKeyBinding(action, STICKY_TOGGLES_CATEGORY,
                                InputConstants.UNKNOWN.getName(), UUID.randomUUID()));
                PRIMARIES.put(action, primary.getId());
            }
            if (primary.getCategory() == null) {
                primary.setCategory(STICKY_TOGGLES_CATEGORY);
            }
        }
    }

    /**
     * Flip the base Minecraft "toggle" game option associated with the given
     * action. Called when a key bound to a "multi.toggle.key.*" binding fires.
     */
    public static void flip(String action) {
        if (!isToggleAction(action)) {
            return;
        }
        String baseAction = action.substring(ACTION_PREFIX.length());
        ToggleOption toggle = toggleOptions().get(baseAction);
        if (toggle == null) {
            return;
        }
        boolean newState = !toggle.getter().getAsBoolean();

        // Switching Toggle -> Hold: align each binding's "active" state with the
        // current physical key state. A toggled-on action whose key isn't held
        // gets released; a key still being held stays active. Must run before
        // the option flip so the needsToggle / isToggleMode checks still see
        // toggle mode.
        if (!newState) {
            Window window = Minecraft.getInstance().getWindow();
            KeyMapping baseMapping = KeyMapping.get(baseAction);
            if (baseMapping instanceof ToggleKeyMapping
                    && ((ToggleKeyMappingAccessor) (Object) baseMapping).getNeedsToggle().getAsBoolean()) {
                InputConstants.Key boundKey = ((KeyMappingAccessor) baseMapping).getBoundKey();
                ((KeyMappingAccessor) baseMapping).setIsDown(InputConstants.isKeyDown(window, boundKey.getValue()));
            }
            for (MultiKeyBinding binding : MultiKeyBindingManager.getKeyBindings(baseAction)) {
                if (binding instanceof StickyMultiKeyBinding sticky && sticky.isToggleMode()) {
                    sticky.forceSetPressed(InputConstants.isKeyDown(window, binding.getKey().getValue()));
                }
            }
        }

        toggle.setter().accept(newState);
        announce(baseAction, newState);
    }

    private static void announce(String action, boolean state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null) {
            return;
        }
        Component message = Component.translatable(action)
                .copy()
                .append(": ")
                .append(Component.translatable(state ? "multi.toggle.state.on" : "multi.toggle.state.off")
                        .withStyle(state ? ChatFormatting.GREEN : ChatFormatting.RED));
        mc.gui.setOverlayMessage(message, false);
    }
}
