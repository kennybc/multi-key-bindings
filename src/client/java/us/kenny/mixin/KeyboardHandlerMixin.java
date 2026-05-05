package us.kenny.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Util;
import us.kenny.KeyEventManager;
import us.kenny.core.MultiKeyBindingScreen;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    /**
     * Captures the GLFW action int for the in-progress key event so that
     * downstream KeyMapping logic can distinguish PRESS (1) from REPEAT (2).
     * Vanilla calls KeyMapping.click()/set(true) on both PRESS and REPEAT,
     * which causes modifier-augmented one-click actions to fire continuously
     * and toggle bindings to flip rapidly when held.
     */
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onKeyPressHead(
            long window,
            int action,
            KeyEvent keyEvent,
            CallbackInfo callbackInfo) {
        KeyEventManager.setCurrentAction(action);
    }

    /**
     * Injected in the method keyPress:
     * Clears selected key binding so we can stop tracking modifier key events in
     * the controls screen.
     */
    @Inject(method = "keyPress", at = @At(value = "FIELD", target = "Lnet/minecraft/client/KeyboardHandler;debugCrashKeyTime:J", ordinal = 0, opcode = Opcodes.GETFIELD))
    private void onKeyPress(
            long window,
            int action,
            KeyEvent keyEvent,
            CallbackInfo callbackInfo) {
        if (action == 0 && Minecraft.getInstance().screen instanceof MultiKeyBindingScreen screen) {
            if (screen.getSelectedKey() != null || screen.getSelectedMultiKeyBinding() != null) {
                screen.setSelectedKey(null);
                screen.setSelectedMultiKeyBinding(null);
                screen.setLastKeySelection(Util.getMillis());
            }
        }
    }
}