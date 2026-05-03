package us.kenny.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;

@Mixin(AbstractButton.class)
public abstract class AbstractButtonMixin {
    private static final int LABEL_HORIZONTAL_PADDING = 3;
    private static final float MIN_LABEL_SCALE = 0.65f;

    /**
     * When a button's message is wider than the button itself (e.g. a key bind with
     * several modifier prefixes like "Left Shift+Left Ctrl+K"), scale the label
     * down. Short labels still go through the vanilla path unchanged.
     */
    @Inject(method = "renderDefaultLabel", at = @At("HEAD"), cancellable = true)
    private void onRenderDefaultLabel(ActiveTextCollector collector, CallbackInfo ci) {
        AbstractButton self = (AbstractButton) (Object) this;
        Component message = self.getMessage();
        Font font = Minecraft.getInstance().font;
        int textWidth = font.width(message);
        int availableWidth = self.getWidth() - 2 * LABEL_HORIZONTAL_PADDING;
        if (availableWidth <= 0 || textWidth <= availableWidth) {
            return;
        }

        float idealScale = (float) availableWidth / (float) textWidth;
        float scale = Math.max(idealScale, MIN_LABEL_SCALE);
        boolean fitsAtScale = textWidth * scale <= availableWidth;

        ActiveTextCollector.Parameters originalParameters = collector.defaultParameters();
        collector.defaultParameters(originalParameters.withScale(scale));

        // withScale multiplies the (x, y) we pass by `scale`, so divide the
        // intended screen positions by scale to land where we want.
        if (fitsAtScale) {
            float screenCenterX = self.getX() + self.getWidth() / 2f;
            float screenTopY = self.getY() + (self.getHeight() - font.lineHeight * scale) / 2f;
            int x = Math.round(screenCenterX / scale);
            int y = Math.round(screenTopY / scale);
            collector.accept(TextAlignment.CENTER, x, y, message);
        } else {
            int x1 = Math.round((self.getX() + LABEL_HORIZONTAL_PADDING) / scale);
            int x2 = Math.round((self.getX() + self.getWidth() - LABEL_HORIZONTAL_PADDING) / scale);
            int y1 = Math.round(self.getY() / scale);
            int y2 = Math.round((self.getY() + self.getHeight()) / scale);
            collector.acceptScrollingWithDefaultCenter(message, x1, x2, y1, y2);
        }

        collector.defaultParameters(originalParameters);
        ci.cancel();
    }
}
