package us.kenny.mixin;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.screen.option.ControlsListWidget.KeyBindingEntry;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;

import java.util.ArrayList;
import java.util.List;

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
     * Register the new binding in our manager and create an entry in the key bind
     * list for it.
     */
    @Unique
    private void createCustomKeyBinding() {
        MultiKeyBinding multiKeyBinding = MultiKeyBindingManager.addKeyBinding(
                binding.getId(),
                binding.getCategory(),
                InputUtil.UNKNOWN_KEY);
        MultiKeyBindingEntry multiKeyBindingEntry = new MultiKeyBindingEntry(controlsListWidget, multiKeyBinding);

        List<ControlsListWidget.Entry> entries = new ArrayList<>(controlsListWidget.children());
        entries.add(controlsListWidget.children().indexOf(this.self) + 1, multiKeyBindingEntry);
        controlsListWidget.replaceEntries(entries);
    }

    /**
     * Builds our custom "+" button in native key binding entries.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(ControlsListWidget controlsListWidget, final KeyBinding keyBinding, final Text bindingName,
            CallbackInfo ci) {
        this.self = (KeyBindingEntry) (Object) this;
        this.controlsListWidget = controlsListWidget;

        this.addKeyBindingButton = ButtonWidget.builder(Text.of("+"), (button) -> {
            this.self.setFocused(false);
            createCustomKeyBinding();
        })
                .size(20, 20)
                .build();

    }

    /**
     * Renders our custom "+" button in native key binding entries.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks,
            CallbackInfo ci) {
        // Mimic the positioning and layout of the existing buttons
        int scrollbarX = controlsListWidget.getRowRight() + 6 + 2;
        int buttonX = scrollbarX - 165; // 5 wide gap between buttons, 20 wide "+" button
        int buttonY = this.getContentY() - 2;
        ; // Align with the existing buttons

        addKeyBindingButton.setPosition(buttonX, buttonY);
        addKeyBindingButton.render(context, mouseX, mouseY, deltaTicks);
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