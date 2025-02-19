package us.kenny.mixin;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {
    @Accessor("timesPressed")
    int getTimesPressed();

    @Accessor("timesPressed")
    void setTimesPressed(int timesPressed);

    @Accessor("boundKey")
    InputUtil.Key getBoundKey();

    @Accessor("KEYS_BY_ID")
    static Map<String, KeyBinding> getKeysByIdMap() {
        throw new AssertionError();
    }
}
