package us.kenny.mixin;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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

import us.kenny.EditVisibilityMode;
import us.kenny.HiddenBindingManager;
import us.kenny.ModifierManager;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
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
    private Button eyeButton;
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

        this.eyeButton = Button.builder(Component.literal(visibilityLabel()), (button) -> {
            this.self.setFocused(false);
            HiddenBindingManager.toggle(this.key.getName());
            this.keyBindsList.resetMappingAndUpdateButtons();
        })
                .size(60, 20)
                .build();
    }

    @Unique
    private String visibilityLabel() {
        return HiddenBindingManager.isHidden(this.key.getName()) ? "Hidden" : "Visible";
    }

    @Unique
    private Button rowActionButton() {
        return EditVisibilityMode.isActive() ? this.eyeButton : this.addKeyBindingButton;
    }

    /**
     * Render the row action button in the vanilla "+" slot. In normal mode
     * it's the add-binding "+"; in edit mode it's the Visible/Hidden text
     * toggle, and the vanilla change/reset buttons are skipped entirely.
     */
    @Inject(method = "extractContent", at = @At("TAIL"))
    private void onExtractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered,
            float deltaTicks,
            CallbackInfo ci) {
        boolean editMode = EditVisibilityMode.isActive();
        Button button = rowActionButton();

        // In edit mode the change/reset buttons don't render — place the
        // visibility toggle where the vanilla resetButton lives, so it
        // right-aligns with other rows.
        int buttonX = editMode
                ? this.resetButton.getX() + this.resetButton.getWidth() - button.getWidth()
                : this.changeButton.getX() - button.getWidth() - 5;
        int buttonY = this.getContentY() - 2;

        if (editMode) {
            this.eyeButton.setMessage(Component.literal(visibilityLabel()));
        }

        button.setPosition(buttonX, buttonY);
        button.extractRenderState(graphics, mouseX, mouseY, deltaTicks);

        // Shift the row-action button to the left if the "Ok Zoomer" settings
        // button is also rendered.
        if (key.getName().equals("key.ok_zoomer.zoom") && this.children().size() > 3) {
            buttonX -= 22;
        }
    }

    /**
     * Skip rendering the reset button in edit mode.
     */
    @WrapOperation(method = "extractContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", ordinal = 0))
    private void gateResetButton(Button button, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float delta, Operation<Void> original) {
        if (EditVisibilityMode.isActive()) {
            return;
        }
        original.call(button, graphics, mouseX, mouseY, delta);
    }

    /**
     * Skip rendering the change-key button in edit mode.
     */
    @WrapOperation(method = "extractContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", ordinal = 1))
    private void gateChangeButton(Button button, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float delta, Operation<Void> original) {
        if (EditVisibilityMode.isActive()) {
            return;
        }
        original.call(button, graphics, mouseX, mouseY, delta);
    }

    /**
     * Gray out the binding name when this row is hidden and we're in
     * edit-visibility mode.
     */
    @WrapOperation(method = "extractContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private void grayHiddenName(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color,
            Operation<Void> original) {
        int actualColor = (EditVisibilityMode.isActive()
                && HiddenBindingManager.isHidden(this.key.getName()))
                        ? 0xFF888888
                        : color;
        original.call(graphics, font, text, x, y, actualColor);
    }

    /**
     * Skip the vanilla yellow collision stripe in edit-visibility mode.
     */
    @WrapOperation(method = "extractContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
    private void gateCollisionStripe(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color,
            Operation<Void> original) {
        if (EditVisibilityMode.isActive()) {
            return;
        }
        original.call(graphics, x1, y1, x2, y2, color);
    }

    /**
     * Check vanilla key bindings against custom ones for collisions.
     * Hidden entries (per HiddenBindingManager) are skipped so they don't
     * contribute to the yellow collision indicator on other bindings.
     */
    @Inject(method = "refreshEntry", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/options/controls/KeyBindsList$KeyEntry;hasCollision:Z", ordinal = 1, opcode = Opcodes.GETFIELD))
    private void onGetHasCollision(CallbackInfo ci, @Local MutableComponent collisions) {
        if (this.key.isUnbound() || HiddenBindingManager.isHidden(this.key.getName())) {
            this.hasCollision = false;
            return;
        }
        String boundKeyName = this.key.saveString();
        List<InputConstants.Key> keyModifiers = ModifierManager.getModifiers(this.key.getName());
        for (MultiKeyBinding mkb : MultiKeyBindingManager.getKeyBindings()) {
            if (mkb.isUnbound()
                    || HiddenBindingManager.isHidden(mkb.getAction())
                    || !mkb.getKey().getName().equals(boundKeyName)
                    || !ModifierManager.modifiersEqual(keyModifiers,
                            ModifierManager.getModifiers(mkb.getId().toString()))) {
                continue;
            }
            if (this.hasCollision) {
                collisions.append(", ");
            }
            this.hasCollision = true;
            collisions.append(Component.translatable(mkb.getTranslationKey()));
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
        return ImmutableList.of(this.changeButton, this.resetButton, rowActionButton());
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return ImmutableList.of(this.changeButton, this.resetButton, rowActionButton());
    }
}