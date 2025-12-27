package us.kenny.mixin.controlling;

import com.blamejared.controlling.api.SortOrder;
import com.blamejared.controlling.api.entries.IKeyEntry;
import com.blamejared.controlling.client.CustomList;
import com.blamejared.controlling.client.NewKeyBindsList;
import com.blamejared.controlling.client.NewKeyBindsScreen;
import com.blamejared.searchables.api.SearchableType;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.KeyMapping;
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
     * Controlling sorting does not support our custom key bindings, so we need to:
     * (1)
     * During the filter, include any native key bindings whose associated custom
     * key bindings (bindings to the same action) pass the filter.
     * (2)
     * Sort the native entries, but keep track of the relationships to custom key
     * bindings.
     * (3)
     * Once sorting is complete, add the custom key bindings back in place.
     *
     * @return
     */
    @WrapOperation(method = "filterKeys(Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private void onFilterKeysSort(Consumer<List<NewKeyBindsList.KeyEntry>> instance, Object object, Operation<Void> original) {
        @SuppressWarnings("unchecked")
        List<NewKeyBindsList.Entry> entries = ((List<NewKeyBindsList.Entry>) object);
        CustomList list = this.getCustomList();

        // Separate out any MultiKeyBindingEntry so they don't undergo sorting
        HashMap<String, List<MultiKeyBindingEntry>> multiKeyBindingEntries = new HashMap<>();

        for (NewKeyBindsList.Entry entry : entries) {
            if (entry instanceof MultiKeyBindingEntry multiKeyBindingEntry) {
                multiKeyBindingEntries
                        .computeIfAbsent(multiKeyBindingEntry.getMultiKeyBinding().getAction(), k -> new ArrayList<>())
                        .add(multiKeyBindingEntry);
            }
        }

        // Sort only the regular entries
        entries.removeIf((entry) -> (entry instanceof MultiKeyBindingEntry) || !(entry instanceof IKeyEntry));
        list.sort(this.sortOrder);
        List<NewKeyBindsList.Entry> sortedEntries = new ArrayList<>(list.children());

        // Clear and rebuild children (display list) with MultiKeyBindingEntry in correct place
        // DO NOT clear allEntries - it's the persistent unfiltered list
        list.clearEntries();
        for (NewKeyBindsList.Entry entry : sortedEntries) {
            if (entry instanceof NewKeyBindsList.KeyEntry keyEntry) {
                String multiAction = "multi." + keyEntry.getKey().getName();
                list.addEntryInternal(entry);
                multiKeyBindingEntries.getOrDefault(multiAction, Collections.emptyList()).forEach(list::addEntryInternal);
            }
        }
    }

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

        List<KeyBindsList.Entry> rebuilt = new ArrayList<>();
        Set<KeyBindsList.Entry> insertedParents = new HashSet<>();

        for (KeyBindsList.Entry entry : filtered) {
            if (entry instanceof ControllingMultiKeyBindingEntry child) {
                KeyBindsList.Entry parent = child.getParentEntry();

                if (parent instanceof ControllingHideableKeyEntry hideableKeyEntry) {
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