package us.kenny.mixin.controlling;

import com.blamejared.controlling.client.CustomList;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CustomList.class)
public interface CustomListAccessor {
    @Invoker("addEntry")
    public int invokeAddEntry(ControlsListWidget.Entry ent);
}