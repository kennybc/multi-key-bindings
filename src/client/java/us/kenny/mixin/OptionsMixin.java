package us.kenny.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import us.kenny.ConfigManager;
import us.kenny.MultiKeyBindingManager;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

@Mixin(Options.class)
public class OptionsMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void afterOptionsInit(Minecraft client, File file, CallbackInfo ci) {
        MultiKeyBindingManager.setGameOptions((Options) (Object) this);
        ConfigManager.loadConfigFile();
    }
}
