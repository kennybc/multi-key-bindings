package us.kenny.mixin.controlling;

import com.blamejared.controlling.client.NewKeyBindsList;
import com.blamejared.controlling.client.NewKeyBindsList.KeyEntry;
import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.option.ControlsListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
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
import us.kenny.core.controlling.ControllingHideableKeyEntry;
import us.kenny.core.controlling.ControllingMultiKeyBindingEntry;

import java.util.List;

@Mixin(KeyEntry.class)
public abstract class KeyEntryMixin extends ControlsListWidget.Entry implements ControllingHideableKeyEntry {
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
    private boolean hidden;
    @Unique
    private ButtonWidget addKeyBindingButton;
    @Unique
    private NewKeyBindsList newKeyBindsList;
    @Unique
    private KeyEntry self;

    /**
     * @see us.kenny.mixin.KeyBindingEntryMixin#createCustomKeyBinding
     */
    @Unique
    private void createCustomKeyBinding() {
        MultiKeyBinding multiKeyBinding = MultiKeyBindingManager.addKeyBinding(
                key.getTranslationKey(),
                key.getCategory(),
                "key.keyboard.unknown");

        MultiKeyBindingEntry multiKeyBindingEntry = new ControllingMultiKeyBindingEntry(newKeyBindsList,
                multiKeyBinding);
        newKeyBindsList.allEntries.add(newKeyBindsList.allEntries.indexOf(this.self) + 1, multiKeyBindingEntry);
        newKeyBindsList.children().add(newKeyBindsList.children().indexOf(this.self) + 1, multiKeyBindingEntry);
    }

    @Unique
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * @see us.kenny.mixin.KeyBindingEntryMixin#onInit
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(NewKeyBindsList newKeyBindsList, KeyBinding keyBinding, Text bindingName, CallbackInfo ci) {
        this.self = (KeyEntry) (Object) this;
        this.hidden = false;
        this.newKeyBindsList = newKeyBindsList;

        this.addKeyBindingButton = ButtonWidget.builder(Text.of("+"), (button) -> {
            this.self.setFocused(false);
            createCustomKeyBinding();
        })
                .size(20, 20)
                .build();
    }

    /**
     * @see us.kenny.mixin.KeyBindingEntryMixin#onRender
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
            int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        int scrollbarX = newKeyBindsList.getRowRight() + 6 + 2;
        int buttonX = scrollbarX - 165;
        int buttonY = y - 2;

        addKeyBindingButton.setPosition(buttonX, buttonY);
        addKeyBindingButton.render(context, mouseX, mouseY, tickDelta);
    }

    /**
     * Deactivates key binding reset button.
     * 
     * @see us.kenny.core.controlling.ControllingHideableKeyEntry
     */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/ButtonWidget;render(Lnet/minecraft/client/gui/DrawContext;IIF)V", ordinal = 0))
    private void onResetButtonRender(ButtonWidget buttonWidget, DrawContext context, int mouseX, int mouseY,
            float delta,
            Operation<Void> original) {
        buttonWidget.active = !this.hidden && !this.key.isDefault();
        ;

        original.call(buttonWidget, context, mouseX, mouseY, delta);
    }

    /**
     * Deactivates key binding change key button.
     * 
     * @see us.kenny.core.controlling.ControllingHideableKeyEntry
     */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/ButtonWidget;render(Lnet/minecraft/client/gui/DrawContext;IIF)V", ordinal = 1))
    private void onChangeKeyButtonRender(ButtonWidget buttonWidget, DrawContext context, int mouseX, int mouseY,
            float delta,
            Operation<Void> original) {
        buttonWidget.active = !this.hidden;

        original.call(buttonWidget, context, mouseX, mouseY, delta);
    }

    /**
     * @see us.kenny.mixin.KeyBindingEntryMixin#children
     */
    @Override
    public List<? extends Element> children() {
        return ImmutableList.of(this.btnChangeKeyBinding, this.btnResetKeyBinding, this.addKeyBindingButton);
    }

    /**
     * @see us.kenny.mixin.KeyBindingEntryMixin#selectableChildren
     */
    @Override
    public List<? extends Selectable> selectableChildren() {
        return ImmutableList.of(this.btnChangeKeyBinding, this.btnResetKeyBinding, this.addKeyBindingButton);
    }
}
