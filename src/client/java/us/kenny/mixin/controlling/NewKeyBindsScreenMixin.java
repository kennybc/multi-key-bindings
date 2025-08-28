package us.kenny.mixin.controlling;

import com.blamejared.controlling.api.entries.IKeyEntry;
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

    /*
     * @WrapOperation(method =
     * "Lcom/blamejared/controlling/client/NewKeyBindsScreen;filterKeys(Ljava/lang/String;)V",
     * at = @At(value = "INVOKE", target =
     * "Lcom/blamejared/searchables/api/SearchableType;filterEntries(Ljava/util/List;Ljava/lang/String;Ljava/util/function/Predicate;)Ljava/util/List;"
     * ), remap = false)
     * private List<ControlsListWidget.Entry> onGetAllEntries(
     * SearchableType<ControlsListWidget.Entry> instance,
     * List<ControlsListWidget.Entry> entries,
     * String lastSearch,
     * Predicate<ControlsListWidget.Entry> extraPredicate,
     * Operation<List<ControlsListWidget.Entry>> original) {
     * // After filtering is complete, update all MultiKeyBindingEntry parent
     * // references
     * NewKeyBindsScreen self = (NewKeyBindsScreen) (Object) this;
     * ControlsListWidget keyBindsList = self.getKeyBindsList();
     * 
     * List<ControlsListWidget.Entry> results = original.call(instance, entries,
     * lastSearch, extraPredicate);
     * 
     * for (ControlsListWidget.Entry entry : results) {
     * MultiKeyBindingClient.LOGGER.info("entry found");
     * if (entry instanceof MultiKeyBindingEntry multiKeyEntry) {
     * multiKeyEntry.setParentList(keyBindsList);
     * keyBindsList.children().add(multiKeyEntry);
     * }
     * }
     * 
     * return results;
     * }
     */

    @WrapOperation(method = "Lcom/blamejared/controlling/client/NewKeyBindsScreen;filterKeys(Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"), remap = false)
    private void interceptSort(Consumer<List<ControlsListWidget.Entry>> postConsumer,
            Object entries,
            Operation<Void> original) {

        // Only call to consumer in this method is with a list of entries
        @SuppressWarnings("unchecked")
        List<ControlsListWidget.Entry> list = ((List<ControlsListWidget.Entry>) entries);

        // Separate out any MultiKeyBindingEntry so they don't undergo sorting
        HashMap<String, List<ControlsListWidget.Entry>> multiKeyBindingEntries = new HashMap<>();
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
            if (entry instanceof IKeyEntry keyEntry)
                list.addAll(multiKeyBindingEntries.getOrDefault("multi." + keyEntry.getKey().getTranslationKey(),
                        Collections.emptyList()));
        }
    }
}