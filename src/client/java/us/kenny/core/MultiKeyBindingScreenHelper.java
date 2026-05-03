package us.kenny.core;

import com.blamejared.controlling.client.NewKeyBindsScreen;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import us.kenny.ModifierManager;
import us.kenny.MultiKeyBindingManager;
import us.kenny.mixin.KeyMappingAccessor;

import java.util.List;
import java.util.function.Consumer;

/**
 * Shared logic for handling key/mouse input while a binding is being captured
 * on a key binds screen. Used by the vanilla mixin and the Controlling mixin so
 * that both screens behave identically.
 * 
 * @see KeyBindsScreen
 * @see NewKeyBindsScreen
 */
public final class MultiKeyBindingScreenHelper {
    private MultiKeyBindingScreenHelper() {
    }

    /**
     * Handle a mouse click while a binding is being captured.
     *
     * @param screen           The key binding screen.
     * @param list             The list widget.
     * @param mouseButtonEvent The mouse button event.
     * @return True if the click was consumed to assign a multi-key binding.
     */
    public static boolean handleMouseClicked(MultiKeyBindingScreen screen, KeyBindsList list,
            MouseButtonEvent mouseButtonEvent) {
        InputConstants.Key pressedKey = InputConstants.Type.MOUSE.getOrCreate(mouseButtonEvent.button());
        KeyMapping selectedKey = screen.getSelectedKey();
        MultiKeyBinding selectedMultiKeyBinding = screen.getSelectedMultiKeyBinding();

        if (selectedKey != null) {
            ModifierManager.addModifier(selectedKey.getName(),
                    ((KeyMappingAccessor) selectedKey).getBoundKey());
        }

        if (selectedMultiKeyBinding != null && !selectedMultiKeyBinding.isUnbound()) {
            ModifierManager.addModifier(selectedMultiKeyBinding.getId().toString(),
                    selectedMultiKeyBinding.getKey());
            MultiKeyBindingManager.setKeyBinding(selectedMultiKeyBinding, pressedKey);
            screen.setSelectedMultiKeyBinding(null);
            list.resetMappingAndUpdateButtons();
            return true;
        }
        return false;
    }

    /**
     * Handle a key press while a binding is being captured.
     *
     * @param screen   The key binding screen.
     * @param list     The list widget.
     * @param keyEvent The key event.
     * @return True if the key was consumed. Escape clears bindings but is not
     *         consumed (vanilla handles it).
     */
    public static boolean handleKeyPressed(MultiKeyBindingScreen screen, KeyBindsList list, KeyEvent keyEvent) {
        InputConstants.Key pressedKey = InputConstants.getKey(keyEvent);
        KeyMapping selectedKey = screen.getSelectedKey();
        MultiKeyBinding selectedMultiKeyBinding = screen.getSelectedMultiKeyBinding();

        if (keyEvent.isEscape()) {
            if (selectedKey != null) {
                selectedKey.setKey(InputConstants.UNKNOWN);
            }
            if (selectedMultiKeyBinding != null) {
                selectedMultiKeyBinding.setKey(InputConstants.UNKNOWN);
                ModifierManager.setModifiers(selectedMultiKeyBinding.getId().toString(), List.of());
            }
            return false;
        }

        boolean handled = false;
        if (selectedKey != null) {
            applyKeyPress(
                    selectedKey.getName(),
                    ((KeyMappingAccessor) selectedKey).getBoundKey(),
                    pressedKey,
                    selectedKey::setKey);
            handled = true;
        }
        if (selectedMultiKeyBinding != null) {
            applyKeyPress(
                    selectedMultiKeyBinding.getId().toString(),
                    selectedMultiKeyBinding.getKey(),
                    pressedKey,
                    key -> MultiKeyBindingManager.setKeyBinding(selectedMultiKeyBinding, key));
            handled = true;
        }

        if (handled) {
            screen.setLastKeySelection(Util.getMillis());
            list.resetMappingAndUpdateButtons();
        }
        return handled;
    }

    /**
     * Accumulate a key press into the binding. If the pressed key is a modifier,
     * add it to the modifier set and demote the current key. Otherwise, promote
     * the current key to a modifier and make the pressed key the new primary bound
     * key.
     *
     * @param id         The binding ID.
     * @param currentKey The current primary bound key.
     * @param pressedKey The newly pressed key.
     * @param setKey     Callback to set the primary bound key.
     */
    private static void applyKeyPress(String id, InputConstants.Key currentKey, InputConstants.Key pressedKey,
            Consumer<InputConstants.Key> setKey) {
        if (ModifierManager.isModifierKey(pressedKey) && !currentKey.equals(InputConstants.UNKNOWN)) {
            ModifierManager.addModifier(id, pressedKey);
            ModifierManager.removeModifier(id, currentKey);
        } else {
            ModifierManager.addModifier(id, currentKey);
            setKey.accept(pressedKey);
        }
    }
}
