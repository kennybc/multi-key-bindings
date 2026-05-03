package us.kenny.mixin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingScreen;
import us.kenny.core.MultiKeyBindingScreenHelper;

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
     * Injected in the method mouseClicked:
     * Updates selected custom key binding with whatever mouse button was pressed.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void onMouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (MultiKeyBindingScreenHelper.handleMouseClicked(this, this.keyBindsList, mouseButtonEvent)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Injected in the method keyPressed:
     * Updates selected custom key binding with whatever key was pressed.
     */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void onKeyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (MultiKeyBindingScreenHelper.handleKeyPressed(this, this.keyBindsList, keyEvent)) {
            cir.setReturnValue(true);
        }
    }
}
