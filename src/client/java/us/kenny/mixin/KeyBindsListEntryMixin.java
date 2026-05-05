package us.kenny.mixin;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import us.kenny.ModifierManager;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList.KeyEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

@Mixin(KeyBindsList.KeyEntry.class)
public abstract class KeyBindsListEntryMixin extends KeyBindsList.Entry {
    @Final
    @Shadow
    private KeyMapping key;
    @Final
    @Shadow
    private Button changeButton;
    @Final
    @Shadow
    private Button resetButton;
    @Shadow
    private boolean hasCollision;

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

        List<KeyBindsList.Entry> entries = new ArrayList<>(keyBindsList.children());
        entries.add(keyBindsList.children().indexOf(this.self) + 1, multiKeyBindingEntry);
        keyBindsList.replaceEntries(entries);
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
    @Inject(method = "extractContent", at = @At("TAIL"))
    private void onExtractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered,
            float deltaTicks,
            CallbackInfo ci) {
        // Mimic the positioning and layout of the existing buttons
        int scrollbarX = this.keyBindsList.getRowRight() + 6 + 2;
        int buttonX = scrollbarX - 165; // 5 wide gap between buttons, 20 wide "+" button
        int buttonY = this.getContentY() - 2;
        // Align with the existing buttons

        this.addKeyBindingButton.setPosition(buttonX, buttonY);
        this.addKeyBindingButton.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
    }

    /**
     * Check vanilla key bindings against custom ones for collisions.
     */
    @Inject(method = "refreshEntry", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/options/controls/KeyBindsList$KeyEntry;hasCollision:Z", ordinal = 1, opcode = Opcodes.GETFIELD))
    private void onGetHasCollision(CallbackInfo ci, @Local MutableComponent collisions) {
        if (this.key.isUnbound()) {
            return;
        }
        String boundKeyName = this.key.saveString();
        List<InputConstants.Key> keyModifiers = ModifierManager.getModifiers(this.key.getName());
        for (MultiKeyBinding mkb : MultiKeyBindingManager.getKeyBindings()) {
            if (mkb.isUnbound()
                    || !mkb.getKey().getName().equals(boundKeyName)
                    || !ModifierManager.modifiersEqual(keyModifiers,
                            ModifierManager.getModifiers(mkb.getId().toString()))) {
                continue;
            }
            if (this.hasCollision) {
                collisions.append(", ");
            }
            this.hasCollision = true;
            collisions.append(Component.translatable(mkb.getAction().replaceFirst("^multi.", "")));
        }
    }

    /**
     * Clear modifiers when the reset button is clicked.
     */
    @Inject(method = "lambda$new$2(Lnet/minecraft/client/KeyMapping;Lnet/minecraft/client/gui/screens/options/controls/KeyBindsList;Lnet/minecraft/client/gui/components/Button;)V", at = @At("HEAD"))
    private static void onResetButtonClicked(KeyMapping keyBinding, KeyBindsList listWidget, Button buttonWidget,
            CallbackInfo callbackInfo) {
        ModifierManager.setModifiers(keyBinding.getName(), List.of());
    }

    /**
     * Clear a binding when the edit button is clicked to simplify modifier logic.
     */
    @Inject(method = "lambda$new$0(Lnet/minecraft/client/gui/screens/options/controls/KeyBindsList;Lnet/minecraft/client/KeyMapping;Lnet/minecraft/client/gui/components/Button;)V", at = @At("HEAD"))
    private static void onEditButtonClicked(KeyBindsList listWidget, KeyMapping keyBinding, Button buttonWidget,
            CallbackInfo callbackInfo) {
        ModifierManager.setModifiers(keyBinding.getName(), List.of());
        keyBinding.setKey(InputConstants.UNKNOWN);
    }

    /**
     * The following override hardcoded lists that enable our custom buttons to be
     * interacted with.
     */
    @Override
    public List<? extends GuiEventListener> children() {
        return ImmutableList.of(this.changeButton, this.resetButton, this.addKeyBindingButton);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return ImmutableList.of(this.changeButton, this.resetButton, this.addKeyBindingButton);
    }
}