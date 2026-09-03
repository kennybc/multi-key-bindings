package us.kenny.core.profile;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import us.kenny.HiddenBindingManager;
import us.kenny.ProfileManager;

public final class ProfileScreenController {
    private static final Identifier VISIBILITY_ICON = Identifier.fromNamespaceAndPath(
            "multi-key-bindings", "icon/visibility");
    private static final Identifier VISIBILITY_EXIT_ICON = Identifier.fromNamespaceAndPath(
            "multi-key-bindings", "icon/visibility_exit");
    /**
     * Set to true right before a mid-toggle screen swap so the incoming
     * screen's init doesn't reset edit-visibility mode. Any other entry
     * (options -> controls, back from manage profiles, etc.) resets.
     */
    private static boolean skipVisibilityResetOnNextEnter = false;

    private final Screen owner;
    private final Supplier<Screen> screenRebuilder;
    private final boolean anchorPopupAtButtonBottom;

    private Button visibilityButton;
    private Button dropdownButton;
    private ProfilePopupMenu popup;

    /**
     * Create a controller bound to a specific Key Binds screen instance.
     *
     * @param owner                     The screen this controller is attached
     *                                  to. Must be a KeyBindsScreen or subclass
     *                                  for Manage-Profiles navigation to work.
     * @param screenRebuilder           Produces a fresh instance of the owner
     *                                  screen for post-swap navigation.
     * @param anchorPopupAtButtonBottom If true, anchor the popup's bottom edge
     *                                  at the dropdown button's bottom;
     *                                  otherwise at its top. Vanilla anchors at
     *                                  top (popup floats above the button);
     *                                  Controlling anchors at bottom.
     */
    public ProfileScreenController(Screen owner, Supplier<Screen> screenRebuilder,
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
        Identifier icon = editing ? VISIBILITY_EXIT_ICON : VISIBILITY_ICON;
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
        this.dropdownButton = Button.builder(dropdownLabel(), b -> openMenu())
                .bounds(0, 0, width, 20)
                .build();
        return this.dropdownButton;
    }

    /**
     * Route a mouse click through the popup menu when it is open. Returns
     * true when the click was consumed (popup was open) so the caller can
     * short-circuit its usual mouse handling.
     *
     * @param event The mouse button event.
     */
    public boolean handleMouseClick(MouseButtonEvent event) {
        if (popup == null) {
            return false;
        }
        boolean consumed = popup.mouseClicked(event.x(), event.y(), event.button());
        if (!consumed && !popup.containsPoint(event.x(), event.y())) {
            popup = null;
        }
        return true;
    }

    /**
     * Render the popup menu if it is open.
     *
     * @param gfx    The graphics extractor.
     * @param mouseX The mouse X coordinate.
     * @param mouseY The mouse Y coordinate.
     */
    public void renderPopup(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        if (popup != null) {
            popup.render(gfx, mouseX, mouseY);
        }
    }

    private void toggleEditMode() {
        HiddenBindingManager.toggleEditMode();
        popup = null;
        skipVisibilityResetOnNextEnter = true;
        Minecraft.getInstance().setScreenAndShow(screenRebuilder.get());
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
        Minecraft.getInstance().setScreenAndShow(screenRebuilder.get());
    }

    private void openNewProfileDialog() {
        popup = null;
        Minecraft.getInstance().setScreenAndShow(new ProfileNameDialog(
                owner,
                Component.translatable("multi.profile.dialog.new.title"),
                Component.translatable("multi.profile.dialog.create"),
                "",
                name -> {
                    ProfileManager.create(name);
                    Minecraft.getInstance().setScreenAndShow(owner);
                }));
    }

    private void openManageProfiles() {
        popup = null;
        Minecraft.getInstance().setScreenAndShow(new ManageProfilesScreen((KeyBindsScreen) owner));
    }

    private static Component dropdownLabel() {
        return Component.literal(ProfileManager.getActiveProfileName() + " ▼");
    }
}
