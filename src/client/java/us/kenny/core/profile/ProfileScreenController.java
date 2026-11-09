package us.kenny.core.profile;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import us.kenny.HiddenBindingManager;
import us.kenny.ProfileManager;

public final class ProfileScreenController {
    private static final ResourceLocation VISIBILITY_ICON = ResourceLocation.fromNamespaceAndPath(
            "multi-key-bindings", "icon/visibility");
    private static final ResourceLocation VISIBILITY_EXIT_ICON = ResourceLocation.fromNamespaceAndPath(
            "multi-key-bindings", "icon/visibility_exit");
    /**
     * Set to true right before a mid-toggle screen swap so the incoming
     * screen's init doesn't reset edit-visibility mode. Any other entry
     * (options -> controls, back from manage profiles, etc.) resets.
     */
    private static boolean skipVisibilityResetOnNextEnter = false;

    private final Screen owner;
    private final Supplier<KeyBindsScreen> screenRebuilder;
    private final boolean anchorPopupAtButtonBottom;

    private Button visibilityButton;
    private Button dropdownButton;
    private ProfilePopupMenu popup;

    /**
     * Create a controller bound to a specific Key Binds screen instance.
     *
     * @param owner                     The already-initialized screen the
     *                                  controller is attached to. Used as the
     *                                  parent (backdrop + cancel target) for
     *                                  overlay dialogs, which render their
     *                                  parent's render — passing an
     *                                  uninitialized screen there would NPE.
     * @param screenRebuilder           Produces a fresh instance of the owner
     *                                  screen. Used for every post-profile-
     *                                  change navigation so the new screen
     *                                  runs a fresh init against the current
     *                                  profile state.
     * @param anchorPopupAtButtonBottom If true, anchor the popup's bottom edge
     *                                  at the dropdown button's bottom;
     *                                  otherwise at its top. Vanilla anchors at
     *                                  top (popup floats above the button);
     *                                  Controlling anchors at bottom.
     */
    public ProfileScreenController(Screen owner, Supplier<KeyBindsScreen> screenRebuilder,
            boolean anchorPopupAtButtonBottom) {
        this.owner = owner;
        this.screenRebuilder = screenRebuilder;
        this.anchorPopupAtButtonBottom = anchorPopupAtButtonBottom;
    }

    /**
     * Reset edit-visibility mode on a fresh entry into the screen. Suppressed
     * for one call after a mid-toggle screen swap so the mode persists across
     * that specific transition.
     */
    public void resetEditModeOnEnter() {
        if (skipVisibilityResetOnNextEnter) {
            skipVisibilityResetOnNextEnter = false;
        } else {
            HiddenBindingManager.setEditMode(false);
        }
    }

    /**
     * Build the sprite-icon button that toggles edit-visibility mode.
     */
    public Button buildVisibilityButton() {
        boolean editing = HiddenBindingManager.isEditMode();
        ResourceLocation icon = editing ? VISIBILITY_EXIT_ICON : VISIBILITY_ICON;
        Component tooltip = Component.translatable(
                editing ? "multi.visibility.tooltip.exit" : "multi.visibility.tooltip");
        this.visibilityButton = SpriteIconButton.builder(tooltip, b -> toggleEditMode(), true)
                .size(20, 20)
                .sprite(icon, 16, 16)
                .build();
        return this.visibilityButton;
    }

    /**
     * Build the profile-dropdown button that opens the popup menu.
     *
     * @param width The button's width, in pixels.
     */
    public Button buildDropdownButton(int width) {
        this.dropdownButton = Button.builder(dropdownLabel(width), b -> openMenu())
                .bounds(0, 0, width, 20)
                .build();
        return this.dropdownButton;
    }

    /**
     * Route a mouse click through the popup menu when it is open. Returns
     * true when the click was consumed (popup was open) so the caller can
     * short-circuit its usual mouse handling.
     *
     * @param mouseX Screen-space X.
     * @param mouseY Screen-space Y.
     * @param button The mouse button.
     */
    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (popup == null) {
            return false;
        }
        boolean consumed = popup.mouseClicked(mouseX, mouseY, button);
        if (!consumed && !popup.containsPoint(mouseX, mouseY)) {
            popup = null;
        }
        return true;
    }

    /**
     * Route a mouse-wheel event to the popup when it is open. Returns true
     * (consumed) whenever the popup is open so the list underneath doesn't
     * scroll while the menu is showing.
     *
     * @param mouseX  Screen-space X.
     * @param mouseY  Screen-space Y.
     * @param scrollY Vertical wheel delta.
     */
    public boolean handleMouseScroll(double mouseX, double mouseY, double scrollY) {
        if (popup == null) {
            return false;
        }
        popup.mouseScrolled(mouseX, mouseY, scrollY);
        return true;
    }

    /**
     * Render the popup menu if it is open.
     *
     * @param gfx    The graphics context.
     * @param mouseX The mouse X coordinate.
     * @param mouseY The mouse Y coordinate.
     */
    public void renderPopup(GuiGraphics gfx, int mouseX, int mouseY) {
        if (popup != null) {
            // Draw above the screen's widgets and text.
            gfx.flush();
            gfx.pose().pushPose();
            gfx.pose().translate(0.0F, 0.0F, 400.0F);
            popup.render(gfx, mouseX, mouseY);
            gfx.pose().popPose();
        }
    }

    private void toggleEditMode() {
        HiddenBindingManager.toggleEditMode();
        popup = null;
        skipVisibilityResetOnNextEnter = true;
        Minecraft.getInstance().setScreen(screenRebuilder.get());
    }

    private void openMenu() {
        int anchorY = anchorPopupAtButtonBottom
                ? dropdownButton.getY() + dropdownButton.getHeight()
                : dropdownButton.getY();
        popup = new ProfilePopupMenu(
                dropdownButton.getX(),
                anchorY,
                this::switchProfile,
                this::openNewProfileDialog,
                this::openManageProfiles);
    }

    private void switchProfile(String name) {
        ProfileManager.load(name);
        popup = null;
        Minecraft.getInstance().setScreen(screenRebuilder.get());
    }

    private void openNewProfileDialog() {
        popup = null;
        Minecraft.getInstance().setScreen(new ProfileNameDialog(
                owner,
                Component.translatable("multi.profile.dialog.new.title"),
                Component.translatable("multi.profile.dialog.create"),
                "",
                ProfileNameDialog.takenForCreate(),
                name -> {
                    ProfileManager.create(name);
                    Minecraft.getInstance().setScreen(screenRebuilder.get());
                }));
    }

    private void openManageProfiles() {
        popup = null;
        Minecraft.getInstance().setScreen(new ManageProfilesScreen(screenRebuilder));
    }

    /**
     * The dropdown label: the active profile name, shortened with "..." so
     * the name plus the arrow fits inside the button.
     *
     * @param buttonWidth The dropdown button's width, in pixels.
     */
    private static Component dropdownLabel(int buttonWidth) {
        var font = Minecraft.getInstance().font;
        String arrow = " \u25BC";
        int available = buttonWidth - 8 - font.width(arrow);
        String name = ProfilePopupMenu.ellipsize(font, ProfileManager.getActiveProfileName(), available);
        return Component.literal(name + arrow);
    }
}
