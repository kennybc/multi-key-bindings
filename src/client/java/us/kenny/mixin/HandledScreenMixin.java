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
    @Unique
    private boolean isMouseMatch(String action, KeyBinding instance, int code, Operation<Boolean> original) {
        Collection<KeyBinding> keyBindings = MultiKeyBindingManager.getKeyBindings(action);
        for (KeyBinding keyBinding : keyBindings) {
            InputUtil.Key boundKey = ((KeyBindingAccessor) keyBinding).getBoundKey();
            if (boundKey.getCategory() == InputUtil.Type.MOUSE && code == boundKey.getCode()) {
                return true;
            }
        }
        return original.call(instance, code);
    }

    @Unique
    private boolean isKeyMatch(String action, KeyBinding instance, int code, int scanCode, Operation<Boolean> original) {
        Collection<KeyBinding> keyBindings = MultiKeyBindingManager.getKeyBindings(action);
        for (KeyBinding keyBinding : keyBindings) {
            InputUtil.Key boundKey = ((KeyBindingAccessor) keyBinding).getBoundKey();
            if (boundKey.getCategory() == InputUtil.Type.KEYSYM && code == boundKey.getCode()) {
                return true;
            }
        }
        return original.call(instance, code, scanCode);
    }

    @WrapOperation(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;matchesMouse(I)Z"))
    public boolean onMouseClicked(KeyBinding instance, int code, Operation<Boolean> original) {
        return isMouseMatch("key.pickItem", instance, code, original);
    }

    @WrapOperation(method = "mouseReleased", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;matchesMouse(I)Z"))
    public boolean onMouseReleased(KeyBinding instance, int code, Operation<Boolean> original) {
        return isMouseMatch("key.pickItem", instance, code, original);
    }

    @WrapOperation(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;matchesKey(II)Z", ordinal = 0))
    public boolean onInventoryKeyPressed(KeyBinding instance, int code, int scanCode, Operation<Boolean> original) {
        return isKeyMatch("key.inventory", instance, code, scanCode, original);
    }

    @WrapOperation(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;matchesKey(II)Z", ordinal = 1))
    public boolean onPickItemKeyPressed(KeyBinding instance, int code, int scanCode, Operation<Boolean> original) {
        return isKeyMatch("key.pickItem", instance, code, scanCode, original);
    }

    @WrapOperation(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;matchesKey(II)Z", ordinal = 2))
    public boolean onDropItemKeyPressed(KeyBinding instance, int code, int scanCode, Operation<Boolean> original) {
        return isKeyMatch("key.drop", instance, code, scanCode, original);
    }
}