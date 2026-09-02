package us.kenny.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import us.kenny.EditVisibilityMode;
import us.kenny.HiddenBindingManager;
import us.kenny.MultiKeyBindingManager;
import us.kenny.ToggleManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;

@Mixin(KeyBindsList.class)
public abstract class KeyBindsListMixin extends AbstractSelectionList<KeyBindsList.Entry> {

    public KeyBindsListMixin(Minecraft client, int width, int height, int y, int itemHeight) {
        super(client, width, height, y, itemHeight);
    }

    /**
     * Injected in the constructor:
     * The injected code will insert any custom bindings into the list of key
     * bindings in the game settings.
     */
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/options/controls/KeyBindsList;addEntry(Lnet/minecraft/client/gui/components/AbstractSelectionList$Entry;)I", ordinal = 1))
    private int onAddEntry(KeyBindsList instance,
            AbstractSelectionList.Entry<KeyBindsList.Entry> entry,
            Operation<Integer> original) {
        KeyBindsList self = (KeyBindsList) (Object) this;

        if (entry instanceof KeyBindsList.KeyEntry && !EditVisibilityMode.isActive()) {
            KeyMapping keyBinding = ((KeyBindsListEntryAccessor) entry).getKeyMapping();
            if (HiddenBindingManager.isHidden(keyBinding.getName())) {
                return -1;
            }
        }

        int lastIndex = original.call(instance, entry);
        // Multi sub-bindings don't render in edit mode — visibility is a
        // top-level trait, and their parent KeyEntry owns the eye toggle.
        if (entry instanceof KeyBindsList.KeyEntry && !EditVisibilityMode.isActive()) {
            KeyMapping keyBinding = ((KeyBindsListEntryAccessor) entry).getKeyMapping();

            Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager
                    .getKeyBindings(keyBinding.getName());
            for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
                multiKeyBinding.setCategory(keyBinding.getCategory());
                MultiKeyBindingEntry multiKeyBindingEntry = new MultiKeyBindingEntry(self, multiKeyBinding);

                lastIndex = original.call(instance, multiKeyBindingEntry);
            }
        }

        return lastIndex;
    }

    /**
     * After the vanilla list is built, append a primary entry for each
     * "multi.toggle.key.*" action followed by any sub-bindings the user has
     * added for it.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void addToggleEntries(CallbackInfo ci) {
        KeyBindsList self = (KeyBindsList) (Object) this;
        // Subclasses (e.g. Controlling's CustomList/NewKeyBindsList) rebuild entries
        // in their own constructor and want their own toggle-section injection.
        if (self.getClass() != KeyBindsList.class) {
            return;
        }
        boolean editMode = EditVisibilityMode.isActive();
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
            // In normal mode, respect the hidden set for toggle primaries.
            // In edit mode, always show primaries so the user can toggle them.
            if (!editMode && HiddenBindingManager.isHidden(fullAction)) {
                continue;
            }
            if (!headerAdded) {
                this.addEntry(self.new CategoryEntry(ToggleManager.TOGGLES_CATEGORY));
                headerAdded = true;
            }
            this.addEntry(new MultiKeyBindingEntry(self, primary, true));

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
                this.addEntry(new MultiKeyBindingEntry(self, sub));
            }
        }
    }

    /**
     * Remove any category header whose entries were all filtered out.
     * Runs after both the vanilla category/entry adds and our own toggle
     * entries.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void pruneEmptyCategories(CallbackInfo ci) {
        KeyBindsList self = (KeyBindsList) (Object) this;
        if (self.getClass() != KeyBindsList.class) {
            return;
        }
        List<KeyBindsList.Entry> children = new ArrayList<>(self.children());
        List<KeyBindsList.Entry> kept = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            KeyBindsList.Entry entry = children.get(i);
            if (entry instanceof KeyBindsList.CategoryEntry) {
                boolean hasContent = i + 1 < children.size()
                        && !(children.get(i + 1) instanceof KeyBindsList.CategoryEntry);
                if (!hasContent) {
                    continue;
                }
            }
            kept.add(entry);
        }
        if (kept.size() != children.size()) {
            self.replaceEntries(kept);
        }
    }
}