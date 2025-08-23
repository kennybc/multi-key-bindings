package us.kenny.mixin.controlling;

import com.blamejared.controlling.client.NewKeyBindsList;
import com.blamejared.controlling.client.NewKeyBindsList.KeyEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = KeyEntry.class, remap = false)
public interface KeyEntryAccessor {
    @Invoker("<init>")
    static KeyEntry create(NewKeyBindsList newKeyBindsList, KeyBinding binding, Text bindingName) {
        return null;
    }

    @Accessor("key")
    KeyBinding getBinding();
}