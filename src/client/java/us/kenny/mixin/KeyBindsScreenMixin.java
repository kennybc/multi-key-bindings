package us.kenny.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import us.kenny.EditVisibilityMode;
import us.kenny.HiddenBindingManager;
import us.kenny.ProfileManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingScreen;
import us.kenny.core.MultiKeyBindingScreenHelper;
import us.kenny.core.profile.ManageProfilesScreen;
import us.kenny.core.profile.ProfileNameDialog;
import us.kenny.core.profile.ProfilePopupMenu;

@Mixin(KeyBindsScreen.class)
public abstract class KeyBindsScreenMixin extends Screen implements MultiKeyBindingScreen {
    @Shadow
    public long lastKeySelection;
    @Shadow
    private KeyBindsList keyBindsList;
    @Mutable
    @Shadow
    private KeyMapping selectedKey;
    @Shadow
    private Button resetButton;

    @Unique
    private static final int FOOTER_BUTTON_WIDTH = 80;
    @Unique
    private static final Identifier VISIBILITY_ICON = Identifier.fromNamespaceAndPath("multi-key-bindings",
            "icon/visibility");
    @Unique
    private static final Identifier VISIBILITY_EXIT_ICON = Identifier.fromNamespaceAndPath("multi-key-bindings",
            "icon/visibility_exit");
    /**
     * Set to true right before a mid-toggle screen swap so the incoming
     * init doesn't reset EditVisibilityMode. Any other entry into the
     * screen (options -> controls, back from manage profiles, etc.)
     * resets the mode.
     */
    @Unique
    private static boolean skipVisibilityReset = false;

    @Unique
    private MultiKeyBinding selectedMultiKeyBinding;

    @Unique
    private Button profileDropdownButton;
    @Unique
    private Button editVisibilityButton;
    @Unique
    private ProfilePopupMenu profilePopupMenu;

    protected KeyBindsScreenMixin(Component title) {
        super(title);
    }

    @Unique
    public KeyMapping getSelectedKey() {
        return this.selectedKey;
    }

    @Unique
    public MultiKeyBinding getSelectedMultiKeyBinding() {
        return this.selectedMultiKeyBinding;
    }

    @Unique
    public void setSelectedKey(KeyMapping keyMapping) {
        this.selectedKey = keyMapping;
    }

    @Unique
    public void setSelectedMultiKeyBinding(MultiKeyBinding multiKeyBinding) {
        this.selectedMultiKeyBinding = multiKeyBinding;
    }

    @Unique
    public void setLastKeySelection(long time) {
        this.lastKeySelection = time;
    }

