package us.kenny.mixin.controlling;

import com.blamejared.controlling.api.DisplayMode;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;

import java.util.function.Predicate;

@Mixin(value = DisplayMode.class, remap = false)
public abstract class DisplayModeMixin {

    @ModifyReturnValue(method = "getPredicate", at = @At("RETURN"))
    private Predicate<ControlsListWidget.Entry> onGetPredicate(Predicate<ControlsListWidget.Entry> original) {

        // Wrap original predicate
        return entry -> {
            if (entry instanceof MultiKeyBindingEntry multiKeyBindingEntry) {
                DisplayMode self = (DisplayMode) (Object) this;
                MultiKeyBinding multiKeyBinding = ((MultiKeyBindingEntry) multiKeyBindingEntry).getMultiKeyBinding();

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
                            if (!mkb.getKey().equals(InputUtil.UNKNOWN_KEY) &&
                                    mkb.getKey().getTranslationKey()
                                            .equals(multiKeyBinding.getKey().getTranslationKey())) {
                                return true;
                            }
                        }
                        return false;

                    }
                }
            }

            return original.test(entry);
        };
    }
}