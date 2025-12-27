package us.kenny.core;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Unique;
import us.kenny.MultiKeyBindingManager;
import us.kenny.mixin.KeyBindsListAccessor;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.CommonColors;

public class MultiKeyBindingEntry extends KeyBindsList.Entry {
    protected final KeyBindsList parentList;
    protected MultiKeyBindingScreen parentScreen;
    private final MultiKeyBinding multiKeyBinding;
    private boolean duplicate;

    private final Button editButton;
    private final Button resetButton;
    protected Button removeKeyBindingButton;

    public MultiKeyBindingEntry(final KeyBindsList parentList, final MultiKeyBinding multiKeyBinding) {
        this.parentList = parentList;
        this.parentScreen = ((MultiKeyBindingScreen) ((KeyBindsListAccessor) parentList)
                .getKeyBindsScreen());
        this.multiKeyBinding = multiKeyBinding;

        this.editButton = Button
                .builder(Component.nullToEmpty(multiKeyBinding.getAction()), button -> {
                    this.parentScreen.setSelectedMultiKeyBinding(multiKeyBinding);
                    this.parentList.resetMappingAndUpdateButtons();
                })
                .size(75, 20)
                .build();

        this.resetButton = Button
                .builder(Component.translatable("controls.reset"), button -> {
                    multiKeyBinding.setKey(InputConstants.UNKNOWN);
                    this.parentList.resetMappingAndUpdateButtons();
                })
                .size(50, 20)
                .build();

        this.removeKeyBindingButton = Button
                .builder(Component.literal("\uD83D\uDDD1").withStyle(ChatFormatting.RED),
                        (button) -> {
                            MultiKeyBindingManager.removeKeyBinding(multiKeyBinding);
                            this.parentScreen.setSelectedMultiKeyBinding(null);

                            List<KeyBindsList.Entry> entries = new ArrayList<>(this.parentList.children());
                            entries.remove(this);
                            this.parentList.replaceEntries(entries);

                            this.parentList.resetMappingAndUpdateButtons();
                        })
                .size(20, 20)
                .build();

        this.refreshEntry();
    }

    @Unique
    public MultiKeyBinding getMultiKeyBinding() {
        return this.multiKeyBinding;
    }

    /**
     * Renders our custom entry in the list of key bindings.
     */
    @Override
    public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
        // Render buttons
        int scrollbarX = this.parentList.getRowRight() + 6 + 2;
        int contentX = this.getContentX();
        int contentY = this.getContentY();
        int buttonY = contentY - 2;

        int resetButtonX = scrollbarX - this.resetButton.getWidth() - 10;
        this.resetButton.setPosition(resetButtonX, buttonY);
        this.resetButton.render(graphics, mouseX, mouseY, deltaTicks);

        int editButtonX = resetButtonX - this.editButton.getWidth() - 5;
        this.editButton.setPosition(editButtonX, buttonY);
        this.editButton.render(graphics, mouseX, mouseY, deltaTicks);

        int removeKeyBindingButtonX = editButtonX - this.removeKeyBindingButton.getWidth() - 5;
        this.removeKeyBindingButton.setPosition(removeKeyBindingButtonX, buttonY);
        this.removeKeyBindingButton.render(graphics, mouseX, mouseY, deltaTicks);

        // Render an arrow instead of action name
        int leftOffset = 10;
        int topOffset = 5;
        int arrowLength = 20;

        graphics.fill(contentX + leftOffset, contentY + topOffset, contentX + leftOffset + arrowLength,
                contentY + topOffset + 1,
                CommonColors.LIGHTER_GRAY);
        graphics.fill(contentX + leftOffset, contentY, contentX + leftOffset + 1, contentY + topOffset,
                CommonColors.LIGHTER_GRAY);

        int tipX = contentX + leftOffset + arrowLength;
        for (int i = 0; i <= 2; i++) {
            graphics.fill(tipX - i, contentY + topOffset - i, tipX - i + 1, contentY + topOffset - i + 1,
                    CommonColors.LIGHTER_GRAY);
            graphics.fill(tipX - i, contentY + topOffset + i, tipX - i + 1, contentY + topOffset + i + 1,
                    CommonColors.LIGHTER_GRAY);
        }
    }

    /**
     * Checks for duplicates and updates buttons to reflect selection status and
     * duplication key bindings.
     */
    @Override
    public void refreshEntry() {
        this.editButton.setMessage(this.multiKeyBinding.getKey().getDisplayName());
        this.resetButton.active = !this.multiKeyBinding.getKey().equals(InputConstants.UNKNOWN);
        this.duplicate = false;

        MutableComponent duplicates = Component.empty();
        if (!this.multiKeyBinding.getKey().equals(InputConstants.UNKNOWN)) {
            for (KeyMapping kb : MultiKeyBindingManager.getGameOptions().keyMappings) {
                if (!kb.isUnbound() && kb.saveString()
                        .equals(this.multiKeyBinding.getKey().getName())) {
                    if (this.duplicate) {
                        duplicates.append(", ");
                    }

                    this.duplicate = true;
                    duplicates.append(Component.translatable(kb.getName()));
                }
            }

            for (MultiKeyBinding mkb : MultiKeyBindingManager.getKeyBindings()) {
                if (!mkb.getId().equals(this.multiKeyBinding.getId()) && !mkb.getKey().equals(InputConstants.UNKNOWN) &&
                        mkb.getKey().getName()
                                .equals(this.multiKeyBinding.getKey().getName())) {
                    if (this.duplicate) {
                        duplicates.append(", ");
                    }

                    this.duplicate = true;
                    duplicates.append(Component.translatable(mkb.getAction().replaceFirst("^multi.", "")));
                }
            }
        }

        if (this.duplicate) {
            this.editButton
                    .setMessage(
                            Component.literal("[ ").append(this.editButton.getMessage().copy().withStyle(ChatFormatting.WHITE))
                                    .append(" ]").withStyle(ChatFormatting.RED));
            this.editButton.setTooltip(
                    Tooltip.create(Component.translatable("controls.keybinds.duplicateKeybinds", new Object[] { duplicates })));
        } else {
            this.editButton.setTooltip(null);
        }

        if (this.parentScreen.getSelectedMultiKeyBinding() == this.multiKeyBinding) {
            this.editButton
                    .setMessage(
                            Component.literal("> ")
                                    .append(this.editButton.getMessage().copy()
                                            .withStyle(new ChatFormatting[] {
                                                    ChatFormatting.WHITE,
                                                    ChatFormatting.UNDERLINE }))
                                    .append(" <")
                                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    /**
     * The following override hardcoded lists that enable our custom buttons to be
     * interacted with.
     */
    @Override
    public List<? extends GuiEventListener> children() {
        return ImmutableList.of(this.editButton, this.resetButton, this.removeKeyBindingButton);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return ImmutableList.of(this.editButton, this.resetButton, this.removeKeyBindingButton);
    }
}
