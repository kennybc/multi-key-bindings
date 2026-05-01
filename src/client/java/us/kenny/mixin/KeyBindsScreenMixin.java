package us.kenny.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Util;

import java.util.List;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import us.kenny.ModifierManager;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingScreen;

@Mixin(KeyBindsScreen.class)
public abstract class KeyBindsScreenMixin implements MultiKeyBindingScreen {
    @Shadow
    public long lastKeySelection;
    @Shadow
    private KeyBindsList keyBindsList;
    @Mutable
    @Shadow
    private KeyMapping selectedKey;

    @Unique
    private MultiKeyBinding selectedMultiKeyBinding;

    @Unique
    public KeyMapping getSelectedKey() {
        return this.selectedKey;
    }

    @Unique
    public MultiKeyBinding getSelectedMultiKeyBinding() {
        return this.selectedMultiKeyBinding;
    }

    @Unique
    public void setSelectedKey(KeyMapping keyMapping) {
        this.selectedKey = keyMapping;
    }

    @Unique
    public void setSelectedMultiKeyBinding(MultiKeyBinding multiKeyBinding) {
        this.selectedMultiKeyBinding = multiKeyBinding;
    }

    @Unique
    public void setLastKeySelection(long time) {
        this.lastKeySelection = time;
    }

    /**
     * Handles a key press during binding capture. If the pressed key is a modifier,
     * it is accumulated and the current key is demoted out of the modifier set.
     * Otherwise, the current key is promoted to a modifier and the pressed key
     * becomes the new primary key.
     *
     * @param id         The action name or binding UUID string to update modifiers
     *                   for.
     * @param currentKey The key currently bound.
     * @param pressedKey The key that was just pressed.
     * @param setKey     Callback to apply the new primary key.
     */
    @Unique
    private void handleKeyPress(String id, InputConstants.Key currentKey, InputConstants.Key pressedKey,
            Consumer<InputConstants.Key> setKey, CallbackInfoReturnable<Boolean> cir) {
        if (ModifierManager.isModifierKey(pressedKey)) {
            ModifierManager.addModifier(id, pressedKey);
            ModifierManager.removeModifier(id, currentKey);
        } else {
            ModifierManager.addModifier(id, currentKey);
            setKey.accept(pressedKey);
        }

        this.lastKeySelection = Util.getMillis();
        this.keyBindsList.resetMappingAndUpdateButtons();
        cir.setReturnValue(true);
        cir.cancel();
    }

    /**
     * Injected in the method mouseClicked:
     * Updates selected custom key binding with whatever mouse button was pressed.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void onMouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        InputConstants.Key pressedKey = InputConstants.Type.MOUSE.getOrCreate(mouseButtonEvent.button());

        if (this.selectedKey != null) {
            ModifierManager.addModifier(this.selectedKey.getName(),
                    ((KeyMappingAccessor) this.selectedKey).getBoundKey());
        }

        if (this.selectedMultiKeyBinding != null && !this.selectedMultiKeyBinding.isUnbound()) {
            ModifierManager.addModifier(this.selectedMultiKeyBinding.getId().toString(),
                    this.selectedMultiKeyBinding.getKey());
            MultiKeyBindingManager.setKeyBinding(this.selectedMultiKeyBinding, pressedKey);
            this.selectedMultiKeyBinding = null;
            this.keyBindsList.resetMappingAndUpdateButtons();
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    /**
     * Injected in the method keyPressed:
     * Updates selected custom key binding with whatever key was pressed.
     */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void onKeyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        InputConstants.Key pressedKey = InputConstants.getKey(keyEvent);

        if (keyEvent.isEscape()) {
            if (this.selectedKey != null)
                this.selectedKey.setKey(InputConstants.UNKNOWN);
            if (this.selectedMultiKeyBinding != null) {
                this.selectedMultiKeyBinding.setKey(InputConstants.UNKNOWN);
                ModifierManager.setModifiers(this.selectedMultiKeyBinding.getId().toString(), List.of());
            }
        } else {
            if (this.selectedKey != null) {
                handleKeyPress(
                        this.selectedKey.getName(),
                        ((KeyMappingAccessor) this.selectedKey).getBoundKey(),
                        pressedKey,
                        this.selectedKey::setKey,
                        cir);
            }
            if (this.selectedMultiKeyBinding != null) {
                handleKeyPress(
                        this.selectedMultiKeyBinding.getId().toString(),
                        this.selectedMultiKeyBinding.getKey(),
                        pressedKey,
                        key -> MultiKeyBindingManager.setKeyBinding(this.selectedMultiKeyBinding, key),
                        cir);
            }
        }
    }
}