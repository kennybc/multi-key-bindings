package us.kenny.mixin;

import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ControlsListWidget.KeyBindingEntry.class)
public interface KeyBindingEntryAccessor {
    @Invoker("<init>")
    static ControlsListWidget.KeyBindingEntry create(ControlsListWidget widget, KeyBinding binding, Text bindingName) {
        return null;
    }

    @Accessor("binding")
    KeyBinding getBinding();
}