package us.kenny.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;

import java.util.Collection;
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
}