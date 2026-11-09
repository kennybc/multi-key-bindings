package us.kenny.core.profile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Modal-style overlay for entering a new or renamed profile name. Dims
 * the parent screen and returns to it on submit or cancel.
 */
public final class ProfileNameDialog extends Screen {
    private final Screen parent;
    private final Component confirmText;
    private final String initialValue;
    private final Consumer<String> onSubmit;
    private final Predicate<String> isTaken;

    private EditBox nameField;
    private Button confirmButton;

    /**
     * @param isTaken Returns true when a candidate name would clash with an
     *                existing profile. Taken names disable the confirm button
     *                and show an error hint instead of failing silently.
     */
    public ProfileNameDialog(Screen parent, Component title, Component confirm,
            String initialValue, Predicate<String> isTaken, Consumer<String> onSubmit) {
        super(title);
        this.parent = parent;
        this.confirmText = confirm;
        this.initialValue = initialValue;
        this.isTaken = isTaken;
        this.onSubmit = onSubmit;
    }

    /**
     * Taken-name predicate for creating a new profile: any existing name,
     * compared case-insensitively.
     */
    public static Predicate<String> takenForCreate() {
        return name -> ProfileNameValidator.isTaken(name, us.kenny.ProfileManager.listProfileNames());
    }

    /**
     * Taken-name predicate for renaming {@code currentName}: any other existing
     * name. A case-only change of the profile's own name is allowed.
     */
    public static Predicate<String> takenForRename(String currentName) {
        return name -> !name.equalsIgnoreCase(currentName)
                && ProfileNameValidator.isTaken(name, us.kenny.ProfileManager.listProfileNames());
    }

    private boolean isAcceptable(String value) {
        return ProfileNameValidator.isValid(value) && !isTaken.test(value);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.nameField = new EditBox(this.font, cx - 100, cy - 20, 200, 20, Component.empty());
        this.nameField.setMaxLength(32);
        this.nameField.setValue(initialValue == null ? "" : initialValue);
        this.nameField.setResponder(v -> updateConfirmEnabled());
        addRenderableWidget(this.nameField);
        setInitialFocus(this.nameField);

        this.confirmButton = Button.builder(confirmText, b -> submit())
                .bounds(cx - 100, cy + 20, 95, 20)
                .build();
        addRenderableWidget(this.confirmButton);

        Button cancelButton = Button.builder(Component.translatable("multi.profile.dialog.cancel"),
                        b -> Minecraft.getInstance().setScreen(parent))
                .bounds(cx + 5, cy + 20, 95, 20)
                .build();
        addRenderableWidget(cancelButton);

        updateConfirmEnabled();
    }

    private void updateConfirmEnabled() {
        this.confirmButton.active = isAcceptable(this.nameField.getValue());
    }

    private void submit() {
        if (!isAcceptable(this.nameField.getValue())) {
            return;
        }
        this.onSubmit.accept(this.nameField.getValue());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        // The parent draws its own (blurred) background; a second blur in the
        // same frame is not allowed, so renderBackground below is a no-op.
        parent.render(gfx, -1, -1, partialTicks);
        // Lift the overlay and our widgets above everything the parent drew.
        gfx.flush();
        gfx.pose().pushPose();
        gfx.pose().translate(0.0F, 0.0F, 400.0F);
        gfx.fill(0, 0, this.width, this.height, 0xC0000000);

        int cx = this.width / 2;
        int cy = this.height / 2;
        gfx.drawString(this.font, this.title, cx - this.font.width(this.title) / 2, cy - 50, 0xFFFFFFFF);

        String value = this.nameField == null ? "" : this.nameField.getValue();
        boolean taken = ProfileNameValidator.isValid(value) && isTaken.test(value);
        Component hint = Component.translatable(taken ? "multi.profile.dialog.taken" : "multi.profile.dialog.hint");
        gfx.drawString(this.font, hint, cx - this.font.width(hint) / 2, cy + 5, taken ? 0xFFFF5555 : 0xFF999999);

        super.render(gfx, mouseX, mouseY, partialTicks);
        gfx.pose().popPose();
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
