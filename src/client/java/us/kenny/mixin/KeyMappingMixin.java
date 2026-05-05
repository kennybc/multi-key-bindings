package us.kenny.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import us.kenny.KeyEventManager;
import us.kenny.ModifierManager;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.StickyMultiKeyBinding;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @Shadow
    public abstract String getName();

    private static boolean isVanillaToggleActive(KeyMapping mapping) {
        return mapping instanceof ToggleKeyMapping
                && ((ToggleKeyMappingAccessor) mapping).getNeedsToggle().getAsBoolean();
    }

    private static boolean isMultiToggleActive(MultiKeyBinding binding) {
        return binding instanceof StickyMultiKeyBinding sticky && sticky.isToggleMode();
    }

    /**
     * This covers mocking functionality in "on-demand" actions, where an
     * event is triggered by the single press of a key (for custom key bindings).
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
     * @see us.kenny.mixin.KeyMappingMixin#onClick but for gating vanilla bindings.
     */
    @Redirect(method = "click", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;forAllKeyMappings(Lcom/mojang/blaze3d/platform/InputConstants$Key;Ljava/util/function/Consumer;)V"))
    private static void onClickVanilla(InputConstants.Key key, Consumer<KeyMapping> operation) {
        List<KeyMapping> mappings = KeyMappingAccessor.getMap().get(key);
        if (mappings == null || mappings.isEmpty()) {
            return;
        }
        for (KeyMapping mapping : mappings) {
            List<InputConstants.Key> primaryModifiers = ModifierManager.getModifiers(mapping.getName());
            if (primaryModifiers.isEmpty() || ModifierManager.areModifiersActive(primaryModifiers)) {
                operation.accept(mapping);
            }
        }
    }

    /**
     * This covers mocking functionality in "continuous" actions, where
     * the game tests for a key being held down.
     */
    @Inject(method = "set", at = @At("TAIL"))
    private static void onSet(InputConstants.Key key, boolean pressed, CallbackInfo ci) {
        boolean repeat = KeyEventManager.isRepeat();
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(key);
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            if (!pressed) {
                multiKeyBinding.setPressed(false);
            } else if (ModifierManager.areModifiersActive(multiKeyBinding.getId().toString())) {
                if (repeat && isMultiToggleActive(multiKeyBinding)) {
                    continue;
                }
                multiKeyBinding.setPressed(true);
                if (isMultiToggleActive(multiKeyBinding)) {
                    MultiKeyBindingManager.syncToggleState(multiKeyBinding.getAction(),
                            multiKeyBinding.getPressed());
                }
            }
        }
    }

    /**
     * Gates vanilla setDown calls by modifier so toggle bindings don't get
     * silently flipped when the primary key is pressed without modifiers held,
     * and skips GLFW auto-repeat for toggle bindings to prevent rapid re-toggle.
     */
    @Redirect(method = "set", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;forAllKeyMappings(Lcom/mojang/blaze3d/platform/InputConstants$Key;Ljava/util/function/Consumer;)V"))
    private static void onSetVanilla(InputConstants.Key key, Consumer<KeyMapping> operation,
            InputConstants.Key outerKey, boolean pressed) {
        List<KeyMapping> mappings = KeyMappingAccessor.getMap().get(key);
        if (mappings == null || mappings.isEmpty()) {
            return;
        }
        boolean repeat = KeyEventManager.isRepeat();
        for (KeyMapping mapping : mappings) {
            if (!pressed) {
                operation.accept(mapping);
                continue;
            }
            List<InputConstants.Key> primaryModifiers = ModifierManager.getModifiers(mapping.getName());
            if (!primaryModifiers.isEmpty() && !ModifierManager.areModifiersActive(primaryModifiers)) {
                continue;
            }
            if (repeat && isVanillaToggleActive(mapping)) {
                continue;
            }
            operation.accept(mapping);
            if (isVanillaToggleActive(mapping)) {
                MultiKeyBindingManager.syncToggleState(mapping.getName(),
                        ((KeyMappingAccessor) mapping).getIsDown());
            }
        }
    }

    @Inject(method = "setAll", at = @At("TAIL"))
    private static void onSetAll(CallbackInfo ci) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings();
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            if (multiKeyBinding.getKey().getType() == InputConstants.Type.KEYSYM
                    && multiKeyBinding.getKey().getValue() != InputConstants.UNKNOWN.getValue()) {
                boolean keyDown = InputConstants.isKeyDown(Minecraft.getInstance().getWindow(),
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

    @Inject(method = "restoreToggleStatesOnScreenClosed", at = @At("TAIL"))
    private static void onRestoreToggleStatesOnScreenClosed(CallbackInfo ci) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings();
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            if (multiKeyBinding instanceof StickyMultiKeyBinding stickyMultiKeyBinding) {
                if (stickyMultiKeyBinding.shouldRestoreStateOnScreenClosed()) {
                    stickyMultiKeyBinding.setPressed(true);
                }
            }
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
        // Check sub-bindings. Sticky multi-bindings in toggle mode keep their
        // toggled state alive even after the modifier chord is released.
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(this.getName());
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            if (!multiKeyBinding.getPressed()) {
                continue;
            }
            if (isMultiToggleActive(multiKeyBinding)
                    || ModifierManager.areModifiersActive(multiKeyBinding.getId().toString())) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }

        // Suppress vanilla isDown when primary modifiers are required but not held —
        // except for ToggleKeyMappings in toggle mode, where the toggled state must
        // survive modifier release.
        if (isVanillaToggleActive((KeyMapping) (Object) this)) {
            return;
        }
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
    private void onMatchesKey(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        // Check sub-bindings.
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(this.getName());
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            InputConstants.Key multiKey = multiKeyBinding.getKey();
            boolean keyMatches = keyEvent.key() == InputConstants.UNKNOWN.getValue()
                    ? multiKey.getType() == InputConstants.Type.SCANCODE && multiKey.getValue() == keyEvent.scancode()
                    : multiKey.getType() == InputConstants.Type.KEYSYM && multiKey.getValue() == keyEvent.key();
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
            return;
        }
    }

    @Inject(method = "matchesMouse", at = @At("HEAD"), cancellable = true)
    private void onMatchesMouse(MouseButtonEvent mouseButtonEvent, CallbackInfoReturnable<Boolean> cir) {
        // Check sub-bindings.
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(this.getName());
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            InputConstants.Key multiKey = multiKeyBinding.getKey();
            boolean matches = multiKey.getType() == InputConstants.Type.MOUSE
                    && multiKey.getValue() == mouseButtonEvent.button();
            if (matches && ModifierManager.areModifiersActive(multiKeyBinding.getId().toString())) {
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
