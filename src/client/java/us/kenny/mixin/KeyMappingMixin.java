package us.kenny.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.StickyMultiKeyBinding;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Collection;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @Shadow
    public abstract String getName();

    /**
     * This covers mocking functionality in "on-demand" actions, where an
     * event is triggered by the single press of a key.
     */
    @Inject(method = "click", at = @At("TAIL"))
    private static void onClick(InputConstants.Key key, CallbackInfo ci) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(key);
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            multiKeyBinding.incrementTimesPressed();
        }
    }

    /**
     * This covers mocking functionality in "continuous" actions, where
     * the game tests for a key being held down.
     */
    @Inject(method = "set", at = @At("TAIL"))
    private static void onSet(InputConstants.Key key, boolean pressed, CallbackInfo ci) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(key);
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            multiKeyBinding.setPressed(pressed);
        }
    }

    @Inject(method = "setAll", at = @At("TAIL"))
    private static void onSetAll(CallbackInfo ci) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings();
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            if (multiKeyBinding.getKey().getType() == InputConstants.Type.KEYSYM
                    && multiKeyBinding.getKey().getValue() != InputConstants.UNKNOWN.getValue()) {
                multiKeyBinding.setPressed(InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(),
                        multiKeyBinding.getKey().getValue()));
            }
        }
    }

    @Inject(method = "releaseAll", at = @At("TAIL"))
    private static void onReleaseAll(CallbackInfo ci) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings();
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            multiKeyBinding.release();
        }
    }

    @Inject(method = "resetToggleKeys", at = @At("TAIL"))
    private static void onResetToggleKeys(CallbackInfo ci) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings();
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            if (multiKeyBinding instanceof StickyMultiKeyBinding stickyMultiKeyBinding) {
                stickyMultiKeyBinding.untoggle();
            }
        }
    }

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void onIsDown(CallbackInfoReturnable<Boolean> cir) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(this.getName());
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            if (multiKeyBinding.getPressed()) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    private void onConsumeClick(CallbackInfoReturnable<Boolean> cir) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(this.getName());
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            if (multiKeyBinding.getTimesPressed() != 0) {
                multiKeyBinding.decrementTimesPressed();
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
    }

    @Inject(method = "release", at = @At("TAIL"))
    private static void onRelease(CallbackInfo ci) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings();
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            multiKeyBinding.release();
        }
    }

    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private void onMatchesKey(int keyCode, int scanCode, CallbackInfoReturnable<Boolean> cir) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(this.getName());
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            InputConstants.Key key = multiKeyBinding.getKey();
            boolean matches = keyCode == InputConstants.UNKNOWN.getValue()
                    ? key.getType() == InputConstants.Type.SCANCODE && key.getValue() == scanCode
                    : key.getType() == InputConstants.Type.KEYSYM && key.getValue() == keyCode;
            if (matches) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
    }

    @Inject(method = "matchesMouse", at = @At("HEAD"), cancellable = true)
    private void onMatchesMouse(int code, CallbackInfoReturnable<Boolean> cir) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(this.getName());
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            InputConstants.Key key = multiKeyBinding.getKey();
            boolean matches = key.getType() == InputConstants.Type.MOUSE && key.getValue() == code;
            if (matches) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }
    }
}