    /**
     * Prepend the profile dropdown to the vanilla footer LinearLayout and
     * shrink Reset Keys so all three buttons share the row at equal width.
     * Also resets edit-visibility mode on any fresh entry into the screen
     * (a mid-toggle screen swap sets skipVisibilityReset first so the mode
     * survives that specific transition).
     */
    @Inject(method = "addFooter", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;", ordinal = 0))
    private void addProfileButtonToFooter(CallbackInfo ci, @Local LinearLayout footerRow) {
        if (skipVisibilityReset) {
            skipVisibilityReset = false;
        } else {
            EditVisibilityMode.setActive(false);
        }

        this.resetButton.setWidth(FOOTER_BUTTON_WIDTH);

        // Compact icon button on the far left. Icon + tooltip flip when
        // edit-visibility mode is active.
        boolean editing = EditVisibilityMode.isActive();
        Identifier icon = editing ? VISIBILITY_EXIT_ICON : VISIBILITY_ICON;
        Component tooltip = Component.translatable(
                editing ? "multi.visibility.tooltip.exit" : "multi.visibility.tooltip");
        this.editVisibilityButton = SpriteIconButton.builder(
                tooltip,
                b -> toggleEditVisibility(),
                true)
                .size(20, 20)
                .sprite(icon, 16, 16)
                .build();
        footerRow.addChild(this.editVisibilityButton);

        this.profileDropdownButton = Button.builder(dropdownLabel(), b -> openProfileMenu())
                .bounds(0, 0, FOOTER_BUTTON_WIDTH, 20)
                .build();
        footerRow.addChild(this.profileDropdownButton);
    }

    /**
     * Shrink the Done button to match the other two before it enters the
     * footer row.
     */
    @WrapOperation(method = "addFooter", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;", ordinal = 1))
    private LayoutElement shrinkDone(LinearLayout row, LayoutElement doneElement, Operation<LayoutElement> original) {
        if (doneElement instanceof Button b) {
            b.setWidth(FOOTER_BUTTON_WIDTH);
        }
        return original.call(row, doneElement);
    }

    /**
     * Render the popup menu (if open) and the Edit Visibility banner or
     * hidden-count row.
     */
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        renderVisibilityStatus(gfx);
        if (profilePopupMenu != null) {
            profilePopupMenu.render(gfx, mouseX, mouseY);
        }
    }

    @Unique
    private void renderVisibilityStatus(GuiGraphicsExtractor gfx) {
        int listTop = this.keyBindsList.getY();
        int textY = listTop - 12;
        var font = Minecraft.getInstance().font;

        if (EditVisibilityMode.isActive()) {
            Component banner = Component.translatable("multi.visibility.banner");
            gfx.text(font, banner,
                    this.width / 2 - font.width(banner) / 2, textY, 0xFFFFC857);
        } else {
            int hiddenCount = HiddenBindingManager.size();
            if (hiddenCount > 0) {
                Component msg = Component.translatable("multi.visibility.hidden_count", hiddenCount);
                gfx.text(font, msg,
                        this.width / 2 - font.width(msg) / 2, textY, 0xFF999999);
            }
        }
    }

    @Unique
    private void toggleEditVisibility() {
        EditVisibilityMode.toggle();
        closeProfileMenu();
        // Full screen swap so the list rebuilds against the new mode
        // (previously-hidden entries reappear, or vice versa). Suppress
        // the auto-reset on init since we WANT the mode to persist across
        // this swap.
        skipVisibilityReset = true;
        OptionsSubScreenAccessor accessor = (OptionsSubScreenAccessor) this;
        Minecraft.getInstance().setScreenAndShow(
                new KeyBindsScreen(accessor.getLastScreen(), accessor.getOptions()));
    }

    /**
     * Injected in the method mouseClicked:
     * Updates selected custom key binding with whatever mouse button was pressed.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void onMouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (profilePopupMenu != null) {
            boolean consumed = profilePopupMenu.mouseClicked(
                    mouseButtonEvent.x(), mouseButtonEvent.y(), mouseButtonEvent.button());
            if (!consumed && !profilePopupMenu.containsPoint(mouseButtonEvent.x(), mouseButtonEvent.y())) {
                closeProfileMenu();
            }
            cir.setReturnValue(true);
            return;
        }
        if (MultiKeyBindingScreenHelper.handleMouseClicked(this, this.keyBindsList, mouseButtonEvent)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Injected in the method keyPressed:
     * Updates selected custom key binding with whatever key was pressed.
     */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void onKeyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (MultiKeyBindingScreenHelper.handleKeyPressed(this, this.keyBindsList, keyEvent)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void openProfileMenu() {
        closeProfileMenu();
        this.profilePopupMenu = new ProfilePopupMenu(
                this.profileDropdownButton.getX(),
                this.profileDropdownButton.getY(),
                this::switchProfile,
                this::openNewProfileDialog,
                this::openManageProfiles);
    }

    @Unique
    private void closeProfileMenu() {
        this.profilePopupMenu = null;
    }

    @Unique
    private void switchProfile(String name) {
        ProfileManager.load(name);
        closeProfileMenu();
        // Full screen swap: rebuildWidgets doesn't reset the layout tree,
        // so the old KeyBindsList would linger under the new one.
        OptionsSubScreenAccessor accessor = (OptionsSubScreenAccessor) this;
        Minecraft.getInstance().setScreenAndShow(
                new KeyBindsScreen(accessor.getLastScreen(), accessor.getOptions()));
    }

    @Unique
    private void openNewProfileDialog() {
        closeProfileMenu();
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
    private void openManageProfiles() {
        closeProfileMenu();
        Minecraft.getInstance().setScreenAndShow(new ManageProfilesScreen((KeyBindsScreen) (Object) this));
    }

    @Unique
    private static Component dropdownLabel() {
        return Component.literal(ProfileManager.getActiveProfileName() + " ▼");
    }
}
