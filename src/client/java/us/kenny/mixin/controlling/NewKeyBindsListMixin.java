package us.kenny.mixin.controlling;

import com.blamejared.controlling.client.CustomList;
import com.blamejared.controlling.client.NewKeyBindsList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(NewKeyBindsList.class)
public abstract class NewKeyBindsListMixin extends CustomList {

    public NewKeyBindsListMixin(KeybindsScreen controls, MinecraftClient mcIn) {
        super(controls, mcIn);
    }

    @ModifyVariable(method = "<init>", at = @At(value = "LOAD", ordinal = 1))
    private KeyBinding[] modifyKeyBindingList(KeyBinding[] bindings) {
        return new KeyBinding[0];
    }
}