package us.kenny.mixin.controlling;

import com.blamejared.controlling.client.CustomList;
import com.blamejared.controlling.client.NewKeyBindsList.KeyEntry;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;
import us.kenny.core.controlling.ControllingHideableKeyEntry;
import us.kenny.core.controlling.ControllingMultiKeyBindingEntry;

import java.util.Collection;

@Mixin(value = CustomList.class, remap = false)
public abstract class CustomListMixin {

    @Inject(method = "getAllEntries", at = @At("HEAD"))
    private void onGetAllEntries(CallbackInfoReturnable<Integer> cir) {
        CustomList self = (CustomList) (Object) this;
        self.allEntries.forEach(entry -> {
            if (entry instanceof ControllingHideableKeyEntry hideableKeyEntry) {
                hideableKeyEntry.setHidden(false);
            }
        });
    }

    @Inject(method = "addEntry", at = @At("TAIL"))
    private void onAddEntry(ControlsListWidget.Entry entry, CallbackInfoReturnable<Integer> cir) {
        if (!(entry instanceof KeyEntry))
            return;
        CustomList self = (CustomList) (Object) this;
        KeyBinding keyBinding = ((KeyEntry) entry).getKey();

        Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager
                .getKeyBindings(keyBinding.getTranslationKey());
        for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
            multiKeyBinding.setCategory(keyBinding.getCategory());
            MultiKeyBindingEntry multiKeyBindingEntry = new ControllingMultiKeyBindingEntry(self, multiKeyBinding);
            ((CustomListAccessor) self).invokeAddEntry(multiKeyBindingEntry);
        }
    }
}