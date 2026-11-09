package us.kenny.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void closeWithInventoryMouseButton(MouseButtonEvent event, boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir) {
        if (Minecraft.getInstance().options.keyInventory.matchesMouse(event)) {
            ((AbstractContainerScreen<?>) (Object) this).onClose();
            cir.setReturnValue(true);
        }
    }
}
