package us.kenny.mixin.controlling;

import com.blamejared.controlling.api.SortOrder;
import com.blamejared.controlling.api.entries.IKeyEntry;
import com.blamejared.controlling.client.CustomList;
import com.blamejared.controlling.client.NewKeyBindsList;
import com.blamejared.controlling.client.NewKeyBindsScreen;
import com.blamejared.searchables.api.SearchableType;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import us.kenny.EditVisibilityMode;
import us.kenny.ProfileManager;
import us.kenny.ToggleManager;
import us.kenny.core.MultiKeyBindingEntry;
import us.kenny.core.MultiKeyBindingScreen;
import us.kenny.core.MultiKeyBindingScreenHelper;
import us.kenny.core.profile.ManageProfilesScreen;
import us.kenny.core.profile.ProfileNameDialog;
import us.kenny.core.profile.ProfilePopupMenu;
import us.kenny.core.controlling.ControllingHideableKeyEntry;
import us.kenny.core.controlling.ControllingMultiKeyBindingEntry;
import us.kenny.mixin.OptionsSubScreenAccessor;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(value = NewKeyBindsScreen.class, remap = false)
public abstract class NewKeyBindsScreenMixin extends KeyBindsScreen  {

    @Shadow
    private SortOrder sortOrder;

    @Shadow
    private Button buttonNone;

    @Shadow
    private Button buttonConflicting;

    @Shadow
    private Button buttonSort;

    @Shadow
    public abstract Button resetButton();

    @Shadow
    public abstract KeyBindsList getKeyBindsList();

    @Shadow
    protected abstract CustomList getCustomList();

    @Unique
    private Button mkbProfileButton;
    @Unique
    private Button mkbEditVisibilityButton;
    @Unique
    private ProfilePopupMenu mkbProfilePopupMenu;
    @Unique
    private static final Identifier VISIBILITY_ICON = Identifier.fromNamespaceAndPath("multi-key-bindings",
            "icon/visibility");
    @Unique
    private static final Identifier VISIBILITY_EXIT_ICON = Identifier.fromNamespaceAndPath("multi-key-bindings",
            "icon/visibility_exit");
    /**
     * See KeyBindsScreenMixin.skipVisibilityReset for the rationale.
     */
    @Unique
    private static boolean skipVisibilityReset = false;

    public NewKeyBindsScreenMixin(Screen screen, Options settings) {
        super(screen, settings);
    }

    /**
     * Reset edit-visibility mode on any fresh entry into Controlling's screen
     * (except mid-toggle swap).
     */
    @Inject(method = "addFooter", at = @At("HEAD"), remap = true)
    private void resetMkbVisibilityOnEnter(CallbackInfo ci) {
        if (skipVisibilityReset) {
            skipVisibilityReset = false;
        } else {
            EditVisibilityMode.setActive(false);
        }
    }

