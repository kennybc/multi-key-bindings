package us.kenny.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import us.kenny.ConfigManager;
import us.kenny.MultiKeyBindingManager;

import java.io.File;

@Mixin(GameOptions.class)
public class GameOptionsMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void afterOptionsInit(MinecraftClient client, File file, CallbackInfo ci) {
        MultiKeyBindingManager.setGameOptions((GameOptions) (Object) this);
        ConfigManager.loadConfigFile();
    }
}
