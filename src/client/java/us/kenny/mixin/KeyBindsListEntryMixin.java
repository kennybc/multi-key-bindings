package us.kenny.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.InputConstants;
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
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList.KeyEntry;
import net.minecraft.network.chat.Component;

@Mixin(KeyBindsList.KeyEntry.class)
public abstract class KeyBindsListEntryMixin extends KeyBindsList.Entry {
    @Final
    @Shadow
    private KeyMapping key;

    @Unique
    private Button addKeyBindingButton;
    @Unique
    private KeyBindsList keyBindsList;
    @Unique
    private KeyEntry self;

    /**
     * Register the new binding in our manager and create an entry in the key bind
     * list for it.
     */
    @Unique
    private void createCustomKeyBinding() {
        MultiKeyBinding multiKeyBinding = MultiKeyBindingManager.addKeyBinding(
                key.getName(),
                key.getCategory(),
                InputConstants.UNKNOWN);
        MultiKeyBindingEntry multiKeyBindingEntry = new MultiKeyBindingEntry(keyBindsList, multiKeyBinding);
        keyBindsList.children().add(keyBindsList.children().indexOf(this.self) + 1, multiKeyBindingEntry);
    }

    /**
     * Builds our custom "+" button in native key binding entries.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(KeyBindsList keyBindsList, final KeyMapping keyBinding, final Component bindingName,
            CallbackInfo ci) {
        this.self = (KeyEntry) (Object) this;
        this.keyBindsList = keyBindsList;

        this.addKeyBindingButton = Button.builder(Component.nullToEmpty("+"), (button) -> {
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
     private void onRender(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
            int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        // Mimic the positioning and layout of the existing buttons
        int scrollbarX = keyBindsList.getRowRight() + 6 + 2;
        int buttonX = scrollbarX - 165; // 5 wide gap between buttons, 20 wide "+" button
        int buttonY = y - 2; // Align with the existing buttons

        addKeyBindingButton.setPosition(buttonX, buttonY);
        addKeyBindingButton.render(graphics, mouseX, mouseY, tickDelta);
    }


    /**
     * The following override hardcoded lists that enable our custom buttons to be
     * interacted with.
     */
    @ModifyReturnValue(method = "children", at = @At("RETURN"))
    private List<? extends GuiEventListener> modifyChildren(
            List<? extends GuiEventListener> original
    ) {
        if (this.addKeyBindingButton == null || original.contains(this.addKeyBindingButton)) {
            return original;
        }

        List<GuiEventListener> list = new ArrayList<>(original);
        list.add(this.addKeyBindingButton);
        return list;
    }

    @ModifyReturnValue(method = "narratables", at = @At("RETURN"))
    private List<? extends NarratableEntry> modifyNarratables(
            List<? extends NarratableEntry> original
    ) {
        if (this.addKeyBindingButton == null || original.contains(this.addKeyBindingButton)) {
            return original;
        }

        List<NarratableEntry> list = new ArrayList<>(original);
        list.add(this.addKeyBindingButton);
        return list;
    }
}