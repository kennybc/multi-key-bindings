package us.kenny.mixin;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.screen.option.ControlsListWidget.KeyBindingEntry;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mixin(ControlsListWidget.KeyBindingEntry.class)
public abstract class ControlsListWidgetMixin {
    @Final
    @Shadow
    private KeyBinding binding;
    @Final
    @Shadow
    private Text bindingName;

    @Shadow
    abstract void update();

    @Final
    private ButtonWidget addKeyBindingButton;
    @Final
    private ControlsListWidget controlsListWidget;

    private static final Logger LOGGER = LoggerFactory.getLogger("better-key-bindings");

    // Build and register the "+" button
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(ControlsListWidget controlsListWidget, final KeyBinding binding, final Text bindingName, CallbackInfo ci) {
        ControlsListWidget.KeyBindingEntry self = (KeyBindingEntry) (Object) this;
        this.controlsListWidget = controlsListWidget;

        addKeyBindingButton = ButtonWidget.builder(Text.of("+"), (button) -> {
                    self.setFocused(false);

                    KeyBinding newKeyBinding = new KeyBinding(binding.getTranslationKey() + "_alt", -1, binding.getCategory());
                    KeyBindingEntry newKeyBindingEntry = KeyBindingEntryAccessor.create(controlsListWidget, newKeyBinding, Text.of("     |"));

                    controlsListWidget.children().add(controlsListWidget.children().indexOf(self) + 1, newKeyBindingEntry);
                    controlsListWidget.update();
                })
                .size(20, 20)
                .build();
        this.update();
    }

    // Render the "+" button
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        // Mimic the positioning and layout of the existing buttons
        int scrollbarX = controlsListWidget.getRowRight() + 6 + 2;
        int buttonX = scrollbarX - 165; // 5 wide gap between buttons, 20 wide "+" button
        int buttonY = y - 2; // Align with the existing buttons

        addKeyBindingButton.setPosition(buttonX, buttonY);
        addKeyBindingButton.render(context, mouseX, mouseY, tickDelta);
    }

    // Redirect hardcoded lists that enable the buttons to be interacted with
    @Redirect(method = "children",
            at = @At(value = "INVOKE", remap = false, target = "Lcom/google/common/collect/ImmutableList;of(Ljava/lang/Object;Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList;"))
    private ImmutableList<Element> redirectChildren(Object first, Object second) {
        return ImmutableList.of((Element) first, (Element) second, addKeyBindingButton);
    }

    @Redirect(method = "selectableChildren",
            at = @At(value = "INVOKE", remap = false, target = "Lcom/google/common/collect/ImmutableList;of(Ljava/lang/Object;Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList;"))
    private ImmutableList<Element> redirectSelectableChildren(Object first, Object second) {
        return ImmutableList.of((Element) first, (Element) second, addKeyBindingButton);
    }
}