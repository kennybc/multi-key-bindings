package us.kenny.core.controlling;

import com.blamejared.controlling.client.CustomList;
import com.blamejared.controlling.client.NewKeyBindsList;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import us.kenny.MultiKeyBindingManager;
import us.kenny.StickyToggleManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;
import us.kenny.core.MultiKeyBindingScreen;
import us.kenny.mixin.KeyBindsListAccessor;

import java.util.ArrayList;
import java.util.List;

public class ControllingMultiKeyBindingEntry extends MultiKeyBindingEntry implements ControllingHideableKeyEntry {
    private final KeyBindsList.Entry parentEntry;

    public ControllingMultiKeyBindingEntry(final CustomList parentList, NewKeyBindsList.KeyEntry parentEntry,
            final MultiKeyBinding multiKeyBinding) {
        this(parentList, parentEntry, multiKeyBinding, false);
    }

    public ControllingMultiKeyBindingEntry(final CustomList parentList, KeyBindsList.Entry parentEntry,
            final MultiKeyBinding multiKeyBinding, final boolean primary) {
        super(parentList, multiKeyBinding, primary);

        this.parentEntry = parentEntry;
        this.parentScreen = ((MultiKeyBindingScreen) ((KeyBindsListAccessor) parentList).getKeyBindsScreen());

        if (primary) {
            // CustomList stores a persistent copy of all entries; the "+" button must
            // append the new sub-binding to allEntries and rebuild, otherwise the
            // entry would be lost on the next filter/sort.
            this.addKeyBindingButton = Button.builder(Component.literal("+"), button -> {
                MultiKeyBinding subBinding = MultiKeyBindingManager.addKeyBinding(
                        StickyToggleManager.stripMultiPrefix(multiKeyBinding.getAction()),
                        multiKeyBinding.getCategory(),
                        InputConstants.UNKNOWN);
                ControllingMultiKeyBindingEntry subEntry = new ControllingMultiKeyBindingEntry(parentList,
                        this, subBinding, false);

                List<KeyBindsList.Entry> entries = new ArrayList<>(parentList.children());
                int insertAt = entries.indexOf(this) + 1;
                while (insertAt < entries.size()
                        && entries.get(insertAt) instanceof MultiKeyBindingEntry sibling
                        && !sibling.isPrimary()
                        && sibling.getMultiKeyBinding().getAction().equals(multiKeyBinding.getAction())) {
                    insertAt++;
                }
                entries.add(insertAt, subEntry);

                parentList.allEntries.add(subEntry);
                parentList.clearEntries();
                for (KeyBindsList.Entry entry : entries) {
                    parentList.addEntryInternal(entry);
                }
            }).size(20, 20).build();
        } else {
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
    }

    public KeyBindsList.Entry getParentEntry() {
        return this.parentEntry;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
