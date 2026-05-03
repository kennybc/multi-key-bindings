package us.kenny.mixin.controlling;

import com.blamejared.controlling.client.NewKeyBindsList;
import com.blamejared.controlling.client.NewKeyBindsList.KeyEntry;
import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
     * @see us.kenny.mixin.KeyBindsListEntryMixin#onExtractContent
     */
    @Inject(method = "extractContent", at = @At("HEAD"))
    private void onExtractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered,
            float deltaTicks, CallbackInfo ci) {
        int scrollbarX = newKeyBindsList.getRowRight() + 6 + 2;
        int buttonX = scrollbarX - 165;
        int buttonY = this.getContentY() - 2;

        addKeyBindingButton.setPosition(buttonX, buttonY);
        addKeyBindingButton.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
    }

    /**
     * Deactivates key binding reset button.
     * 
     * @see us.kenny.core.controlling.ControllingHideableKeyEntry
     */
    @WrapOperation(method = "extractContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", ordinal = 0))
    private void onResetButtonExtractContent(Button button, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
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
    @WrapOperation(method = "extractContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", ordinal = 1))
    private void onChangeKeyButtonExtractContent(Button button, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
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
            collisions.append(Component.translatable(mkb.getAction().replaceFirst("^multi.", "")));
        }
    }

    /**
     * @see us.kenny.mixin.KeyBindsListEntryMixin#onResetButtonClicked
     */
    @Inject(method = "lambda$new$2(Lcom/blamejared/controlling/client/NewKeyBindsList;Lnet/minecraft/client/KeyMapping;Lnet/minecraft/client/gui/components/Button;)V", at = @At("HEAD"), remap = false)
    private static void onResetButtonClicked(NewKeyBindsList listWidget, KeyMapping keyBinding, Button buttonWidget,
            CallbackInfo callbackInfo) {
        ModifierManager.setModifiers(keyBinding.getName(), List.of());
    }

    /**
     * @see us.kenny.mixin.KeyBindsListEntryMixin#onEditButtonClicked
     */
    @Inject(method = "lambda$new$0(Lcom/blamejared/controlling/client/NewKeyBindsList;Lnet/minecraft/client/KeyMapping;Lnet/minecraft/client/gui/components/Button;)V", at = @At("HEAD"), remap = false)
    private static void onEditButtonClicked(NewKeyBindsList listWidget, KeyMapping keyBinding, Button buttonWidget,
            CallbackInfo callbackInfo) {
        ModifierManager.setModifiers(keyBinding.getName(), List.of());
        keyBinding.setKey(InputConstants.UNKNOWN);
    }

    /**
     * @see us.kenny.mixin.KeyBindsListEntryMixin#children
     */
    @Override
    public List<GuiEventListener> children() {
        return ImmutableList.of(this.btnChangeKeyBinding, this.btnResetKeyBinding, this.addKeyBindingButton);
    }

    /**
     * @see us.kenny.mixin.KeyBindsListEntryMixin#narratables
     */
    @Override
    public List<? extends NarratableEntry> narratables() {
        return ImmutableList.of(this.btnChangeKeyBinding, this.btnResetKeyBinding, this.addKeyBindingButton);
    }
}