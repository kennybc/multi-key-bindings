package us.kenny.mixin.controlling;

import com.blamejared.controlling.client.CustomList;
import com.blamejared.controlling.client.NewKeyBindsList;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import us.kenny.EditVisibilityMode;
import us.kenny.HiddenBindingManager;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;
import us.kenny.core.controlling.ControllingHideableKeyEntry;
import us.kenny.core.controlling.ControllingMultiKeyBindingEntry;

import java.util.Collection;

@Mixin(value = CustomList.class, remap = false)
public abstract class CustomListMixin {

    /**
     * Injected in the getAllEntries method:
     * This is called when filters are changed, so we need to restore all button
     * visibility and then redetermine their visibility from the new filters.
     */
    @Inject(method = "getAllEntries", at = @At("HEAD"))
    private void onGetAllEntries(CallbackInfoReturnable<Integer> cir) {
        CustomList self = (CustomList) (Object) this;
        self.allEntries.forEach(entry -> {
            if (entry instanceof ControllingHideableKeyEntry hideableKeyEntry) {
                hideableKeyEntry.setHidden(false);
            }
        });
    }

    /**
     * Skip adding hidden entries to the list entirely not in edit mode. Runs before
     * the vanilla addEntry logic so hidden rows never enter allEntries.
     */
    @Inject(method = "addEntry(Lnet/minecraft/client/gui/screens/options/controls/KeyBindsList$Entry;)I", at = @At("HEAD"), cancellable = true)
    private void gateHiddenAddEntry(KeyBindsList.Entry entry, CallbackInfoReturnable<Integer> cir) {
        if (EditVisibilityMode.isActive()) {
            return;
        }

        if (entry instanceof NewKeyBindsList.KeyEntry keyEntry) {
            if (HiddenBindingManager.isHidden(keyEntry.getKey().getName())) {
                cir.setReturnValue(-1);
            }
            return;
        }
        if (entry instanceof MultiKeyBindingEntry mkbEntry) {
            String action = mkbEntry.getMultiKeyBinding().getAction();
            if (HiddenBindingManager.isHidden(action)) {
                cir.setReturnValue(-1);
            }
        }
    }

    /**
     * @see us.kenny.core.controlling.ControllingMultiKeyBindingEntry
     */
    @Inject(method = "addEntry(Lnet/minecraft/client/gui/screens/options/controls/KeyBindsList$Entry;)I", at = @At("TAIL"))
    private void onAddEntry(KeyBindsList.Entry entry, CallbackInfoReturnable<Integer> cir) {
        // Only show top level bindings in visibility edit mode
        if (EditVisibilityMode.isActive()) {
            return;
        }

        if (entry instanceof NewKeyBindsList.KeyEntry keyEntry) {
            CustomList self = (CustomList) (Object) this;
            KeyMapping keyBinding = keyEntry.getKey();

            Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager
                    .getKeyBindings(keyBinding.getName());
            for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
                multiKeyBinding.setCategory(keyBinding.getCategory());
                MultiKeyBindingEntry multiKeyBindingEntry = new ControllingMultiKeyBindingEntry(self, keyEntry,
                        multiKeyBinding);
                ((CustomListAccessor) self).invokeAddEntry(multiKeyBindingEntry);
            }
        }
    }
}