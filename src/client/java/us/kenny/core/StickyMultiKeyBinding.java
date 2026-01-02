package us.kenny.core;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Only used for sprint and sneak.
 */
public class StickyMultiKeyBinding extends MultiKeyBinding {
    private final BooleanSupplier toggleGetter;
    private boolean releasedByScreenWhenDown; // Toggle was released by opening a screen
    private final boolean shouldRestore; // Should toggle be restored after ^ screen is closed?

    public StickyMultiKeyBinding(UUID id, String action, String category, InputConstants.Key key,
            BooleanSupplier toggleGetter, boolean shouldRestore) {
        super(id, action, category, key);
        this.toggleGetter = toggleGetter;
        this.shouldRestore = shouldRestore;
    }

    @Override
    public void setPressed(boolean pressed) {
        if (this.toggleGetter.getAsBoolean()) {
            if (pressed) {
                super.setPressed(!this.getPressed());
            }
        } else {
            super.setPressed(pressed);
        }
    }

    @Override
    public void release() {
         if (this.toggleGetter.getAsBoolean() && this.getPressed() || this.releasedByScreenWhenDown) {
         this.releasedByScreenWhenDown = true;
      }

      this.untoggle();
    }

    public boolean shouldSetOnIngameFocus() {
      return super.shouldSetOnIngameFocus() && !this.toggleGetter.getAsBoolean();
   }

    public boolean shouldRestoreStateOnScreenClosed() {
      boolean bl = this.shouldRestore && this.toggleGetter.getAsBoolean() && this.getKey().getType() == InputConstants.Type.KEYSYM && this.releasedByScreenWhenDown;
      this.releasedByScreenWhenDown = false;
      return bl;
   }

    public void untoggle() {
        super.setPressed(false);
    }
}
