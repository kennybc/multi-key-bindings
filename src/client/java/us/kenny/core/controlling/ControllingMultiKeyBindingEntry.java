package us.kenny.core.controlling;

import com.blamejared.controlling.client.CustomList;
import com.blamejared.controlling.client.NewKeyBindsList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;
import us.kenny.core.MultiKeyBindingScreen;
import us.kenny.mixin.KeyBindsListAccessor;

import java.util.ArrayList;
import java.util.List;

public class ControllingMultiKeyBindingEntry extends MultiKeyBindingEntry {
    private final NewKeyBindsList.KeyEntry parentEntry;

    public ControllingMultiKeyBindingEntry(final CustomList parentList, NewKeyBindsList.KeyEntry parentEntry, final MultiKeyBinding multiKeyBinding) {
        super(parentList, multiKeyBinding);

        this.parentEntry = parentEntry;
        this.parentScreen = ((MultiKeyBindingScreen) ((KeyBindsListAccessor) parentList)
                .getKeyBindsScreen());

        /*
         * Controlling stores a persistent copy of all entries because it modifies the
         * displayed list during filter. Therefore the remove key binding button must
         * also remove from this list.
         */
        this.removeKeyBindingButton = Button
                .builder(Component.literal("\uD83D\uDDD1").withStyle(ChatFormatting.RED),
                        (button) -> {
                            MultiKeyBindingManager.removeKeyBinding(multiKeyBinding);
                            this.parentScreen.setSelectedMultiKeyBinding(null);

                            List<KeyBindsList.Entry> entries = new ArrayList<>(parentList.children());
                            entries.remove(this);

                            parentList.allEntries.remove(this);
                            parentList.clearEntries();
                            for (KeyBindsList.Entry entry : entries) {
                                parentList.addEntryInternal(entry);
                            }
                        })
                .size(20, 20)
                .build();
    }

    public KeyBindsList.Entry getParentEntry() {
        return this.parentEntry;
    }
}