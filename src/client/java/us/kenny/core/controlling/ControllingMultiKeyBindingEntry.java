package us.kenny.core.controlling;

import com.blamejared.controlling.client.CustomList;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;
import us.kenny.core.MultiKeyBindingScreen;
import us.kenny.mixin.ControlsListWidgetAccessor;

public class ControllingMultiKeyBindingEntry extends MultiKeyBindingEntry {
    public ControllingMultiKeyBindingEntry(final CustomList customList, final MultiKeyBinding multiKeyBinding) {
        super(customList, multiKeyBinding);

        this.parentScreen = ((MultiKeyBindingScreen) ((ControlsListWidgetAccessor) customList)
                .getParent());

        /*
         * Controlling stores a persistent copy of all entries because it modifies the
         * displayed list during filter. Therefore the remove key binding button must
         * also remove from this list.
         */
        this.removeKeyBindingButton = ButtonWidget
                .builder(Text.literal("\uD83D\uDDD1").formatted(Formatting.RED),
                        (button) -> {
                            MultiKeyBindingManager.removeKeyBinding(multiKeyBinding);
                            this.parentScreen.setSelectedMultiKeyBinding(null);
                            customList.children().remove(this);
                            customList.allEntries.remove(this);
                            customList.update();
                        })
                .size(20, 20)
                .build();
    }
}
