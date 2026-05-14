package us.kenny.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import us.kenny.MultiKeyBindingManager;
import us.kenny.StickyToggleManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;

import java.util.Collection;
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

        int lastIndex = original.call(instance, entry);
        if (entry instanceof KeyBindsList.KeyEntry) {
            KeyMapping keyBinding = ((KeyBindsListEntryAccessor) entry).getKeyMapping();

            // Create and insert a MultiKeyBindingEntry for any custom bindings
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
    private void addToggleStickyEntries(CallbackInfo ci) {
        KeyBindsList self = (KeyBindsList) (Object) this;
        // Subclasses (e.g. Controlling's CustomList/NewKeyBindsList) rebuild entries
        // in their own constructor and want their own toggle-section injection.
        if (self.getClass() != KeyBindsList.class) {
            return;
        }
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
                this.addEntry(self.new CategoryEntry(StickyToggleManager.STICKY_TOGGLES_CATEGORY));
                headerAdded = true;
            }
            this.addEntry(new MultiKeyBindingEntry(self, primary, true));

            for (MultiKeyBinding sub : bindings) {
                if (sub == primary) {
                    continue;
                }
                if (sub.getCategory() == null) {
                    sub.setCategory(StickyToggleManager.STICKY_TOGGLES_CATEGORY);
                }
                this.addEntry(new MultiKeyBindingEntry(self, sub));
            }
        }
    }
}