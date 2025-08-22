package us.kenny.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import us.kenny.MultiKeyBindingManager;

import java.util.Collection;

@Mixin(ControlsListWidget.class)
public abstract class ControlsListWidgetMixin {

    /**
     * Injected in the constructor:
     * The injected code will insert any custom bindings into the list of key
     * bindings in the game settings.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(KeybindsScreen parent, MinecraftClient client, CallbackInfo ci) {
        ControlsListWidget self = (ControlsListWidget) (Object) this;
        for (int i = self.children().size() - 1; i >= 0; i--) {
            ControlsListWidget.Entry entry = self.children().get(i);

            if (!(entry instanceof ControlsListWidget.KeyBindingEntry))
                continue;
            KeyBinding keyBinding = ((KeyBindingEntryAccessor) entry).getBinding();

            // Create and insert instances of KeyBindingEntry for any custom bindings
            Collection<KeyBinding> keyBindings = MultiKeyBindingManager.getKeyBindings(keyBinding.getTranslationKey());
            for (KeyBinding customKeyBinding : keyBindings) {
                ControlsListWidget.KeyBindingEntry newKeyBindingEntry = KeyBindingEntryAccessor.create(self,
                        customKeyBinding, Text.of("     |"));
                self.children().add(i + 1, newKeyBindingEntry);
            }

        }
    }
}