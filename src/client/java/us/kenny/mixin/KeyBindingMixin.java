package us.kenny.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import us.kenny.ConfigManager;
import us.kenny.MultiKeyBindingManager;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {
    @Shadow
    public abstract String getTranslationKey();

    @Shadow
    public abstract String getCategory();


    /**
     * Injected in the isPressed method:
     * The injected code will check to see if any of our custom bindings for this given action are pressed.
     * If any are pressed, set return value to true and exit early.
     * -----
     * NOTE: This covers mocking functionality in "continuous" actions, where the game tests for a key being
     * held down.
     */
    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void onIsPressed(CallbackInfoReturnable<Boolean> cir) {
        // Check if any bound keys from our manager are pressed
        Collection<KeyBinding> keyBindings = MultiKeyBindingManager.getKeyBindings(this.getTranslationKey());
        for (KeyBinding keyBinding : keyBindings) {
            if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), ((KeyBindingAccessor) keyBinding).getBoundKey().getCode())) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    /**
     * Injected in the onKeyPressed method:
     * The injected code will check to see if any of our custom bindings for this given action are pressed.
     * If any are pressed, set return value to true and exit early.
     * -----
     * NOTE: This covers mocking functionality in "on-demand" actions, where an event is triggered by the single
     * press of a key.
     */
    @Inject(method = "onKeyPressed", at = @At("TAIL"))
    private static void onOnKeyPressed(InputUtil.Key key, CallbackInfo ci) {
        Collection<KeyBinding> keyBindings = MultiKeyBindingManager.getKeyBindings(key);
        for (KeyBinding keyBinding : keyBindings) {
            if (keyBinding != null) {
                KeyBinding parentKeyBinding = KeyBindingAccessor.getKeysByIdMap().get(keyBinding.getTranslationKey().substring(6));
                ((KeyBindingAccessor) parentKeyBinding).setTimesPressed(((KeyBindingAccessor) parentKeyBinding).getTimesPressed() + 1);
            }
        }
    }

    /**
     * Injected in the setBoundKey method (at the head):
     * The injected code will check to see if any of our custom bindings are updated in their "dummy" KeyBinding
     * instances.
     * If any are updated, reflect the updates in our manager.
     */
    @Inject(method = "setBoundKey", at = @At("HEAD"))
    private void onSetBoundKey(InputUtil.Key boundKey, CallbackInfo ci) {
        if (getTranslationKey().startsWith("multi.") && !ConfigManager.isLoading) {
            MultiKeyBindingManager.setKeyBinding(UUID.fromString(getCategory()), boundKey);
        }
    }

    /**
     * Injected in the setBoundKey method (at the tail):
     * The injected code will tell our manager to save the changes we just made to our config file.
     * The reason this code is injected is to prevent the circular logic that would occur if we call save()
     * from the manager setKeyBinding(...) method.
     */
    @Inject(method = "setBoundKey", at = @At("TAIL"))
    private void afterSetBoundKey(InputUtil.Key boundKey, CallbackInfo ci) {
        if (getTranslationKey().startsWith("multi.") && !ConfigManager.isLoading) {
            ConfigManager.saveConfigFile();
        }
    }
}