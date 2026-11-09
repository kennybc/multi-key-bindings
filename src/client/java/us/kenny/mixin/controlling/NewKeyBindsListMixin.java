package us.kenny.mixin.controlling;

import com.blamejared.controlling.client.CustomList;
import com.blamejared.controlling.client.NewKeyBindsList;

import net.minecraft.client.gui.screens.options.controls.KeyBindsList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import us.kenny.HiddenBindingManager;
import us.kenny.MultiKeyBindingManager;
import us.kenny.ToggleManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingScreenHelper;
import us.kenny.core.controlling.ControllingMultiKeyBindingEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Mixin(value = NewKeyBindsList.class, remap = false)
public abstract class NewKeyBindsListMixin {

    /**
     * NewKeyBindsList rebuilds its entry list inside its own constructor, wiping
     * anything that was added during the inherited KeyBindsList.<init> pass.
     * Re-append the toggles section here using Controlling-aware entries
     * so the remove and "+" buttons go through CustomList.allEntries.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void addToggleEntries(CallbackInfo ci) {
        CustomList self = (CustomList) (Object) this;
        boolean editMode = HiddenBindingManager.isEditMode();
        boolean headerAdded = false;
        for (String action : ToggleManager.TOGGLE_ACTIONS) {
            String fullAction = "multi." + action;
            Collection<MultiKeyBinding> bindings = MultiKeyBindingManager.getKeyBindings(action);
            UUID primaryId = ToggleManager.getPrimaryId(fullAction);
            MultiKeyBinding primary = primaryId == null ? null
                    : bindings.stream().filter(b -> b.getId().equals(primaryId)).findFirst().orElse(null);
            if (primary == null) {
                continue;
            }
            // Normal mode respects the hidden set; edit mode always shows
            // primaries so users can toggle them.
            if (!editMode && HiddenBindingManager.isHidden(fullAction)) {
                continue;
            }
            if (!headerAdded) {
                ((CustomListAccessor) self).invokeAddEntry(
                        ((KeyBindsList) self).new CategoryEntry(ToggleManager.TOGGLES_CATEGORY));
                headerAdded = true;
            }
            ControllingMultiKeyBindingEntry primaryEntry = new ControllingMultiKeyBindingEntry(self,
                    (KeyBindsList.Entry) null, primary, true);
            ((CustomListAccessor) self).invokeAddEntry(primaryEntry);

            // Sub-bindings never render in edit mode
            if (editMode) {
                continue;
            }
            for (MultiKeyBinding sub : bindings) {
                if (sub == primary) {
                    continue;
                }
                if (sub.getCategory() == null) {
                    sub.setCategory(ToggleManager.TOGGLES_CATEGORY);
                }
                ((CustomListAccessor) self).invokeAddEntry(
                        new ControllingMultiKeyBindingEntry(self, primaryEntry, sub, false));
            }
        }
    }

    /**
     * Remove any CategoryEntry whose group has no remaining children (all
     * were filtered out). Runs after addToggleEntries so it accounts for
     * both vanilla and MKB toggle categories.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void pruneEmptyCategories(CallbackInfo ci) {
        CustomList self = (CustomList) (Object) this;
        List<KeyBindsList.Entry> children = new ArrayList<>(self.children());
        List<KeyBindsList.Entry> kept = MultiKeyBindingScreenHelper.filterEmptyCategories(children,
                e -> e instanceof NewKeyBindsList.CategoryEntry || e instanceof KeyBindsList.CategoryEntry);

        if (kept.size() != children.size()) {
            self.clearEntries();
            self.allEntries.clear();
            self.allEntries.addAll(kept);
            for (KeyBindsList.Entry entry : kept) {
                self.addEntryInternal(entry);
            }
        }
    }
}
