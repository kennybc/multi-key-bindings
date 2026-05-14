package us.kenny.mixin.controlling;

import com.blamejared.controlling.client.NewKeyBindsList;
import com.blamejared.controlling.client.NewKeyBindsList.KeyEntry;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

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
import us.kenny.core.controlling.ControllingHideableKeyEntry;
import us.kenny.core.controlling.ControllingMultiKeyBindingEntry;

import java.util.ArrayList;
import java.util.List;

@Mixin(KeyEntry.class)
public abstract class KeyEntryMixin extends KeyBindsList.Entry implements ControllingHideableKeyEntry {
    @Final
    @Shadow
    private KeyMapping key;
    @Final
    @Shadow
    private Button btnChangeKeyBinding;
    @Final
    @Shadow
    private Button btnResetKeyBinding;
    @Shadow
    private boolean hasCollision;

    @Unique
    private boolean hidden;
    @Unique
    private Button addKeyBindingButton;
    @Unique
    private NewKeyBindsList newKeyBindsList;

    /**
     * @see us.kenny.mixin.KeyBindsListEntryMixin#createCustomKeyBinding
     */
    @Unique
    private void createCustomKeyBinding() {
        MultiKeyBinding multiKeyBinding = MultiKeyBindingManager.addKeyBinding(
                key.getName(),
                key.getCategory(),
                InputConstants.UNKNOWN);
        MultiKeyBindingEntry multiKeyBindingEntry = new ControllingMultiKeyBindingEntry(newKeyBindsList,
                (KeyEntry) (Object) this,
                multiKeyBinding);

        List<KeyBindsList.Entry> entries = new ArrayList<>(newKeyBindsList.children());
        entries.add(newKeyBindsList.children().indexOf(this) + 1, multiKeyBindingEntry);

        newKeyBindsList.allEntries.add(newKeyBindsList.allEntries.indexOf(this) + 1, multiKeyBindingEntry);
        newKeyBindsList.clearEntries();
        for (KeyBindsList.Entry entry : entries) {
            newKeyBindsList.addEntryInternal(entry);
        }
    }

    @Unique
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * @see us.kenny.mixin.KeyBindsListEntryMixin#onInit
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(NewKeyBindsList newKeyBindsList, KeyMapping keyBinding, Component bindingName,
            CallbackInfo ci) {
        this.hidden = false;
        this.newKeyBindsList = newKeyBindsList;

        this.addKeyBindingButton = Button.builder(Component.nullToEmpty("+"), (button) -> {
            this.setFocused(false);
            createCustomKeyBinding();
        })
                .size(20, 20)
                .build();
    }

    /**
     * @see us.kenny.mixin.KeyBindsListEntryMixin#onRenderContent
     */
    @Inject(method = "renderContent", at = @At("HEAD"))
    private void onRenderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float deltaTicks,
            CallbackInfo ci) {
        int scrollbarX = newKeyBindsList.getRowRight() + 6 + 2;
        int buttonX = scrollbarX - 165;
        int buttonY = this.getContentY() - 2;

        addKeyBindingButton.setPosition(buttonX, buttonY);
        addKeyBindingButton.render(graphics, mouseX, mouseY, deltaTicks);
    }

    /**
     * Deactivates key binding reset button.
     * 
     * @see us.kenny.core.controlling.ControllingHideableKeyEntry
     */
    @WrapOperation(method = "renderContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", ordinal = 0))
    private void onResetButtonRender(Button button, GuiGraphics graphics, int mouseX, int mouseY,
            float delta,
            Operation<Void> original) {
        button.active = !this.hidden && !this.key.isDefault();

        original.call(button, graphics, mouseX, mouseY, delta);
    }

    /**
     * Deactivates key binding change key button.
     * 
     * @see us.kenny.core.controlling.ControllingHideableKeyEntry
     */
    @WrapOperation(method = "renderContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", ordinal = 1))
    private void onChangeKeyButtonRender(Button button, GuiGraphics graphics, int mouseX, int mouseY,
            float delta,
            Operation<Void> original) {
        button.active = !this.hidden;

        original.call(button, graphics, mouseX, mouseY, delta);
    }

    /**
     * @see us.kenny.mixin.KeyBindsListEntryMixin#onGetHasCollision
     */
    @Inject(method = "refreshEntry", at = @At(value = "FIELD", target = "Lcom/blamejared/controlling/client/NewKeyBindsList$KeyEntry;hasCollision:Z", ordinal = 1, opcode = Opcodes.GETFIELD))
    private void onGetHasCollision(CallbackInfo ci, @Local(ordinal = 0) MutableComponent collisions) {
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
            collisions.append(Component.translatable(mkb.getTranslationKey()));
        }
    }

    /**
     * @see us.kenny.mixin.KeyBindsListEntryMixin#onResetButtonClicked
     */
    @Inject(method = "lambda$new$2(Lnet/minecraft/client/KeyMapping;Lnet/minecraft/client/gui/components/Button;)V", at = @At("HEAD"))
    private void onResetButtonClicked(KeyMapping keyBinding, Button buttonWidget, CallbackInfo ci) {
        ModifierManager.setModifiers(keyBinding.getName(), List.of());
    }

    /**
     * @see us.kenny.mixin.KeyBindsListEntryMixin#onEditButtonClicked
     */
    @Inject(method = "lambda$new$0(Lnet/minecraft/client/KeyMapping;Lnet/minecraft/client/gui/components/Button;)V", at = @At("HEAD"))
    private void onEditButtonClicked(KeyMapping keyBinding, Button buttonWidget, CallbackInfo ci) {
        ModifierManager.setModifiers(keyBinding.getName(), List.of());
        keyBinding.setKey(InputConstants.UNKNOWN);
    }

    /**
     * @see us.kenny.mixin.KeyBindsListEntryMixin#children
     */
    @ModifyReturnValue(method = "children", at = @At("RETURN"))
    private List<? extends GuiEventListener> modifyChildren(
            List<? extends GuiEventListener> original) {
        if (this.addKeyBindingButton == null || original.contains(this.addKeyBindingButton)) {
            return original;
        }

        List<GuiEventListener> list = new ArrayList<>(original);
        list.add(this.addKeyBindingButton);
        return list;
    }

    /**
     * @see us.kenny.mixin.KeyBindsListEntryMixin#narratables
     */
    @ModifyReturnValue(method = "narratables", at = @At("RETURN"))
    private List<? extends NarratableEntry> modifyNarratables(
            List<? extends NarratableEntry> original) {
        if (this.addKeyBindingButton == null || original.contains(this.addKeyBindingButton)) {
            return original;
        }

        List<NarratableEntry> list = new ArrayList<>(original);
        list.add(this.addKeyBindingButton);
        return list;
    }
}