    @Inject(method = "addFooter", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToFooter(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;", remap = true), cancellable = true, remap = true)
    private void customFooter(CallbackInfo ci, @Local Button toggleFreeButton) {
        boolean editing = EditVisibilityMode.isActive();
        Identifier icon = editing ? VISIBILITY_EXIT_ICON : VISIBILITY_ICON;
        Component tooltip = Component.translatable(
                editing ? "multi.visibility.tooltip.exit" : "multi.visibility.tooltip");

        this.mkbEditVisibilityButton = SpriteIconButton.builder(tooltip, b -> toggleMkbEditVisibility(), true)
                .size(20, 20)
                .sprite(icon, 16, 16)
                .build();
        this.mkbProfileButton = Button.builder(mkbDropdownLabel(), b -> openMkbProfileMenu())
                .bounds(0, 0, 80, 20)
                .build();

        Button resetButton = this.resetButton();
        resetButton.setWidth(98);
        Button doneButton = Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .size(98, 20)
                .build();

        LinearLayout container = (LinearLayout)this.layout.addToFooter(LinearLayout.vertical());
        container.spacing(4);

        LinearLayout top = container.addChild(LinearLayout.horizontal());
        top.spacing(4);
        top.addChild(toggleFreeButton);
        top.addChild(this.buttonSort);
        top.addChild(this.buttonNone);
        top.addChild(this.buttonConflicting);

        LinearLayout bottom = container.addChild(LinearLayout.horizontal());
        bottom.spacing(4);
        bottom.addChild(this.mkbEditVisibilityButton);
        bottom.addChild(this.mkbProfileButton);
        bottom.addChild(resetButton);
        bottom.addChild(doneButton);
        ci.cancel();
    }

    @Unique
    private void toggleMkbEditVisibility() {
        EditVisibilityMode.toggle();
        closeMkbProfileMenu();
        skipVisibilityReset = true;
        OptionsSubScreenAccessor accessor = (OptionsSubScreenAccessor) this;
        Minecraft.getInstance().setScreenAndShow(
                new NewKeyBindsScreen(accessor.getLastScreen(), accessor.getOptions()));
    }

    @Unique
    private void openMkbProfileMenu() {
        closeMkbProfileMenu();
        this.mkbProfilePopupMenu = new ProfilePopupMenu(
                this.mkbProfileButton.getX(),
                this.mkbProfileButton.getY() + this.mkbProfileButton.getHeight(),
                this::switchMkbProfile,
                this::openMkbNewProfileDialog,
                this::openMkbManageProfiles);
    }

    @Unique
    private void closeMkbProfileMenu() {
        this.mkbProfilePopupMenu = null;
    }

    @Unique
    private void switchMkbProfile(String name) {
        ProfileManager.load(name);
        closeMkbProfileMenu();
        OptionsSubScreenAccessor accessor = (OptionsSubScreenAccessor) this;
        Minecraft.getInstance().setScreenAndShow(
                new NewKeyBindsScreen(accessor.getLastScreen(), accessor.getOptions()));
    }

    @Unique
    private void openMkbNewProfileDialog() {
        closeMkbProfileMenu();
        Minecraft.getInstance().setScreenAndShow(new ProfileNameDialog(
                this,
                Component.translatable("multi.profile.dialog.new.title"),
                Component.translatable("multi.profile.dialog.create"),
                "",
                name -> {
                    ProfileManager.create(name);
                    Minecraft.getInstance().setScreenAndShow(this);
                }));
    }

    @Unique
    private void openMkbManageProfiles() {
        closeMkbProfileMenu();
        Minecraft.getInstance().setScreenAndShow(new ManageProfilesScreen((KeyBindsScreen) (Object) this));
    }

    @Unique
    private Component mkbDropdownLabel() {
        return Component.literal(ProfileManager.getActiveProfileName() + " ▼");
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"), remap = false)
    private void renderMkbPopup(net.minecraft.client.gui.GuiGraphicsExtractor gfx, int mouseX, int mouseY,
            float partialTicks, CallbackInfo ci) {
        if (mkbProfilePopupMenu != null) {
            mkbProfilePopupMenu.render(gfx, mouseX, mouseY);
        }
    }

    /**
     * @see us.kenny.mixin.KeyBindsScreenMixin#onMouseClicked
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = false)
    public void onMouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (mkbProfilePopupMenu != null) {
            boolean consumed = mkbProfilePopupMenu.mouseClicked(
                    mouseButtonEvent.x(), mouseButtonEvent.y(), mouseButtonEvent.button());
            if (!consumed && !mkbProfilePopupMenu.containsPoint(mouseButtonEvent.x(), mouseButtonEvent.y())) {
                closeMkbProfileMenu();
            }
            cir.setReturnValue(true);
            return;
        }
        if (MultiKeyBindingScreenHelper.handleMouseClicked((MultiKeyBindingScreen) this, this.getKeyBindsList(),
                mouseButtonEvent)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * @see us.kenny.mixin.KeyBindsScreenMixin#onKeyPressed
     */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = false)
    public void onKeyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (MultiKeyBindingScreenHelper.handleKeyPressed((MultiKeyBindingScreen) this, this.getKeyBindsList(),
                keyEvent)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Our custom key bindings cannot be sorted, so we must first remove them before
     * sorting. After sorting, we add them back in place.
     */
    @WrapOperation(method = "filterKeys(Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private void onFilterKeysSort(Consumer<List<NewKeyBindsList.KeyEntry>> instance, Object object,
            Operation<Void> original) {
        @SuppressWarnings("unchecked")
        List<NewKeyBindsList.Entry> entries = ((List<NewKeyBindsList.Entry>) object);
        CustomList list = this.getCustomList();

        // Separate out any MultiKeyBindingEntry so they don't undergo sorting.
        // Toggle primaries + subs form a fixed section at the end of the list,
        // kept in original order. The category header is treated like any other
        // CategoryEntry — vanilla and ours both get dropped in sort mode.
        HashMap<String, List<MultiKeyBindingEntry>> multiKeyBindingEntries = new HashMap<>();
        List<NewKeyBindsList.Entry> toggleSection = new ArrayList<>();

        for (NewKeyBindsList.Entry entry : entries) {
            if (entry instanceof MultiKeyBindingEntry multiKeyBindingEntry) {
                if (ToggleManager.isToggleAction(multiKeyBindingEntry.getMultiKeyBinding().getAction())) {
                    toggleSection.add(entry);
                } else {
                    multiKeyBindingEntries
                            .computeIfAbsent(multiKeyBindingEntry.getMultiKeyBinding().getAction(),
                                    k -> new ArrayList<>())
                            .add(multiKeyBindingEntry);
                }
            }
        }

        // Sort only the regular entries
        entries.removeIf((entry) -> (entry instanceof MultiKeyBindingEntry)
                || !(entry instanceof IKeyEntry));
        list.sort(this.sortOrder);
        List<NewKeyBindsList.Entry> sortedEntries = new ArrayList<>(list.children());

        // Clear and rebuild children with custom bindings in correct place
        list.clearEntries();
        for (NewKeyBindsList.Entry entry : sortedEntries) {
            if (entry instanceof NewKeyBindsList.KeyEntry keyEntry) {
                String multiAction = "multi." + keyEntry.getKey().getName();
                list.addEntryInternal(entry);
                multiKeyBindingEntries.getOrDefault(multiAction, List.of()).forEach(list::addEntryInternal);
            }
        }
        // Append the toggles section in its original order.
        toggleSection.forEach(list::addEntryInternal);
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

        return pruneEmptyCategories(rebuilt);
    }

    /**
     * Drop any CategoryEntry immediately followed by another CategoryEntry
     * or the end of the list — meaning every entry it grouped was filtered
     * out.
     */
    @Unique
    private List<KeyBindsList.Entry> pruneEmptyCategories(List<KeyBindsList.Entry> entries) {
        List<KeyBindsList.Entry> kept = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            KeyBindsList.Entry entry = entries.get(i);
            if (entry instanceof KeyBindsList.CategoryEntry) {
                boolean hasContent = i + 1 < entries.size()
                        && !(entries.get(i + 1) instanceof KeyBindsList.CategoryEntry);
                if (!hasContent) {
                    continue;
                }
            }
            kept.add(entry);
        }
        return kept;
    }
}