package us.kenny.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import us.kenny.MultiKeyBindingManager;

import java.util.Collection;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    /**
     * Tests a key code against a KeyBinding to see if they are associated.
     * We cast the key code to a mouse action.
     *
     * @param instance The key binding we are testing against.
     * @param code     The key code we are testing.
     * @param original The original operation we are wrapping; call the original if we don't detect a match.
     */
    @Unique
    private boolean isMouseMatch(KeyBinding instance, int code, Operation<Boolean> original) {
        Collection<KeyBinding> keyBindings = MultiKeyBindingManager.getKeyBindings(instance.getTranslationKey());
        for (KeyBinding keyBinding : keyBindings) {
            InputUtil.Key boundKey = ((KeyBindingAccessor) keyBinding).getBoundKey();
            if (boundKey.getCategory() == InputUtil.Type.MOUSE && code == boundKey.getCode()) {
                return true;
            }
        }
        return original.call(instance, code);
    }

    /**
     * Tests a key code against a KeyBinding to see if they are associated.
     * We cast the key code to a keyboard action.
     *
     * @param instance The key binding we are testing against.
     * @param code     The key code we are testing.
     * @param original The original operation we are wrapping; call the original if we don't detect a match.
     */
    @Unique
    private boolean isKeyMatch(KeyBinding instance, int code, int scanCode, Operation<Boolean> original) {
        Collection<KeyBinding> keyBindings = MultiKeyBindingManager.getKeyBindings(instance.getTranslationKey());
        for (KeyBinding keyBinding : keyBindings) {
            InputUtil.Key boundKey = ((KeyBindingAccessor) keyBinding).getBoundKey();
            if (boundKey.getCategory() == InputUtil.Type.KEYSYM && code == boundKey.getCode()) {
                return true;
            }
        }
        return original.call(instance, code, scanCode);
    }

    /**
     * The following WrapOperations wrap existing checks on key/mouse state in order to check if any custom
     * key/mouse binds are activated
     */
    @WrapOperation(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;matchesMouse(I)Z"))
    public boolean onMouseClicked(KeyBinding instance, int code, Operation<Boolean> original) {
        return isMouseMatch(instance, code, original);
    }

    @WrapOperation(method = "mouseReleased", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;matchesMouse(I)Z"))
    public boolean onMouseReleased(KeyBinding instance, int code, Operation<Boolean> original) {
        return isMouseMatch(instance, code, original);
    }

    @WrapOperation(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;matchesKey(II)Z"))
    public boolean onKeyPressed(KeyBinding instance, int code, int scanCode, Operation<Boolean> original) {
        return isKeyMatch(instance, code, scanCode, original);
    }

    @WrapOperation(method = "handleHotbarKeyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;matchesKey(II)Z"))
    public boolean onHotbarKeyPressed(KeyBinding instance, int code, int scanCode, Operation<Boolean> original) {
        return isKeyMatch(instance, code, scanCode, original);
    }
}