package us.kenny.mixin.controlling;

import com.blamejared.controlling.client.NewKeyBindsList;
import com.blamejared.controlling.client.NewKeyBindsList.KeyEntry;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
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

@Mixin(value = KeyEntry.class, remap = false)
public abstract class KeyEntryMixin extends ControlsListWidget.Entry {
    @Final
    @Shadow
    private KeyBinding key;
    @Final
    @Shadow
    private ButtonWidget btnChangeKeyBinding;
    @Final
    @Shadow
    private ButtonWidget btnResetKeyBinding;

    @Unique
    private ButtonWidget addKeyBindingButton;
    @Unique
    private NewKeyBindsList newKeyBindsList;
    @Unique
    private KeyEntry self;

    /**
     * Register the new binding in our manager and build a widget (KeyBindingEntry)
     * for it.
     */
    @Unique
    private void createCustomKeyBinding() {
        KeyBinding keyBinding = MultiKeyBindingManager.addKeyBinding("multi." + key.getTranslationKey(),
                "key.keyboard.unknown");

        KeyEntry keyEntry = KeyEntryAccessor.create(newKeyBindsList, keyBinding,
                Text.of("     |"));
        newKeyBindsList.children().add(newKeyBindsList.children().indexOf(this.self) + 1, keyEntry);
    }

    /**
     * Unregister the binding in our manager and remove its widget.
     *
     * @param keyBindingId The UUID of the key binding to remove.
     */
    @Unique
    private void removeMultiKeyBinding(UUID keyBindingId) {
        MultiKeyBindingManager.removeKeyBinding(keyBindingId);

        newKeyBindsList.children().removeIf(c -> c instanceof KeyEntry
                && ((KeyEntryAccessor) c).getBinding().getCategory().equals(keyBindingId.toString()));
    }

    @Inject(method = "<init>(Lcom/blamejared/controlling/client/NewKeyBindsList;Lnet/minecraft/client/option/KeyBinding;Lnet/minecraft/text/Text;)V", at = @At("TAIL"), remap = false)
    private void onInit(NewKeyBindsList newKeyBindsList, KeyBinding binding, Text bindingName, CallbackInfo ci) {
        this.self = (KeyEntry) (Object) this;
        this.newKeyBindsList = newKeyBindsList;

        // If this is one our custom key bindings, build a "delete" button
        if (binding.getTranslationKey().startsWith("multi.")) {
            this.addKeyBindingButton = ButtonWidget
                    .builder(Text.literal("\uD83D\uDDD1").formatted(Formatting.RED),
                            (button) -> removeMultiKeyBinding(UUID.fromString(binding.getCategory())))
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

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
            int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        // Mimic the positioning and layout of the existing buttons
        int scrollbarX = newKeyBindsList.getRowRight() + 6 + 2;
        int buttonX = scrollbarX - 165; // 5 wide gap between buttons, 20 wide "+" button
        int buttonY = y - 2; // Align with the existing buttons

        addKeyBindingButton.setPosition(buttonX, buttonY);
        addKeyBindingButton.render(context, mouseX, mouseY, tickDelta);
    }

    @Override
    public List<? extends Element> children() {
        return ImmutableList.of(this.btnChangeKeyBinding, this.btnResetKeyBinding, this.addKeyBindingButton);
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
        return ImmutableList.of(this.btnChangeKeyBinding, this.btnResetKeyBinding, this.addKeyBindingButton);
    }
}
