package us.kenny.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin {
    private static final int LABEL_HORIZONTAL_PADDING = 3;
    private static final float MIN_LABEL_SCALE = 0.65f;

    /**
     * When a button's message is wider than the button itself (e.g. a key bind with
     * several modifier prefixes like "Left Shift+Left Ctrl+K"), scale the label
     * down. Short labels still go through the vanilla path unchanged.
     */
    @Inject(method = "renderString", at = @At("HEAD"), cancellable = true)
    private void onRenderString(GuiGraphics graphics, Font font, int color, CallbackInfo ci) {
        AbstractButton self = (AbstractButton) (Object) this;
        Component message = self.getMessage();
        int textWidth = font.width(message);
        int availableWidth = self.getWidth() - 2 * LABEL_HORIZONTAL_PADDING;
        if (availableWidth <= 0 || textWidth <= availableWidth) {
            return;
        }

        float idealScale = (float) availableWidth / (float) textWidth;
        float scale = Math.max(idealScale, MIN_LABEL_SCALE);
        boolean fitsAtScale = textWidth * scale <= availableWidth;

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);

        // The pose multiplies the (x, y) we pass by `scale`, so divide the
        // intended screen positions by scale to land where we want.
        int left = Math.round((self.getX() + LABEL_HORIZONTAL_PADDING) / scale);
        int right = Math.round((self.getX() + self.getWidth() - LABEL_HORIZONTAL_PADDING) / scale);
        int top = Math.round(self.getY() / scale);
        int bottom = Math.round((self.getY() + self.getHeight()) / scale);
        int textY = (top + bottom - font.lineHeight) / 2 + 1;

        if (fitsAtScale) {
            int centerX = (left + right) / 2;
            graphics.drawCenteredString(font, message, centerX, textY, color);
        } else {
            int diff = textWidth - (right - left);
            double time = Util.getMillis() / 1000.0;
            double period = Math.max(diff * 0.5, 3.0);
            double phase = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * time / period)) / 2.0 + 0.5;
            double offset = Mth.lerp(phase, 0.0, diff);
            graphics.enableScissor(left, top, right, bottom);
            graphics.drawString(font, message, left - (int) offset, textY, color);
            graphics.disableScissor();
        }

        graphics.pose().popMatrix();
        ci.cancel();
    }
}
