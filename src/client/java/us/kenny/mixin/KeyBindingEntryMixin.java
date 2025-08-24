package us.kenny.mixin;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.screen.option.ControlsListWidget.KeyBindingEntry;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import us.kenny.MultiKeyBindingManager;

import java.util.List;
import java.util.UUID;

@Mixin(ControlsListWidget.KeyBindingEntry.class)
public abstract class KeyBindingEntryMixin extends ControlsListWidget.Entry {
    @Final
    @Shadow
    private KeyBinding binding;
    @Final
    @Shadow
    private ButtonWidget editButton;
    @Final
    @Shadow
    private ButtonWidget resetButton;

    @Unique
    private ButtonWidget addKeyBindingButton;
    @Unique
    private ControlsListWidget controlsListWidget;
    @Unique
    private KeyBindingEntry self;

    /**
     * Register the new binding in our manager and build a widget (KeyBindingEntry)
     * for it.
     */
    @Unique
    private void createCustomKeyBinding() {
        KeyBinding keyBinding = MultiKeyBindingManager.addKeyBinding("multi." + binding.getTranslationKey(),
                "key.keyboard.unknown");

        KeyBindingEntry keyBindingEntry = KeyBindingEntryAccessor.create(controlsListWidget, keyBinding,
                Text.translatable(binding.getTranslationKey()));
        controlsListWidget.children().add(controlsListWidget.children().indexOf(this.self) + 1, keyBindingEntry);
    }

    /**
     * Unregister the binding in our manager and remove its widget.
     *
     * @param keyBindingId The UUID of the key binding to remove.
     */
    @Unique
    private void removeMultiKeyBinding(UUID keyBindingId) {
        MultiKeyBindingManager.removeKeyBinding(keyBindingId);

        controlsListWidget.children().removeIf(c -> c instanceof KeyBindingEntry
                && ((KeyBindingEntryAccessor) c).getBinding().getCategory().equals(keyBindingId.toString()));
    }

    /**
     * Injected in the constructor:
     * The injected code will check if this entry is a native key binding or a
     * custom one
     * created by this mod.
     * - If it is native, build a "+" button to allow the player to create
     * additional custom bindings when pressed.
     * - If it is custom, build a "delete" button to allow the player to remove this
     * binding.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(ControlsListWidget controlsListWidget, final KeyBinding keyBinding, final Text bindingName,
            CallbackInfo ci) {
        this.self = (KeyBindingEntry) (Object) this;
        this.controlsListWidget = controlsListWidget;

        // If this is one our custom key bindings, build a "delete" button
        if (keyBinding.getTranslationKey().startsWith("multi.")) {
            this.addKeyBindingButton = ButtonWidget
                    .builder(Text.literal("\uD83D\uDDD1").formatted(Formatting.RED),
                            (button) -> removeMultiKeyBinding(UUID.fromString(keyBinding.getCategory())))
                    .size(20, 20)
                    .build();
        } else {
            // If this is a native key binding, build a "+" button
            this.addKeyBindingButton = ButtonWidget.builder(Text.of("+"), (button) -> {
                this.self.setFocused(false);
                createCustomKeyBinding();
            })
                    .size(20, 20)
                    .build();
        }
    }

    /**
     * Injected in the render method:
     * The injected code will check if this entry is a native key binding or a
     * custom one
     * created by this mod.
     * - If it is native, render a "+" button to allow the player to create
     * additional custom bindings when pressed.
     * - If it is custom, render a "trash can" button to allow the player to remove
     * this binding.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
            int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        // Mimic the positioning and layout of the existing buttons
        int scrollbarX = controlsListWidget.getRowRight() + 6 + 2;
        int buttonX = scrollbarX - 165; // 5 wide gap between buttons, 20 wide "+" button
        int buttonY = y - 2; // Align with the existing buttons

        addKeyBindingButton.setPosition(buttonX, buttonY);
        addKeyBindingButton.render(context, mouseX, mouseY, tickDelta);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V"))
    private void onRender(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color,
            Operation<Void> original) {
        // Draw arrow instead of displaying action name for custom key bindings
        if (binding.getTranslationKey().startsWith("multi.")) {
            int leftOffset = 10;
            int topOffset = 5;
            int arrowLength = 20;

            context.fill(x + leftOffset, y + topOffset, x + leftOffset + arrowLength, y + topOffset + 1, color);
            context.fill(x + leftOffset, y, x + leftOffset + 1, y + topOffset, color);

            int tipX = x + leftOffset + arrowLength;
            for (int i = 0; i <= 2; i++) {
                context.fill(tipX - i, y + topOffset - i, tipX - i + 1, y + topOffset - i + 1, color);
                context.fill(tipX - i, y + topOffset + i, tipX - i + 1, y + topOffset + i + 1, color);
            }
        } else {
            original.call(context, textRenderer, text, x, y, color);
        }
    }

    /**
     * The following override hardcoded lists that enable our custom buttons to be
     * interacted with.
     */
    @Override
    public List<? extends Element> children() {
        return ImmutableList.of(this.editButton, this.resetButton, this.addKeyBindingButton);
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
        return ImmutableList.of(this.editButton, this.resetButton, this.addKeyBindingButton);
    }
}