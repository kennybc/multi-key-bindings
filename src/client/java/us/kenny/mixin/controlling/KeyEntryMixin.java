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

import java.util.List;

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

    @Unique
    private void createCustomKeyBinding() {
        MultiKeyBinding multiKeyBinding = MultiKeyBindingManager.addKeyBinding("multi." + key.getTranslationKey(),
                "key.keyboard.unknown");

        MultiKeyBindingEntry multiKeyBindingEntry = new MultiKeyBindingEntry(newKeyBindsList, multiKeyBinding);
        newKeyBindsList.allEntries.add(newKeyBindsList.allEntries.indexOf(this.self) + 1, multiKeyBindingEntry);
        newKeyBindsList.children().add(newKeyBindsList.children().indexOf(this.self) + 1, multiKeyBindingEntry);
    }

    @Inject(method = "<init>(Lcom/blamejared/controlling/client/NewKeyBindsList;Lnet/minecraft/client/option/KeyBinding;Lnet/minecraft/text/Text;)V", at = @At("TAIL"), remap = false)
    private void onInit(NewKeyBindsList newKeyBindsList, KeyBinding keyBinding, Text bindingName, CallbackInfo ci) {
        this.self = (KeyEntry) (Object) this;
        this.newKeyBindsList = newKeyBindsList;

        this.addKeyBindingButton = ButtonWidget.builder(Text.of("+"), (button) -> {
            this.self.setFocused(false);
            createCustomKeyBinding();
        })
                .size(20, 20)
                .build();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
            int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {

        int scrollbarX = newKeyBindsList.getRowRight() + 6 + 2;
        int buttonX = scrollbarX - 165;
        int buttonY = y - 2;

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
