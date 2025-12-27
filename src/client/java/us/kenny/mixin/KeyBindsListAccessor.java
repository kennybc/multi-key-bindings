package us.kenny.mixin;

import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(KeyBindsList.class)
public interface KeyBindsListAccessor {
    @Accessor("keyBindsScreen")
    KeyBindsScreen getKeyBindsScreen();
}