package us.kenny.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import us.kenny.MultiKeyBinding;
import us.kenny.MultiKeyBindingManager;

import java.util.List;

@Mixin(ControlsListWidget.class)
public abstract class ControlsListWidgetMixin {

    /**
     * Injected in the constructor:
     * The injected code will check insert any custom bindings into the list of key bindings in the game settings.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(KeybindsScreen parent, MinecraftClient client, CallbackInfo ci) {
        ControlsListWidget self = (ControlsListWidget) (Object) this;
        for (int i = self.children().size() - 1; i >= 0; i--) {
            ControlsListWidget.Entry entry = self.children().get(i);
            if (entry instanceof ControlsListWidget.KeyBindingEntry) {
                KeyBinding keyBinding = ((KeyBindingEntryAccessor) entry).getBinding();

                // Create and insert instances of KeyBindingEntry for any registered custom bindings
                List<MultiKeyBinding> multiKeyBindings = MultiKeyBindingManager.getKeyBindings(keyBinding.getTranslationKey());
                for (MultiKeyBinding multiKeyBinding : multiKeyBindings) {
                    KeyBinding newKeyBinding = new KeyBinding("multi." + keyBinding.getTranslationKey(), multiKeyBinding.getKeyCode(), multiKeyBinding.getId().toString());
                    ControlsListWidget.KeyBindingEntry newKeyBindingEntry = KeyBindingEntryAccessor.create(self, newKeyBinding, Text.of("     |"));
                    self.children().add(i + 1, newKeyBindingEntry);
                }
            }
        }
    }
}