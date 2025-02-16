package us.kenny.mixin;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
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

@Mixin(ControlsListWidget.KeyBindingEntry.class)
public abstract class ControlsListWidgetMixin {
    @Final
    @Shadow
    private KeyBinding binding;

    @Shadow
    abstract void update();

    @Final
    private ButtonWidget addKeyBindingButton;
    @Final
    private ControlsListWidget controlsListWidget;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(ControlsListWidget controlsListWidget, final KeyBinding binding, final Text bindingName, CallbackInfo ci) {
        this.controlsListWidget = controlsListWidget;

        // Build and register the "+" button
        addKeyBindingButton = ButtonWidget.builder(Text.of("+"), (button) -> {
                    ((EntryListWidgetAccessor) controlsListWidget).getChildren().clear();
                    controlsListWidget.update();
                })
                .size(20, 20)
                .build();
        this.update();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        // Mimic the positioning and layout of the existing buttons
        int scrollbarX = controlsListWidget.getRowRight() + 6 + 2;
        int buttonX = scrollbarX - 165; // 5 wide gap between buttons, 20 wide "+" button
        int buttonY = y - 2; // Align with the existing buttons

        // Render the "+" button
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