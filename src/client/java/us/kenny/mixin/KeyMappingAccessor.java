package us.kenny.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("key")
    InputConstants.Key getBoundKey();

    @Accessor("isDown")
    boolean getIsDown();

    @Accessor("isDown")
    void setIsDown(boolean isDown);

    @Accessor("clickCount")
    void setClickCount(int clickCount);
}
