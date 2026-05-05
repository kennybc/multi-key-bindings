package us.kenny.mixin;

import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.ToggleKeyMapping;

@Mixin(ToggleKeyMapping.class)
public interface ToggleKeyMappingAccessor {
    @Accessor("needsToggle")
    BooleanSupplier getNeedsToggle();
}
