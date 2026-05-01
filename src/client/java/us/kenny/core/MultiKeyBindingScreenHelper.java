package us.kenny.core;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Util;

import us.kenny.ModifierManager;
import us.kenny.MultiKeyBindingManager;
import us.kenny.mixin.KeyMappingAccessor;

import java.util.List;
import java.util.function.Consumer;

/**
 * Shared logic for handling key/mouse input while a binding is being captured
 * on a key binds screen. Used by the vanilla {@code KeyBindsScreen} mixin and
 * the Controlling {@code NewKeyBindsScreen} mixin so that both screens behave
 * identically.
 */
public final class MultiKeyBindingScreenHelper {
    private MultiKeyBindingScreenHelper() {
    }

    /**
     * Returns true when the screen should cancel its {@code mouseClicked} and
     * return {@code true} (i.e. we consumed the click to assign a multi-key
     * binding).
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
     * Returns true when the screen should cancel its {@code keyPressed} and
     * return {@code true}. Escape clears the selected binding(s) but does not
     * cancel — vanilla still handles escape itself.
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
     * If the pressed key is a modifier, accumulate it and demote the current
     * key out of the modifier set. Otherwise promote the current key to a
     * modifier and set the pressed key as the new primary.
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
