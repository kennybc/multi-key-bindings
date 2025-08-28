package us.kenny.core;

import com.blamejared.controlling.client.CustomList;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import us.kenny.MultiKeyBindingManager;
import us.kenny.mixin.ControlsListWidgetAccessor;

public class ControllingMultiKeyBindingEntry extends MultiKeyBindingEntry {
    public ControllingMultiKeyBindingEntry(final CustomList customList, final MultiKeyBinding multiKeyBinding) {
        super(customList, multiKeyBinding);

        this.parentScreen = ((MultiKeyBindingScreen) ((ControlsListWidgetAccessor) customList)
                .getParent());

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
