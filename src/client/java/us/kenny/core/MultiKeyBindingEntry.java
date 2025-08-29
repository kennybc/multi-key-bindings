package us.kenny.core;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Unique;
import us.kenny.MultiKeyBindingManager;
import us.kenny.mixin.ControlsListWidgetAccessor;

import java.util.List;

public class MultiKeyBindingEntry extends ControlsListWidget.Entry {
    protected final ControlsListWidget parentList;
    protected MultiKeyBindingScreen parentScreen;
    private final MultiKeyBinding multiKeyBinding;
    private boolean duplicate;

    private final ButtonWidget editButton;
    private final ButtonWidget resetButton;
    protected ButtonWidget removeKeyBindingButton;

    public MultiKeyBindingEntry(final ControlsListWidget parentList, final MultiKeyBinding multiKeyBinding) {
        this.parentList = parentList;
        this.parentScreen = ((MultiKeyBindingScreen) ((ControlsListWidgetAccessor) parentList)
                .getParent());
        this.multiKeyBinding = multiKeyBinding;

        this.editButton = ButtonWidget
                .builder(Text.of(multiKeyBinding.getAction()), button -> {
                    this.parentScreen.setSelectedMultiKeyBinding(multiKeyBinding);
                    this.parentList.update();
                })
                .size(75, 20)
                .build();

        this.resetButton = ButtonWidget
                .builder(Text.translatable("controls.reset"), button -> {
                    multiKeyBinding.setKey(InputUtil.UNKNOWN_KEY);
                    this.parentList.update();
                })
                .size(50, 20)
                .build();

        this.removeKeyBindingButton = ButtonWidget
                .builder(Text.literal("\uD83D\uDDD1").formatted(Formatting.RED),
                        (button) -> {
                            MultiKeyBindingManager.removeKeyBinding(multiKeyBinding);
                            this.parentScreen.setSelectedMultiKeyBinding(null);
                            this.parentList.children().remove(this);
                            this.parentList.update();
                        })
                .size(20, 20)
                .build();

        this.update();
    }

    @Unique
    public MultiKeyBinding getMultiKeyBinding() {
        return this.multiKeyBinding;
    }

    /**
     * Renders our custom entry in the list of key bindings.
     */
    @Override
    public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
            int mouseY, boolean hovered, float tickProgress) {

        // Render buttons
        int scrollbarX = this.parentList.getRowRight() + 6 + 2;
        int buttonY = y - 2;

        int resetButtonX = scrollbarX - this.resetButton.getWidth() - 10;
        this.resetButton.setPosition(resetButtonX, buttonY);
        this.resetButton.render(context, mouseX, mouseY, tickProgress);

        int editButtonX = resetButtonX - this.editButton.getWidth() - 5;
        this.editButton.setPosition(editButtonX, buttonY);
        this.editButton.render(context, mouseX, mouseY, tickProgress);

        int removeKeyBindingButtonX = editButtonX - this.removeKeyBindingButton.getWidth() - 5;
        this.removeKeyBindingButton.setPosition(removeKeyBindingButtonX, buttonY);
        this.removeKeyBindingButton.render(context, mouseX, mouseY, tickProgress);

        // Render an arrow instead of action name
        int leftOffset = 10;
        int topOffset = 5;
        int arrowLength = 20;

        context.fill(x + leftOffset, y + topOffset, x + leftOffset + arrowLength, y + topOffset + 1,
                Colors.ALTERNATE_WHITE);
        context.fill(x + leftOffset, y, x + leftOffset + 1, y + topOffset, Colors.ALTERNATE_WHITE);

        int tipX = x + leftOffset + arrowLength;
        for (int i = 0; i <= 2; i++) {
            context.fill(tipX - i, y + topOffset - i, tipX - i + 1, y + topOffset - i + 1,
                    Colors.ALTERNATE_WHITE);
            context.fill(tipX - i, y + topOffset + i, tipX - i + 1, y + topOffset + i + 1,
                    Colors.ALTERNATE_WHITE);
        }
    }

    /**
     * Checks for duplicates and updates buttons to reflect selection status and
     * duplication key bindings.
     */
    @Override
    public void update() {
        this.editButton.setMessage(this.multiKeyBinding.getKey().getLocalizedText());
        this.resetButton.active = !this.multiKeyBinding.getKey().equals(InputUtil.UNKNOWN_KEY);
        this.duplicate = false;

        MutableText duplicates = Text.empty();
        if (!this.multiKeyBinding.getKey().equals(InputUtil.UNKNOWN_KEY)) {
            for (KeyBinding kb : MultiKeyBindingManager.getGameOptions().allKeys) {
                if (!kb.isUnbound() && kb.getBoundKeyTranslationKey()
                        .equals(this.multiKeyBinding.getKey().getTranslationKey())) {
                    if (this.duplicate) {
                        duplicates.append(", ");
                    }

                    this.duplicate = true;
                    duplicates.append(Text.translatable(kb.getTranslationKey()));
                }
            }

            for (MultiKeyBinding mkb : MultiKeyBindingManager.getKeyBindings()) {
                if (!mkb.getId().equals(this.multiKeyBinding.getId()) && !mkb.getKey().equals(InputUtil.UNKNOWN_KEY) &&
                        mkb.getKey().getTranslationKey()
                                .equals(this.multiKeyBinding.getKey().getTranslationKey())) {
                    if (this.duplicate) {
                        duplicates.append(", ");
                    }

                    this.duplicate = true;
                    duplicates.append(Text.translatable(mkb.getAction().replaceFirst("^multi.", "")));
                }
            }
        }

        if (this.duplicate) {
            this.editButton
                    .setMessage(
                            Text.literal("[ ").append(this.editButton.getMessage().copy().formatted(Formatting.WHITE))
                                    .append(" ]").formatted(Formatting.RED));
            this.editButton.setTooltip(
                    Tooltip.of(Text.translatable("controls.keybinds.duplicateKeybinds", new Object[] { duplicates })));
        } else {
            this.editButton.setTooltip(null);
        }

        if (this.parentScreen.getSelectedMultiKeyBinding() == this.multiKeyBinding) {
            this.editButton
                    .setMessage(
                            Text.literal("> ")
                                    .append(this.editButton.getMessage().copy()
                                            .formatted(new Formatting[] {
                                                    Formatting.WHITE,
                                                    Formatting.UNDERLINE }))
                                    .append(" <")
                                    .formatted(Formatting.YELLOW));
        }
    }

    /**
     * The following override hardcoded lists that enable our custom buttons to be
     * interacted with.
     */
    @Override
    public List<? extends Element> children() {
        return ImmutableList.of(this.editButton, this.resetButton, this.removeKeyBindingButton);
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
        return ImmutableList.of(this.editButton, this.resetButton, this.removeKeyBindingButton);
    }
}
