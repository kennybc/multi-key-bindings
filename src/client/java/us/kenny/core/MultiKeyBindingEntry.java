package us.kenny.core;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Unique;
import us.kenny.ModifierManager;
import us.kenny.MultiKeyBindingManager;
import us.kenny.StickyToggleManager;
import us.kenny.mixin.KeyBindsListAccessor;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
    private final boolean primary;
    private boolean duplicate;

    private final Button editButton;
    private final Button resetButton;
    protected Button removeKeyBindingButton;
    protected Button addKeyBindingButton;

    /**
     * @see us.kenny.core.controlling.ControllingMultiKeyBindingEntry#setHidden
     */
    protected boolean hidden;

    public MultiKeyBindingEntry(final KeyBindsList parentList, final MultiKeyBinding multiKeyBinding) {
        this(parentList, multiKeyBinding, false);
    }

    public MultiKeyBindingEntry(final KeyBindsList parentList, final MultiKeyBinding multiKeyBinding,
            final boolean primary) {
        this.parentList = parentList;
        this.parentScreen = ((MultiKeyBindingScreen) ((KeyBindsListAccessor) parentList)
                .getKeyBindsScreen());
        this.multiKeyBinding = multiKeyBinding;
        this.primary = primary;

        this.editButton = Button
                .builder(Component.nullToEmpty(multiKeyBinding.getAction()), button -> {
                    this.parentScreen.setSelectedMultiKeyBinding(multiKeyBinding);
                    MultiKeyBindingManager.setKeyBinding(multiKeyBinding, InputConstants.UNKNOWN);
                    ModifierManager.setModifiers(multiKeyBinding.getId().toString(), List.of());
                    this.parentList.resetMappingAndUpdateButtons();
                })
                .size(75, 20)
                .build();

        this.resetButton = Button
                .builder(Component.translatable("controls.reset"), button -> {
                    MultiKeyBindingManager.setKeyBinding(multiKeyBinding, InputConstants.UNKNOWN);
                    ModifierManager.setModifiers(multiKeyBinding.getId().toString(), List.of());
                    this.parentList.resetMappingAndUpdateButtons();
                })
                .size(50, 20)
                .build();

        if (primary) {
            this.addKeyBindingButton = Button.builder(Component.literal("+"), button -> {
                MultiKeyBinding subBinding = MultiKeyBindingManager.addKeyBinding(
                        StickyToggleManager.stripMultiPrefix(multiKeyBinding.getAction()),
                        multiKeyBinding.getCategory(),
                        InputConstants.UNKNOWN);
                MultiKeyBindingEntry subEntry = new MultiKeyBindingEntry(this.parentList, subBinding);

                // Append after the contiguous run of sub-bindings already under this primary
                // so insertion order matches the order seen on next screen open.
                List<KeyBindsList.Entry> entries = new ArrayList<>(this.parentList.children());
                int insertAt = entries.indexOf(this) + 1;
                while (insertAt < entries.size() && entries.get(insertAt) instanceof MultiKeyBindingEntry sibling
                        && !sibling.primary
                        && sibling.multiKeyBinding.getAction().equals(multiKeyBinding.getAction())) {
                    insertAt++;
                }
                entries.add(insertAt, subEntry);
                this.parentList.replaceEntries(entries);
            }).size(20, 20).build();
        } else {
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
        }

        this.refreshEntry();
    }

    @Unique
    public MultiKeyBinding getMultiKeyBinding() {
        return this.multiKeyBinding;
    }

    @Unique
    public boolean isPrimary() {
        return this.primary;
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
        this.resetButton.active = !this.hidden && !this.multiKeyBinding.getKey().equals(InputConstants.UNKNOWN);
        this.resetButton.render(graphics, mouseX, mouseY, deltaTicks);

        int editButtonX = resetButtonX - this.editButton.getWidth() - 5;
        this.editButton.setPosition(editButtonX, buttonY);
        this.editButton.active = !this.hidden;
        this.editButton.render(graphics, mouseX, mouseY, deltaTicks);

        Button button = this.primary ? this.addKeyBindingButton : this.removeKeyBindingButton;
        int buttonX = editButtonX - button.getWidth() - 5;
        button.setPosition(buttonX, buttonY);
        button.render(graphics, mouseX, mouseY, deltaTicks);

        if (this.duplicate) {
            int stripeLeft = this.editButton.getX() - 6;
            graphics.fill(stripeLeft, this.getContentY() - 1, stripeLeft + 3, this.getContentBottom(), -256);
        }

        if (this.primary) {
            // Primary rows are top-level: render the action name on the left.
            Font font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.translatable(this.multiKeyBinding.getTranslationKey()), contentX,
                    this.getContentYMiddle() - font.lineHeight / 2, -1);
        } else {
            // Sub rows are indented under a parent: render an arrow instead of action name.
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
    }

    /**
     * Checks for duplicates and updates buttons to reflect selection status and
     * duplication key bindings.
     */
    @Override
    public void refreshEntry() {
        this.editButton.setMessage(this.multiKeyBinding.getDisplayName());
        this.resetButton.active = !this.multiKeyBinding.getKey().equals(InputConstants.UNKNOWN);
        this.duplicate = false;

        MutableComponent duplicates = Component.empty();
        if (!this.multiKeyBinding.getKey().equals(InputConstants.UNKNOWN)) {
            List<InputConstants.Key> modifiers = ModifierManager.getModifiers(this.multiKeyBinding.getId().toString());
            String selfKeyName = this.multiKeyBinding.getKey().getName();

            for (KeyMapping kb : MultiKeyBindingManager.getGameOptions().keyMappings) {
                if (!kb.isUnbound() && kb.saveString().equals(selfKeyName)
                        && ModifierManager.modifiersEqual(modifiers,
                                ModifierManager.getModifiers(kb.getName()))) {
                    if (this.duplicate) {
                        duplicates.append(", ");
                    }

                    this.duplicate = true;
                    duplicates.append(Component.translatable(kb.getName()));
                }
            }

            for (MultiKeyBinding mkb : MultiKeyBindingManager.getKeyBindings()) {
                if (!mkb.getId().equals(this.multiKeyBinding.getId())
                        && !mkb.getKey().equals(InputConstants.UNKNOWN)
                        && mkb.getKey().getName().equals(selfKeyName)
                        && ModifierManager.modifiersEqual(modifiers,
                                ModifierManager.getModifiers(mkb.getId().toString()))) {
                    if (this.duplicate) {
                        duplicates.append(", ");
                    }

                    this.duplicate = true;
                    duplicates.append(Component.translatable(mkb.getTranslationKey()));
                }
            }
        }

        if (this.duplicate) {
            this.editButton.setMessage(
                    Component.literal("[ ").append(this.editButton.getMessage().copy().withStyle(ChatFormatting.WHITE))
                            .append(" ]").withStyle(ChatFormatting.YELLOW));
            this.editButton.setTooltip(Tooltip.create(
                    Component.translatable("controls.keybinds.duplicateKeybinds", new Object[] { duplicates })));
        } else {
            this.editButton.setTooltip(null);
        }

        if (this.parentScreen.getSelectedMultiKeyBinding() == this.multiKeyBinding) {
            this.editButton
                    .setMessage(
                            Component.literal("> ")
                                    .append(this.editButton.getMessage().copy()
                                            .withStyle(new ChatFormatting[] { ChatFormatting.WHITE,
                                                    ChatFormatting.UNDERLINE }))
                                    .append(" <").withStyle(ChatFormatting.YELLOW));
        }
    }

    /**
     * The following override hardcoded lists that enable our custom buttons to be
     * interacted with.
     */
    @Override
    public List<? extends GuiEventListener> children() {
        Button button = this.primary ? this.addKeyBindingButton : this.removeKeyBindingButton;
        return ImmutableList.of(this.editButton, this.resetButton, button);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        Button button = this.primary ? this.addKeyBindingButton : this.removeKeyBindingButton;
        return ImmutableList.of(this.editButton, this.resetButton, button);
    }
}
