package us.kenny.mixin;

import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingScreen;

@Mixin(KeybindsScreen.class)
public abstract class KeybindsScreenMixin implements MultiKeyBindingScreen {
    @Shadow
    public long lastKeyCodeUpdateTime;
    @Shadow
    private ControlsListWidget controlsList;

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
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (this.selectedMultiKeyBinding != null) {
            MultiKeyBindingManager.setKeyBinding(this.selectedMultiKeyBinding,
                    InputUtil.Type.MOUSE.createFromCode(button));
            this.selectedMultiKeyBinding = null;
            this.controlsList.update();
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    /**
     * Injected in the method keyPressed:
     * Updates selected custom key binding with whatever key was pressed.
     */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (this.selectedMultiKeyBinding != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                MultiKeyBindingManager.setKeyBinding(this.selectedMultiKeyBinding,
                        InputUtil.UNKNOWN_KEY);
            } else {
                MultiKeyBindingManager.setKeyBinding(this.selectedMultiKeyBinding,
                        InputUtil.fromKeyCode(keyCode, scanCode));
            }

            this.selectedMultiKeyBinding = null;
            this.lastKeyCodeUpdateTime = Util.getMeasuringTimeMs();
            this.controlsList.update();
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}