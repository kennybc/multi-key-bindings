package us.kenny.mixin;

import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(EntryListWidget.class)
public interface EntryListWidgetAccessor {
    // Access the final field "children" of EntryListWidget to run list operations on
    @Accessor("children")
    List<ControlsListWidget.Entry> getChildren();
}