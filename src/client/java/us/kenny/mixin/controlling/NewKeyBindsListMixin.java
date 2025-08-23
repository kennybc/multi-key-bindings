package us.kenny.mixin.controlling;

import com.blamejared.controlling.client.CustomList;
import com.blamejared.controlling.client.NewKeyBindsList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import us.kenny.MultiKeyBindingClient;
import us.kenny.MultiKeyBindingManager;

import java.lang.reflect.Field;
import java.util.Collection;

@Mixin(value = NewKeyBindsList.class, remap = false)
public abstract class NewKeyBindsListMixin extends CustomList {

    public NewKeyBindsListMixin(KeybindsScreen controls, MinecraftClient mcIn) {
        super(controls, mcIn);
    }

    @WrapOperation(method = "<init>(Lnet/minecraft/client/gui/screen/option/KeybindsScreen;Lnet/minecraft/client/MinecraftClient;)V", at = @At(value = "INVOKE", target = "Lcom/blamejared/controlling/client/NewKeyBindsList;addEntry(Lnet/minecraft/client/gui/screen/option/ControlsListWidget$Entry;)I"), remap = false)
    private int onConstructKeyEntry(NewKeyBindsList newKeyBindsList, ControlsListWidget.Entry entry,
            Operation<Integer> original) {

        int result = original.call(newKeyBindsList, entry);

        if (entry.getClass().getSimpleName().equals("KeyEntry")) {
            try {
                Field keyField = entry.getClass().getDeclaredField("key");
                keyField.setAccessible(true);
                KeyBinding keyBinding = (KeyBinding) keyField.get(entry);

                Collection<KeyBinding> multiKeyBindings = MultiKeyBindingManager
                        .getKeyBindings(keyBinding.getTranslationKey());

                for (KeyBinding multiKeyBinding : multiKeyBindings) {
                    original.call(newKeyBindsList,
                            KeyEntryAccessor.create(newKeyBindsList, multiKeyBinding, Text.of("     |")));
                }
            } catch (Exception e) {
                MultiKeyBindingClient.LOGGER.error(e.getMessage(), e);
            }
        }
        return result;
    }
}