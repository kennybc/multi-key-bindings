package us.kenny.mixin.controlling;

import com.blamejared.controlling.client.CustomList;
import com.blamejared.controlling.client.NewKeyBindsList;

import net.minecraft.client.gui.screens.options.controls.KeyBindsList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import us.kenny.MultiKeyBindingManager;
import us.kenny.StickyToggleManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.controlling.ControllingMultiKeyBindingEntry;

import java.util.Collection;
import java.util.UUID;

@Mixin(value = NewKeyBindsList.class, remap = false)
public abstract class NewKeyBindsListMixin {

    /**
     * NewKeyBindsList rebuilds its entry list inside its own constructor, wiping
     * anything that was added during the inherited KeyBindsList.<init> pass.
     * Re-append the sticky-toggle section here using Controlling-aware entries
     * so the remove and "+" buttons go through CustomList.allEntries.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void addToggleStickyEntries(CallbackInfo ci) {
        CustomList self = (CustomList) (Object) this;
        boolean headerAdded = false;
        for (String action : StickyToggleManager.STICKY_ACTIONS) {
            Collection<MultiKeyBinding> bindings = MultiKeyBindingManager.getKeyBindings(action);
            UUID primaryId = StickyToggleManager.getPrimaryId("multi." + action);
            MultiKeyBinding primary = primaryId == null ? null
                    : bindings.stream().filter(b -> b.getId().equals(primaryId)).findFirst().orElse(null);
            if (primary == null) {
                continue;
            }
            if (!headerAdded) {
                ((CustomListAccessor) self).invokeAddEntry(
                        ((KeyBindsList) self).new CategoryEntry(StickyToggleManager.STICKY_TOGGLES_CATEGORY));
                headerAdded = true;
            }
            String category = StickyToggleManager.STICKY_TOGGLES_CATEGORY.getString();
            primary.setCategory(category);
            ControllingMultiKeyBindingEntry primaryEntry = new ControllingMultiKeyBindingEntry(self,
                    (KeyBindsList.Entry) null, primary, true);
            ((CustomListAccessor) self).invokeAddEntry(primaryEntry);

            for (MultiKeyBinding sub : bindings) {
                if (sub == primary) {
                    continue;
                }
                sub.setCategory(category);
                ((CustomListAccessor) self).invokeAddEntry(
                        new ControllingMultiKeyBindingEntry(self, primaryEntry, sub, false));
            }
        }
    }
}
