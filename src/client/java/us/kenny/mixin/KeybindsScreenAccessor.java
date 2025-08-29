package us.kenny.mixin;

import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeybindsScreen.class)
public interface KeybindsScreenAccessor {
    @Accessor("controlsList")
    ControlsListWidget getControlsList();
}