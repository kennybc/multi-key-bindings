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
import us.kenny.MultiKeyBinding;
import us.kenny.MultiKeyBindingManager;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {
    @Shadow
    public abstract String getTranslationKey();

    @Final
    @Shadow
    private String translationKey;

    @Shadow
    public abstract InputUtil.Key getDefaultKey();

    @Shadow
    public abstract String getCategory();

    @Shadow
    private int timesPressed;

    @Shadow
    @Final
    private static Map<InputUtil.Key, KeyBinding> KEY_TO_BINDINGS;


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
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(this.getTranslationKey());
        for (MultiKeyBinding binding : multiKeyBindings) {
            if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), binding.getKeyCode())) {
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
    @Inject(method = "onKeyPressed", at = @At("TAIL"), cancellable = true)
    private static void onOnKeyPressed(InputUtil.Key key, CallbackInfo ci) {
        Collection<KeyBinding> customBindings = MultiKeyBindingManager.getKeyBindings(key);
        for (KeyBinding binding : customBindings) {
            if (binding != null) {
                ((KeyBindingAccessor) binding).setTimesPressed(((KeyBindingAccessor) binding).getTimesPressed() + 1);
            }
        }
    }

    /**
     * Injected in the setBoundKey method:
     * The injected code will check to see if any of our custom bindings are updated in their "dummy" KeyBinding
     * instances.
     * If any are updated, reflect the updates in our manager.
     */
    @Inject(method = "setBoundKey", at = @At("HEAD"), cancellable = true)
    private void onSetBoundKey(InputUtil.Key boundKey, CallbackInfo ci) {
        if (!getTranslationKey().startsWith("multi.")) return;
        MultiKeyBindingManager.setKeyBinding(
                getTranslationKey().replaceFirst("^multi.", ""),
                UUID.fromString(getCategory()),
                boundKey.getCode());
    }
}