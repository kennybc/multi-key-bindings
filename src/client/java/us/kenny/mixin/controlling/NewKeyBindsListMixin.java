package us.kenny.mixin.controlling;

import java.util.ArrayList;
import java.util.Collection;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.blamejared.controlling.client.CustomList;
import com.blamejared.controlling.client.NewKeyBindsList;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import us.kenny.MultiKeyBindingClient;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;
import us.kenny.mixin.KeyBindsListEntryAccessor;

@Mixin(NewKeyBindsList.class)
public class NewKeyBindsListMixin {

    /**
     * Injected in the constructor:
     * The injected code will insert any custom bindings into the list of key
     * bindings in the game settings.
     */
    /*@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/blamejared/controlling/client/NewKeyBindsList;addEntry(Lnet/minecraft/client/gui/screens/options/controls/KeyBindsList$Entry;)I", ordinal = 1))
    private int onAddEntry(NewKeyBindsList instance,
                           KeyBindsList.Entry entry,
                           Operation<Integer> original) {
        KeyBindsList self = (KeyBindsList) (Object) this;

        int lastIndex = original.call(instance, entry);
        if (entry instanceof NewKeyBindsList.KeyEntry) {
            KeyMapping keyBinding = ((NewKeyBindsList.KeyEntry) entry).getKey();

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
    }*/
}
