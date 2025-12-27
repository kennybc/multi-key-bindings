package us.kenny.mixin.controlling;

import com.blamejared.controlling.client.CustomList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(CustomList.class)
public interface CustomListAccessor {
    @Invoker("addEntry")
    int invokeAddEntry(KeyBindsList.Entry ent);
}