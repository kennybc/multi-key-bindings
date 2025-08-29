package us.kenny.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.StickyMultiKeyBinding;

import java.util.Collection;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {
    @Shadow
    public abstract String getTranslationKey();

    @Shadow
    public abstract String getCategory();

    /**
     * This covers mocking functionality in "on-demand" actions, where an
     * event is triggered by the single press of a key.
     */
    @Inject(method = "onKeyPressed", at = @At("TAIL"))
    private static void onOnKeyPressed(InputUtil.Key key, CallbackInfo ci) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(key);
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            multiKeyBinding.incrementTimesPressed();
        }
    }

    /**
     * This covers mocking functionality in "continuous" actions, where
     * the game tests for a key being held down.
     */
    @Inject(method = "setKeyPressed", at = @At("TAIL"))
    private static void onSetKeyPressed(InputUtil.Key key, boolean pressed, CallbackInfo ci) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(key);
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            multiKeyBinding.setPressed(pressed);
        }
    }

    @Inject(method = "updatePressedStates", at = @At("TAIL"))
    private static void onUpdatedPressedStates(CallbackInfo ci) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings();
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            if (multiKeyBinding.getKey().getCategory() == InputUtil.Type.KEYSYM
                    && multiKeyBinding.getKey().getCode() != InputUtil.UNKNOWN_KEY.getCode()) {
                multiKeyBinding.setPressed(InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(),
                        multiKeyBinding.getKey().getCode()));
            }
        }
    }

    @Inject(method = "unpressAll", at = @At("TAIL"))
    private static void onUnpressAll(CallbackInfo ci) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings();
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            multiKeyBinding.reset();
        }
    }

    @Inject(method = "untoggleStickyKeys", at = @At("TAIL"))
    private static void onUntoggleStickyKeys(CallbackInfo ci) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings();
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            if (multiKeyBinding instanceof StickyMultiKeyBinding stickyMultiKeyBinding) {
                stickyMultiKeyBinding.untoggle();
            }
        }
    }

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void onIsPressed(CallbackInfoReturnable<Boolean> cir) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(this.getTranslationKey());
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            if (multiKeyBinding.getPressed()) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
    }

    @Inject(method = "wasPressed", at = @At("HEAD"), cancellable = true)
    private void onWasPressed(CallbackInfoReturnable<Boolean> cir) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(this.getTranslationKey());
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            if (multiKeyBinding.getTimesPressed() != 0) {
                multiKeyBinding.decrementTimesPressed();
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
    }

    @Inject(method = "reset", at = @At("TAIL"))
    private static void onReset(CallbackInfo ci) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings();
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            multiKeyBinding.reset();
        }
    }

    @Inject(method = "matchesKey", at = @At("HEAD"), cancellable = true)
    private void onMatchesKey(int keyCode, int scanCode, CallbackInfoReturnable<Boolean> cir) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(this.getTranslationKey());
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            InputUtil.Key key = multiKeyBinding.getKey();
            boolean matches = keyCode == InputUtil.UNKNOWN_KEY.getCode()
                    ? key.getCategory() == InputUtil.Type.SCANCODE && key.getCode() == scanCode
                    : key.getCategory() == InputUtil.Type.KEYSYM && key.getCode() == keyCode;
            if (matches) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
    }

    @Inject(method = "matchesMouse", at = @At("HEAD"), cancellable = true)
    private void onMatchesMouse(int code, CallbackInfoReturnable<Boolean> cir) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(this.getTranslationKey());
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            InputUtil.Key key = multiKeyBinding.getKey();
            boolean matches = key.getCategory() == InputUtil.Type.MOUSE && key.getCode() == code;
            if (matches) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
    }
}