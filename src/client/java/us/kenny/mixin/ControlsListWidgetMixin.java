package us.kenny.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;

import java.util.ArrayList;
import java.util.Collection;

@Mixin(ControlsListWidget.class)
public abstract class ControlsListWidgetMixin extends EntryListWidget<ControlsListWidget.Entry> {

    public ControlsListWidgetMixin(MinecraftClient client, int width, int height, int y, int itemHeight) {
        super(client, width, height, y, itemHeight);
    }

    /**
     * Injected in the constructor:
     * The injected code will insert any custom bindings into the list of key
     * bindings in the game settings.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(KeybindsScreen parent, MinecraftClient client, CallbackInfo ci) {
        ControlsListWidget self = (ControlsListWidget) (Object) this;

        Collection<ControlsListWidget.Entry> entries = new ArrayList<ControlsListWidget.Entry>();
        for (int i = 0; i < self.children().size(); i++) {
            ControlsListWidget.Entry entry = self.children().get(i);

            entries.add(entry);
            if (entry instanceof ControlsListWidget.KeyBindingEntry) {
                KeyBinding keyBinding = ((KeyBindingEntryAccessor) entry).getBinding();

                // Create and insert a MultiKeyBindingEntry for any custom bindings
                Collection<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager
                        .getKeyBindings(keyBinding.getId());
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