package us.kenny.mixin.controlling;

import com.blamejared.controlling.api.SortOrder;
import com.blamejared.controlling.api.entries.IKeyEntry;
import com.blamejared.controlling.client.CustomList;
import com.blamejared.controlling.client.NewKeyBindsList;
import com.blamejared.controlling.client.NewKeyBindsScreen;
import com.blamejared.controlling.client.NewKeyBindsList.KeyEntry;
import com.blamejared.searchables.api.SearchableType;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;

import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import us.kenny.StickyToggleManager;
import us.kenny.core.MultiKeyBindingEntry;
import us.kenny.core.MultiKeyBindingScreen;
import us.kenny.core.MultiKeyBindingScreenHelper;
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
     * @see us.kenny.mixin.KeyBindsScreenMixin#onMouseClicked
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = false)
    public void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (MultiKeyBindingScreenHelper.handleMouseClicked((MultiKeyBindingScreen) this, this.getKeyBindsList(),
                button)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * @see us.kenny.mixin.KeyBindsScreenMixin#onKeyPressed
     */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = false)
    public void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (MultiKeyBindingScreenHelper.handleKeyPressed((MultiKeyBindingScreen) this, this.getKeyBindsList(),
                InputConstants.getKey(keyCode, scanCode))) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Our custom key bindings cannot be sorted, so we must first remove them before
     * sorting. After sorting, we add them back in place.
     */
    @WrapOperation(method = "Lcom/blamejared/controlling/client/NewKeyBindsScreen;filterKeys(Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private void onFilterKeysSort(Consumer<List<KeyBindsList.Entry>> postConsumer, Object object,
            Operation<Void> original) {
        // Only call to consumer in this method is with a list of entries
        @SuppressWarnings("unchecked")
        List<NewKeyBindsList.Entry> entries = ((List<NewKeyBindsList.Entry>) object);
        CustomList list = this.getCustomList();

        // Separate out any MultiKeyBindingEntry so they don't undergo sorting.
        // Toggle primaries + subs form a fixed section at the end of the list,
        // kept in original order. The category header is treated like any other
        // CategoryEntry — vanilla and ours both get dropped in sort mode.
        HashMap<String, List<MultiKeyBindingEntry>> multiKeyBindingEntries = new HashMap<>();
        List<KeyBindsList.Entry> regularEntries = new ArrayList<>();
        List<NewKeyBindsList.Entry> toggleSection = new ArrayList<>();

        for (NewKeyBindsList.Entry entry : entries) {
            if (entry instanceof MultiKeyBindingEntry multiKeyBindingEntry) {
                if (StickyToggleManager.isToggleAction(multiKeyBindingEntry.getMultiKeyBinding().getAction())) {
                    toggleSection.add(entry);
                } else {
                    multiKeyBindingEntries
                            .computeIfAbsent(multiKeyBindingEntry.getMultiKeyBinding().getAction(),
                                    k -> new ArrayList<>())
                            .add(multiKeyBindingEntry);
                }
            } else {
                regularEntries.add(entry);
            }
        }

        // Sort only the regular entries
        original.call(postConsumer, regularEntries);

        // Clear and rebuild children with custom bindings in correct place
        entries.clear();
        for (KeyBindsList.Entry entry : regularEntries) {
            entries.add(entry);
            if (entry instanceof KeyEntry keyEntry) {
                String multiAction = "multi." + keyEntry.getKey().getName();
                entries.addAll(multiKeyBindingEntries.getOrDefault(multiAction, List.of()));
            }
        }
        // Append the sticky-toggle section in its original order.
        toggleSection.forEach(entries::add);
    }

    /**
     * If a child custom binding passes the filter predicate but its parent does
     * not, retroactively add back the parent, but set it to be read-only.
     */
    @WrapOperation(method = "filterKeys(Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lcom/blamejared/searchables/api/SearchableType;filterEntries(Ljava/util/List;Ljava/lang/String;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private List<KeyBindsList.Entry> onFilterKeysFilter(SearchableType<KeyBindsList.Entry> instance,
            List<KeyBindsList.Entry> entries, String search, Predicate<KeyBindsList.Entry> predicate,
            Operation<List<KeyBindsList.Entry>> original) {
        List<KeyBindsList.Entry> filtered = original.call(instance, entries, search, predicate);

        // Build a set of all parents that have children in the filtered list.
        // Toggle primaries have parentEntry == null (top-level); skip those.
        Set<KeyBindsList.Entry> parentsToReinsert = new HashSet<>();
        for (KeyBindsList.Entry entry : filtered) {
            if (entry instanceof ControllingMultiKeyBindingEntry child && child.getParentEntry() != null) {
                parentsToReinsert.add(child.getParentEntry());
            }
        }
        Set<KeyBindsList.Entry> parentsInFiltered = new HashSet<>(filtered);

        List<KeyBindsList.Entry> rebuilt = new ArrayList<>();
        Set<KeyBindsList.Entry> insertedParents = new HashSet<>();

        for (KeyBindsList.Entry entry : filtered) {
            if (entry instanceof ControllingMultiKeyBindingEntry child) {
                KeyBindsList.Entry parent = child.getParentEntry();

                if (parent != null) {
                    // Only set hidden if the parent didn't match the filter itself
                    if (parent instanceof ControllingHideableKeyEntry hideableKeyEntry
                            && !parentsInFiltered.contains(parent)) {
                        hideableKeyEntry.setHidden(true);
                    }

                    if (!insertedParents.contains(parent)) {
                        rebuilt.add(parent);
                        insertedParents.add(parent);
                    }
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