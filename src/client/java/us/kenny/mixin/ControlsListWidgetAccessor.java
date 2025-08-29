package us.kenny.mixin;

import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ControlsListWidget.class)
public interface ControlsListWidgetAccessor {
    @Accessor("parent")
    KeybindsScreen getParent();

    @Accessor("maxKeyNameLength")
    int getMaxKeyNameLength();
}