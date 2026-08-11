package us.kenny.core.profile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Modal-style overlay for entering a new or renamed profile name. Dims
 * the parent screen and returns to it on submit or cancel.
 */
public final class ProfileNameDialog extends Screen {
    private final Screen parent;
    private final Component titleText;
    private final Component confirmText;
    private final String initialValue;
    private final Consumer<String> onSubmit;

    private EditBox nameField;
    private Button confirmButton;

    public ProfileNameDialog(Screen parent, Component title, Component confirm,
            String initialValue, Consumer<String> onSubmit) {
        super(title);
        this.parent = parent;
        this.titleText = title;
        this.confirmText = confirm;
        this.initialValue = initialValue;
        this.onSubmit = onSubmit;
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
                        b -> Minecraft.getInstance().setScreenAndShow(parent))
                .bounds(cx + 5, cy + 20, 95, 20)
                .build();
        addRenderableWidget(cancelButton);

        updateConfirmEnabled();
    }

    private void updateConfirmEnabled() {
        this.confirmButton.active = ProfileNameValidator.isValid(this.nameField.getValue());
    }

    private void submit() {
        if (!ProfileNameValidator.isValid(this.nameField.getValue())) {
            return;
        }
        this.onSubmit.accept(this.nameField.getValue());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        parent.extractRenderState(gfx, -1, -1, partialTicks);
        gfx.fill(0, 0, this.width, this.height, 0xC0000000);

        int cx = this.width / 2;
        int cy = this.height / 2;
        gfx.text(this.font, titleText, cx - this.font.width(titleText) / 2, cy - 50, 0xFFFFFFFF);

        Component hint = Component.translatable("multi.profile.dialog.hint");
        gfx.text(this.font, hint, cx - this.font.width(hint) / 2, cy + 5, 0xFF999999);

        super.extractRenderState(gfx, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }
}
