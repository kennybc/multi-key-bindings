package us.kenny.mixin.controlling;

import com.blamejared.controlling.api.DisplayMode;
import com.blamejared.controlling.client.NewKeyBindsList.KeyEntry;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;
import us.kenny.core.controlling.ControllingHideableKeyEntry;

import java.util.Collection;
import java.util.function.Predicate;

@Mixin(value = DisplayMode.class, remap = false)
public abstract class DisplayModeMixin {

    @Invoker("<init>")
    private static DisplayMode init(String name, int id, Predicate<KeyEntry> predicate) {
        throw new AssertionError(); // unreachable statement
    }

    /**
     * Overwrites the "CONFLICTING" enum: we need to check if it conflicts with
     * custom key bindings as well.
     */
    @Redirect(method = "<clinit>()V", at = @At(value = "NEW", ordinal = 2, target = "Lcom/blamejared/controlling/api/DisplayMode;<init>(Ljava/lang/String;ILjava/util/function/Predicate;)Lcom/blamejared/controlling/api/DisplayMode;"))
    private static DisplayMode onConflicting(String name, int id, Predicate<KeyEntry> predicate) {
        return init(name, id, entry -> {
            for (MultiKeyBinding mkb : MultiKeyBindingManager.getKeyBindings()) {
                if (!mkb.getKey().equals(InputUtil.UNKNOWN_KEY) &&
                        mkb.getKey().getTranslationKey()
                                .equals(entry.getKey().getBoundKeyTranslationKey())) {
                    return true;
                }
            }
            return predicate.test(entry);
        });
    }

    /**
     * Tests a custom key binding against the current display mode predicate.
     * 
     * @param multiKeyBinding The custom key binding we are testing.
     * 
     * @see us.kenny.core.MultiKeyBindingEntry#render
     */
    @Unique
    private boolean testMultiKeyBinding(MultiKeyBinding multiKeyBinding) {
        DisplayMode self = (DisplayMode) (Object) this;

        switch (self) {
            case ALL -> {
                return true;
            }
            case NONE -> {
                return multiKeyBinding.getKey().equals(InputUtil.UNKNOWN_KEY);
            }
            case CONFLICTING -> {
                for (KeyBinding kb : MultiKeyBindingManager.getGameOptions().allKeys) {
                    if (!kb.isUnbound() && kb.getBoundKeyTranslationKey()
                            .equals(multiKeyBinding.getKey().getTranslationKey())) {
                        return true;
                    }
                }

                for (MultiKeyBinding mkb : MultiKeyBindingManager.getKeyBindings()) {
                    if (!mkb.getId().equals(multiKeyBinding.getId()) && !mkb.getKey().equals(InputUtil.UNKNOWN_KEY) &&
                            mkb.getKey().getTranslationKey()
                                    .equals(multiKeyBinding.getKey().getTranslationKey())) {
                        return true;
                    }
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * Controlling filters do not support our custom key bindings, so we need to
     * introduce some special behavior and predicate testing.
     * 
     * @see us.kenny.mixin.controlling.NewKeyBindsScreenMixin#onFilterKeysSort
     */
    @ModifyReturnValue(method = "getPredicate", at = @At("RETURN"))
    private Predicate<ControlsListWidget.Entry> onGetPredicate(Predicate<ControlsListWidget.Entry> original) {

        // Wrap original predicate
        return entry -> {
            // Apply filter to custom MultiKeyBinding
            if (entry instanceof MultiKeyBindingEntry multiKeyBindingEntry) {
                MultiKeyBinding multiKeyBinding = ((MultiKeyBindingEntry) multiKeyBindingEntry).getMultiKeyBinding();
                return testMultiKeyBinding(multiKeyBinding);
            }

            // Include original entry if any associated MultiKeyBinding passes the filter
            if (entry instanceof KeyEntry keyEntry) {
                if (entry instanceof ControllingHideableKeyEntry hideableKeyEntry) {
                    if (!original.test(keyEntry)) {
                        hideableKeyEntry.setHidden(true);
                    }
                }

                Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager
                        .getKeyBindings(keyEntry.getKey().getTranslationKey());
                for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
                    if (testMultiKeyBinding(multiKeyBinding))
                        return true;
                }
            }

            return original.test(entry);
        };
    }
}