package us.kenny.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingScreen;

@Mixin(KeyBindsScreen.class)
public abstract class KeyBindsScreenMixin implements MultiKeyBindingScreen {
    @Shadow
    public long lastKeySelection;
    @Shadow
    private KeyBindsList keyBindsList;

    @Unique
    private MultiKeyBinding selectedMultiKeyBinding;

    @Unique
    public MultiKeyBinding getSelectedMultiKeyBinding() {
        return this.selectedMultiKeyBinding;
    }

    @Unique
    public void setSelectedMultiKeyBinding(MultiKeyBinding multiKeyBinding) {
        this.selectedMultiKeyBinding = multiKeyBinding;
    }

    /**
     * Injected in the method mouseClicked:
     * Updates selected custom key binding with whatever mouse button was pressed.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (this.selectedMultiKeyBinding != null) {
            MultiKeyBindingManager.setKeyBinding(this.selectedMultiKeyBinding,
                    InputConstants.Type.MOUSE.getOrCreate(mouseButtonEvent.button()));
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
        if (this.selectedMultiKeyBinding != null) {
            if (keyEvent.isEscape()) {
                MultiKeyBindingManager.setKeyBinding(this.selectedMultiKeyBinding,
                        InputConstants.UNKNOWN);
            } else {
                MultiKeyBindingManager.setKeyBinding(this.selectedMultiKeyBinding,
                        InputConstants.getKey(keyEvent));
            }

            this.selectedMultiKeyBinding = null;
            this.lastKeySelection = Util.getMillis();
            this.keyBindsList.resetMappingAndUpdateButtons();
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
