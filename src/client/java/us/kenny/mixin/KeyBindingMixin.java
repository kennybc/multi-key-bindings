package us.kenny.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import us.kenny.MultiKeyBinding;
import us.kenny.MultiKeyBindingManager;

import java.util.List;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {
    @Shadow
    public abstract String getTranslationKey();

    @Shadow
    public abstract InputUtil.Key getDefaultKey();

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void onIsPressed(CallbackInfoReturnable<Boolean> cir) {
        // Check if any bound keys from our manager are pressed
        List<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(this.getTranslationKey());
        for (MultiKeyBinding binding : multiKeyBindings) {
            if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), binding.getKeyCode())) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}