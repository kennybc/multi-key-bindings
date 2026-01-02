package us.kenny.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;


@Mixin(KeyBindsList.class)
public interface KeyBindsListAccessor {
    @Accessor("keyBindsScreen")
    KeyBindsScreen getKeyBindsScreen();
}