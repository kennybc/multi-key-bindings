package us.kenny.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import us.kenny.ModifierManager;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.StickyMultiKeyBinding;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

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
            if (ModifierManager.areModifiersActive(multiKeyBinding.getId().toString())) {
                multiKeyBinding.incrementTimesPressed();
            }
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
            if (!pressed) {
                multiKeyBinding.setPressed(false);
            } else if (ModifierManager.areModifiersActive(multiKeyBinding.getId().toString())) {
                multiKeyBinding.setPressed(true);
            }
        }
    }

    @Inject(method = "setAll", at = @At("TAIL"))
    private static void onSetAll(CallbackInfo ci) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings();
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            if (multiKeyBinding.getKey().getType() == InputConstants.Type.KEYSYM
                    && multiKeyBinding.getKey().getValue() != InputConstants.UNKNOWN.getValue()) {
                boolean keyDown = InputConstants.isKeyDown(
                        Minecraft.getInstance().getWindow().getWindow(),
                        multiKeyBinding.getKey().getValue());
                multiKeyBinding
                        .setPressed(keyDown && ModifierManager.areModifiersActive(multiKeyBinding.getId().toString()));
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
        // Check sub-bindings.
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(this.getName());
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            if (multiKeyBinding.getPressed()
                    && ModifierManager.areModifiersActive(multiKeyBinding.getId().toString())) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }

        // Suppress vanilla isDown when primary modifiers are required but not held.
        List<InputConstants.Key> primaryModifiers = ModifierManager.getModifiers(this.getName());
        if (!primaryModifiers.isEmpty() && !ModifierManager.areModifiersActive(primaryModifiers)) {
            cir.setReturnValue(false);
            cir.cancel();
            return;
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

    @Inject(method = "same", at = @At("HEAD"), cancellable = true)
    private void onSame(KeyMapping other, CallbackInfoReturnable<Boolean> cir) {
        if (!ModifierManager.modifiersEqual(
                ModifierManager.getModifiers(this.getName()),
                ModifierManager.getModifiers(other.getName()))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isUnbound", at = @At("RETURN"), cancellable = true)
    private void onIsUnbound(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && !ModifierManager.getModifiers(this.getName()).isEmpty()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private void onMatchesKey(int keyCode, int scanCode, CallbackInfoReturnable<Boolean> cir) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(this.getName());
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            InputConstants.Key multiKey = multiKeyBinding.getKey();
            boolean keyMatches = keyCode == InputConstants.UNKNOWN.getValue()
                    ? multiKey.getType() == InputConstants.Type.SCANCODE && multiKey.getValue() == scanCode
                    : multiKey.getType() == InputConstants.Type.KEYSYM && multiKey.getValue() == keyCode;
            if (keyMatches && ModifierManager.areModifiersActive(multiKeyBinding.getId().toString())) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }

        // If this vanilla binding has primary modifiers, gate the vanilla match on
        // them.
        List<InputConstants.Key> primaryModifiers = ModifierManager.getModifiers(this.getName());
        if (!primaryModifiers.isEmpty()) {
            if (!ModifierManager.areModifiersActive(primaryModifiers)) {
                cir.setReturnValue(false);
                cir.cancel();
            }
            // Modifiers are held — let vanilla check the key normally.
            return;
        }
    }

    @Inject(method = "matchesMouse", at = @At("HEAD"), cancellable = true)
    private void onMatchesMouse(int code, CallbackInfoReturnable<Boolean> cir) {
        // Check sub-bindings.
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(this.getName());
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            InputConstants.Key multiKey = multiKeyBinding.getKey();
            boolean matches = multiKey.getType() == InputConstants.Type.MOUSE
                    && multiKey.getValue() == code;
            if (matches) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }

        // If this vanilla binding has primary modifiers, gate the vanilla match on
        // them.
        List<InputConstants.Key> primaryModifiers = ModifierManager.getModifiers(this.getName());
        if (!primaryModifiers.isEmpty()) {
            if (!ModifierManager.areModifiersActive(primaryModifiers)) {
                cir.setReturnValue(false);
                cir.cancel();
            }
            return;
        }
    }

    @Inject(method = "getTranslatedKeyMessage", at = @At("TAIL"), cancellable = true)
    public void getLocalizedName(CallbackInfoReturnable<Component> cir) {
        InputConstants.Key key = ((KeyMappingAccessor) this).getBoundKey();
        cir.setReturnValue(ModifierManager.getDisplayName(this.getName(), key.getDisplayName()));
    }

    @Inject(method = "isDefault", at = @At("RETURN"), cancellable = true)
    private void onIsDefault(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && !ModifierManager.getModifiers(this.getName()).isEmpty()) {
            cir.setReturnValue(false);
        }
    }
}