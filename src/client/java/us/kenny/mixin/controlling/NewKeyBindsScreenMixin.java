package us.kenny.mixin.controlling;

import com.blamejared.controlling.client.NewKeyBindsList.KeyEntry;
import com.blamejared.controlling.client.NewKeyBindsScreen;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import us.kenny.core.MultiKeyBindingEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

@Mixin(value = NewKeyBindsScreen.class, remap = false)
public abstract class NewKeyBindsScreenMixin extends KeybindsScreen {

    public NewKeyBindsScreenMixin(Screen screen, GameOptions settings) {
        super(screen, settings);
    }

    /**
     * Controlling sorting does not support our custom key bindings, so we need to:
     * (1)
     * During the filter, include any native key bindings whose associated custom
     * key bindings (bindings to the same action) pass the filter.
     * (2)
     * Sort the native entries, but keep track of the relationships to custom key
     * bindings.
     * (3)
     * Once sorting is complete, add the custom key bindings back in place.
     */
    @WrapOperation(method = "Lcom/blamejared/controlling/client/NewKeyBindsScreen;filterKeys(Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private void onFilterKeysSort(Consumer<List<ControlsListWidget.Entry>> postConsumer,
            Object allEntries,
            Operation<Void> original) {

        // Only call to consumer in this method is with a list of entries
        @SuppressWarnings("unchecked")
        List<ControlsListWidget.Entry> list = ((List<ControlsListWidget.Entry>) allEntries);

        // Separate out any MultiKeyBindingEntry so they don't undergo sorting
        HashMap<String, List<MultiKeyBindingEntry>> multiKeyBindingEntries = new HashMap<>();
        List<ControlsListWidget.Entry> regularEntries = new ArrayList<>();

        for (ControlsListWidget.Entry entry : list) {
            if (entry instanceof MultiKeyBindingEntry multiKeyBindingEntry) {
                multiKeyBindingEntries
                        .computeIfAbsent(multiKeyBindingEntry.getMultiKeyBinding().getAction(), k -> new ArrayList<>())
                        .add(multiKeyBindingEntry);
            } else {
                regularEntries.add(entry);
            }
        }

        // Sort only the regular entries
        original.call(postConsumer, regularEntries);

        // Add any MultiKeyBindingEntry back into the correct place
        list.clear();
        for (ControlsListWidget.Entry entry : regularEntries) {
            list.add(entry);
            if (entry instanceof KeyEntry keyEntry) {
                String multiAction = "multi." + keyEntry.getKey().getTranslationKey();
                list.addAll(multiKeyBindingEntries.getOrDefault(multiAction, Collections.emptyList()));
            }
        }
    }
}