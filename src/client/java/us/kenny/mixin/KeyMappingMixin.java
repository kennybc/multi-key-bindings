package us.kenny.mixin;

import org.objectweb.asm.Opcodes;
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
import us.kenny.StickyToggleManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.StickyMultiKeyBinding;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Collection;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
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
     * Called after a modifier key is released to release every currently-pressed
     * binding whose required modifiers are no longer all held. Toggle bindings
     * are skipped — their toggled state is meant to outlive modifier changes.
     */
    private static void pruneStaleBindings() {
        for (MultiKeyBinding binding : MultiKeyBindingManager.getKeyBindings()) {
            if (!binding.getPressed() || isMultiToggleActive(binding)) {
                continue;
            }
            if (!ModifierManager.areModifiersActive(binding.getId().toString(), binding.getKey(), false)) {
                binding.setPressed(false);
            }
        }
        for (KeyMapping mapping : MultiKeyBindingManager.getGameOptions().keyMappings) {
            KeyMappingAccessor accessor = (KeyMappingAccessor) mapping;
            if (!accessor.getIsDown() || isVanillaToggleActive(mapping)
                    || ModifierManager.getModifiers(mapping.getName()).isEmpty()) {
                continue;
            }
            if (!ModifierManager.areModifiersActive(mapping.getName(), accessor.getBoundKey(), false)) {
                accessor.setIsDown(false);
            }
        }
    }

    /**
     * This covers mocking functionality in "on-demand" actions, where an
     * event is triggered by the single press of a key (for custom key bindings).
     */
    @Inject(method = "click", at = @At("TAIL"))
    private static void onClick(InputConstants.Key key, CallbackInfo ci) {
        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(key);
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            if (ModifierManager.shouldActivate(multiKeyBinding.getId().toString(), multiKeyBinding.getKey())) {
                String action = multiKeyBinding.getAction();
                if (StickyToggleManager.isToggleAction(action)) {
                    StickyToggleManager.flip(action);
                } else {
                    multiKeyBinding.incrementTimesPressed();
                }
            }
        }
    }

    /**
     * @see us.kenny.mixin.KeyMappingMixin#onClick but for gating vanilla bindings.
     */
    @Redirect(method = "click", at = @At(value = "FIELD", target = "Lnet/minecraft/client/KeyMapping;clickCount:I", opcode = Opcodes.PUTFIELD))
    private static void onClickVanilla(KeyMapping keyMapping, int value) {
        for (KeyMapping mapping : MultiKeyBindingManager.getGameOptions().keyMappings) {
            if (!((KeyMappingAccessor) mapping).getBoundKey().equals(((KeyMappingAccessor) keyMapping).getBoundKey())) {
                continue;
            }
            if (ModifierManager.shouldActivate(mapping.getName(), ((KeyMappingAccessor) mapping).getBoundKey())) {
                ((KeyMappingAccessor) mapping).setClickCount(value);
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
            } else if (ModifierManager.shouldActivate(multiKeyBinding.getId().toString(), multiKeyBinding.getKey())) {
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

        // Modifier release can shrink the held set and invalidate active chords,
        // modifier press only grows it, so no prune is needed.
        if (!pressed && ModifierManager.isModifierKey(key)) {
            pruneStaleBindings();
        }
    }

    /**
     * Gates vanilla setDown calls by modifier so toggle bindings don't get
     * silently flipped when the primary key is pressed without modifiers held,
     * and skips GLFW auto-repeat for toggle bindings to prevent rapid re-toggle.
     */
    @Redirect(method = "set", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;setDown(Z)V"))
    private static void onSetVanilla(KeyMapping keyMapping, boolean pressed) {
        boolean repeat = KeyEventManager.isRepeat();
        for (KeyMapping mapping : MultiKeyBindingManager.getGameOptions().keyMappings) {
            if (!((KeyMappingAccessor) mapping).getBoundKey().equals(((KeyMappingAccessor) keyMapping).getBoundKey())) {
                continue;
            }
            if (!pressed) {
                mapping.setDown(false);
                continue;
            }
            if (!ModifierManager.shouldActivate(mapping.getName(), ((KeyMappingAccessor) mapping).getBoundKey())) {
                continue;
            }
            if (repeat && isVanillaToggleActive(mapping)) {
                continue;
            }
            mapping.setDown(true);
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
                boolean keyDown = InputConstants.isKeyDown(
                        Minecraft.getInstance().getWindow().getWindow(),
                        multiKeyBinding.getKey().getValue());
                multiKeyBinding.setPressed(keyDown
                        && ModifierManager.shouldActivate(multiKeyBinding.getId().toString(),
                                multiKeyBinding.getKey()));
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
        for (MultiKeyBinding binding : MultiKeyBindingManager.getKeyBindings(this.getName())) {
            if (binding.getPressed()) {
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
            String id = multiKeyBinding.getId().toString();
            if (keyMatches && (ModifierManager.getModifiers(id).isEmpty()
                    || ModifierManager.areModifiersActive(id, multiKeyBinding.getKey(), true))) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }

        if (!ModifierManager.getModifiers(this.getName()).isEmpty()
                && !ModifierManager.areModifiersActive(this.getName(),
                        ((KeyMappingAccessor) this).getBoundKey(), true)) {
            cir.setReturnValue(false);
            cir.cancel();
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
            String id = multiKeyBinding.getId().toString();
            if (matches && (ModifierManager.getModifiers(id).isEmpty()
                    || ModifierManager.areModifiersActive(id, multiKeyBinding.getKey(), true))) {
                cir.setReturnValue(true);
                cir.cancel();
                return;
            }
        }

        if (!ModifierManager.getModifiers(this.getName()).isEmpty()
                && !ModifierManager.areModifiersActive(this.getName(),
                        ((KeyMappingAccessor) this).getBoundKey(), true)) {
            cir.setReturnValue(false);
            cir.cancel();
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
