package us.kenny.mixin;

import com.google.common.collect.ImmutableList;
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
    private Text bindingName;
    @Final
    @Shadow
    private ButtonWidget editButton;
    @Final
    @Shadow
    private ButtonWidget resetButton;

    private ButtonWidget addKeyBindingButton;
    private ControlsListWidget controlsListWidget;
    private KeyBindingEntry self;

    /**
     * Register the new binding in our manager and build a widget (KeyBindingEntry) for it.
     */
    private void createMultiKeyBinding() {
        UUID multiKeyBindingId = MultiKeyBindingManager.addKeyBinding(binding.getTranslationKey(), -1);

        // Prefix the "dummy" KeyBinding so that we know it's one of our custom bindings & store our MultiKeyBinding UUID in the KeyBinding category field
        KeyBinding newKeyBinding = new KeyBinding("multi." + binding.getTranslationKey(), -1, multiKeyBindingId.toString());
        KeyBindingEntry newKeyBindingEntry = KeyBindingEntryAccessor.create(controlsListWidget, newKeyBinding, Text.of("     |"));

        controlsListWidget.children().add(controlsListWidget.children().indexOf(this.self) + 1, newKeyBindingEntry);
    }

    /**
     * Unregister the binding in our manager and remove its widget.
     *
     * @param action            The game action which the key binding is an alternate binding of.
     * @param multiKeyBindingId The UUID of the key binding to remove.
     */
    private void removeMultiKeyBinding(String action, UUID multiKeyBindingId) {
        MultiKeyBindingManager.removeKeyBinding(action, multiKeyBindingId);

        controlsListWidget.children().removeIf(c -> c instanceof KeyBindingEntry
                && ((KeyBindingEntryAccessor) c).getBinding().getCategory().equals(multiKeyBindingId.toString()));
    }

    /**
     * Injected in the constructor:
     * The injected code will check if this entry is a native key binding or a custom one
     * created by this mod.
     * - If it is native, build a "+" button to allow the player to create additional custom bindings when pressed.
     * - If it is custom, build a "delete" button to allow the player to remove this binding.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(ControlsListWidget controlsListWidget, final KeyBinding binding, final Text bindingName, CallbackInfo ci) {
        this.self = (KeyBindingEntry) (Object) this;
        this.controlsListWidget = controlsListWidget;

        // If this is one our custom key bindings, build a "delete" button
        if (binding.getTranslationKey().startsWith("multi.")) {
            this.addKeyBindingButton = ButtonWidget.builder(Text.literal("\uD83D\uDDD1").formatted(Formatting.RED), (button) -> {
                        removeMultiKeyBinding(binding.getTranslationKey().replaceFirst("^multi.", ""), UUID.fromString(binding.getCategory()));
                    })
                    .size(20, 20)
                    .build();
        } else {
            // If this is a native key binding, build a "+" button
            this.addKeyBindingButton = ButtonWidget.builder(Text.of("+"), (button) -> {
                        self.setFocused(false);
                        createMultiKeyBinding();
                    })
                    .size(20, 20)
                    .build();
        }
    }

    /**
     * Injected in the render method:
     * The injected code will check if this entry is a native key binding or a custom one
     * created by this mod.
     * - If it is native, render a "+" button to allow the player to create additional custom bindings when pressed.
     * - If it is custom, render a "trash can" button to allow the player to remove this binding.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        // Mimic the positioning and layout of the existing buttons
        int scrollbarX = controlsListWidget.getRowRight() + 6 + 2;
        int buttonX = scrollbarX - 165; // 5 wide gap between buttons, 20 wide "+" button
        int buttonY = y - 2; // Align with the existing buttons

        addKeyBindingButton.setPosition(buttonX, buttonY);
        addKeyBindingButton.render(context, mouseX, mouseY, tickDelta);
    }

    // Override hardcoded lists that enable our custom buttons to be interacted with
    @Override
    public List<? extends Element> children() {
        return ImmutableList.of(this.editButton, this.resetButton, this.addKeyBindingButton);
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
        return ImmutableList.of(this.editButton, this.resetButton, this.addKeyBindingButton);
    }
}