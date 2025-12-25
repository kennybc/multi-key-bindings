package us.kenny.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;

import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;

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
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(KeyBindsScreen keyBindsScreen, Minecraft client, CallbackInfo ci) {
        KeyBindsList self = (KeyBindsList) (Object) this;

        Collection<KeyBindsList.Entry> entries = new ArrayList<KeyBindsList.Entry>();
        for (int i = 0; i < self.children().size(); i++) {
            KeyBindsList.Entry entry = self.children().get(i);

            entries.add(entry);
            if (entry instanceof KeyBindsList.KeyEntry) {
                KeyMapping keyBinding = ((KeyBindsListEntryAccessor) entry).getKeyMapping();

                // Create and insert a MultiKeyBindingEntry for any custom bindings
                Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager
                        .getKeyBindings(keyBinding.getName());
                for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
                    multiKeyBinding.setCategory(keyBinding.getCategory());
                    MultiKeyBindingEntry multiKeyBindingEntry = new MultiKeyBindingEntry(self, multiKeyBinding);

                    entries.add(multiKeyBindingEntry);
                }
            }
        }

        this.replaceEntries(entries);
    }
}