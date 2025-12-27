package us.kenny.mixin.controlling;

import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin({AbstractSelectionList.class})
public interface AbstractSelectionListAccessor {
    @Accessor("children")
    <E> List<E> getChildren();
}