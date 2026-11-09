package us.kenny.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
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

import us.kenny.HiddenBindingManager;
import us.kenny.ModifierManager;
import us.kenny.MultiKeyBindingManager;
import us.kenny.core.MultiKeyBinding;
import us.kenny.core.MultiKeyBindingEntry;
import us.kenny.core.profile.KeyEntryVisibilityController;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
    private KeyEntryVisibilityController visibilityController;
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
        this.visibilityController = new KeyEntryVisibilityController(
                keyBinding,
                this::createCustomKeyBinding,
                keyBindsList::resetMappingAndUpdateButtons,
                () -> this.self.setFocused(false));
    }

    /**
     * Render the row action button in the vanilla "+" slot. In normal mode
     * it's the add-binding "+"; in edit mode it's the Visible/Hidden text
     * toggle, and the vanilla change/reset buttons are skipped entirely.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
            int mouseY, boolean hovered, float deltaTicks, CallbackInfo ci) {
        boolean editMode = HiddenBindingManager.isEditMode();
        Button button = this.visibilityController.rowActionButton();

        // In edit mode the change/reset buttons don't render — place the
        // visibility toggle where the vanilla resetButton lives, so it
        // right-aligns with other rows.
        int buttonX;
        if (editMode) {
            buttonX = this.resetButton.getX() + this.resetButton.getWidth() - button.getWidth();
            this.visibilityController.refreshVisibilityLabel();
        } else {
            // Mimic the positioning and layout of the existing buttons
            buttonX = this.changeButton.getX() - button.getWidth() - 5;

            // Shift "+" button to the left if the "Ok Zoomer" settings button is also
            // rendered
            if (key.getName().equals("key.ok_zoomer.zoom") && this.children().size() > 3) {
                buttonX -= 22;
            }
        }
        int buttonY = y - 2; // Align with the existing buttons

        button.setPosition(buttonX, buttonY);
        button.render(graphics, mouseX, mouseY, deltaTicks);
    }

    /**
     * Skip rendering the reset button in edit mode.
     */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", ordinal = 0))
    private void gateResetButton(Button button, GuiGraphics graphics, int mouseX, int mouseY,
            float delta, Operation<Void> original) {
        if (HiddenBindingManager.isEditMode()) {
            return;
        }
        original.call(button, graphics, mouseX, mouseY, delta);
    }

    /**
     * Skip rendering the change-key button in edit mode.
     */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", ordinal = 1))
    private void gateChangeButton(Button button, GuiGraphics graphics, int mouseX, int mouseY,
            float delta, Operation<Void> original) {
        if (HiddenBindingManager.isEditMode()) {
            return;
        }
        original.call(button, graphics, mouseX, mouseY, delta);
    }

    /**
     * Gray out the binding name when this row is hidden and we're in
     * edit-visibility mode.
     */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I", ordinal = 0))
    private int grayHiddenName(GuiGraphics graphics, Font font, Component text, int x, int y, int color,
            Operation<Integer> original) {
        int actualColor = (HiddenBindingManager.isEditMode()
                && HiddenBindingManager.isHidden(this.key.getName()))
                        ? 0xFF888888
                        : color;
        return original.call(graphics, font, text, x, y, actualColor);
    }

    /**
     * Skip the vanilla yellow collision stripe in edit-visibility mode.
     */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private void gateCollisionStripe(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color,
            Operation<Void> original) {
        if (HiddenBindingManager.isEditMode()) {
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
        for (MultiKeyBinding mkb : MultiKeyBindingManager.getKeyBindings()) {
            if (mkb.isUnbound()
                    || HiddenBindingManager.isActionHidden(mkb.getAction())
                    || !ModifierManager.bindingsConflict(this.key, mkb)) {
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
    @Inject(method = "method_19870", at = @At("HEAD"))
    private void onResetButtonClicked(KeyMapping keyMapping, Button button, CallbackInfo ci) {
        ModifierManager.setModifiers(keyMapping.getName(), List.of());
    }

    /**
     * Clear a binding when the edit button is clicked to simplify modifier logic.
     */
    @Inject(method = "method_19871", at = @At("HEAD"))
    private void onEditButtonClicked(KeyMapping keyMapping, Button button, CallbackInfo ci) {
        ModifierManager.setModifiers(keyMapping.getName(), List.of());
        keyMapping.setKey(InputConstants.UNKNOWN);
    }

    /**
     * The following override hardcoded lists that enable our custom buttons to be
     * interacted with.
     */
    @ModifyReturnValue(method = "children", at = @At("RETURN"))
    private List<? extends GuiEventListener> modifyChildren(
            List<? extends GuiEventListener> original) {
        if (this.visibilityController == null) {
            return original;
        }
        Button action = this.visibilityController.rowActionButton();
        if (HiddenBindingManager.isEditMode()) {
            return List.of(action);
        }
        if (original.contains(action)) {
            return original;
        }

        List<GuiEventListener> list = new ArrayList<>(original);
        list.add(action);
        return list;
    }

    @ModifyReturnValue(method = "narratables", at = @At("RETURN"))
    private List<? extends NarratableEntry> modifyNarratables(
            List<? extends NarratableEntry> original) {
        if (this.visibilityController == null) {
            return original;
        }
        Button action = this.visibilityController.rowActionButton();
        if (HiddenBindingManager.isEditMode()) {
            return List.of(action);
        }
        if (original.contains(action)) {
            return original;
        }

        List<NarratableEntry> list = new ArrayList<>(original);
        list.add(action);
        return list;
    }
}