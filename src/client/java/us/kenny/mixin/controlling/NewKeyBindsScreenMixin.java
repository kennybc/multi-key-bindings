package us.kenny.mixin.controlling;

import com.blamejared.controlling.api.SortOrder;
import com.blamejared.controlling.client.CustomList;
import com.blamejared.controlling.client.NewKeyBindsScreen;
import com.blamejared.controlling.client.NewKeyBindsList.KeyEntry;
import com.blamejared.searchables.api.SearchableType;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import us.kenny.core.MultiKeyBindingEntry;
import us.kenny.core.controlling.ControllingHideableKeyEntry;
import us.kenny.core.controlling.ControllingMultiKeyBindingEntry;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(value = NewKeyBindsScreen.class, remap = false)
public abstract class NewKeyBindsScreenMixin extends KeyBindsScreen {
    @Shadow
    private SortOrder sortOrder;
    @Shadow
    public abstract KeyBindsList getKeyBindsList();
    @Shadow
    protected abstract CustomList getCustomList();

    public NewKeyBindsScreenMixin(Screen screen, Options settings) {
        super(screen, settings);
    }

    /**
     * Our custom key bindings cannot be sorted, so we must first remove them before sorting. After sorting, we add them back in place.
     */
    @WrapOperation(method = "Lcom/blamejared/controlling/client/NewKeyBindsScreen;filterKeys(Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private void onFilterKeysSort(Consumer<List<KeyBindsList.Entry>> postConsumer, Object allEntries, Operation<Void> original) {
        // Only call to consumer in this method is with a list of entries
        @SuppressWarnings("unchecked")
        List<KeyBindsList.Entry> list = ((List<KeyBindsList.Entry>) allEntries);

        // Separate out any MultiKeyBindingEntry so they don't undergo sorting
        HashMap<String, List<MultiKeyBindingEntry>> multiKeyBindingEntries = new HashMap<>();
        List<KeyBindsList.Entry> regularEntries = new ArrayList<>();

        for (KeyBindsList.Entry entry : list) {
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

        // Clear and rebuild children with custom bindings in correct place
        list.clear();
        for (KeyBindsList.Entry entry : regularEntries) {
            list.add(entry);
            if (entry instanceof KeyEntry keyEntry) {
                String multiAction = "multi." + keyEntry.getKey().getName();
                list.addAll(multiKeyBindingEntries.getOrDefault(multiAction, Collections.emptyList()));
            }
        }
    }

    /**
     * If a child custom binding passes the filter predicate but its parent does not, retroactively add back the parent, but set it to be read-only.
     */
    @WrapOperation(method = "filterKeys(Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lcom/blamejared/searchables/api/SearchableType;filterEntries(Ljava/util/List;Ljava/lang/String;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private List<KeyBindsList.Entry> onFilterKeysFilter(SearchableType<KeyBindsList.Entry> instance, List<KeyBindsList.Entry> entries, String search, Predicate<KeyBindsList.Entry> predicate, Operation<List<KeyBindsList.Entry>> original) {
        List<KeyBindsList.Entry> filtered = original.call(instance, entries, search, predicate);

        // Build a set of all parents that have children in the filtered list
        Set<KeyBindsList.Entry> parentsToReinsert = new HashSet<>();
        for (KeyBindsList.Entry entry : filtered) {
            if (entry instanceof ControllingMultiKeyBindingEntry child) {
                parentsToReinsert.add(child.getParentEntry());
            }
        }
        Set<KeyBindsList.Entry> parentsInFiltered = new HashSet<>(filtered);

        List<KeyBindsList.Entry> rebuilt = new ArrayList<>();
        Set<KeyBindsList.Entry> insertedParents = new HashSet<>();

        for (KeyBindsList.Entry entry : filtered) {
            if (entry instanceof ControllingMultiKeyBindingEntry child) {
                KeyBindsList.Entry parent = child.getParentEntry();

                // Only set hidden if the parent didn't match the filter itself
                if (parent instanceof ControllingHideableKeyEntry hideableKeyEntry && !parentsInFiltered.contains(parent)) {
                    hideableKeyEntry.setHidden(true);
                }

                if (!insertedParents.contains(parent)) {
                    rebuilt.add(parent);
                    insertedParents.add(parent);
                }
            }
            // Skip parents that will be re-added when processing their children
            if (!parentsToReinsert.contains(entry)) {
                rebuilt.add(entry);
            }
        }

        return rebuilt;
    }
}