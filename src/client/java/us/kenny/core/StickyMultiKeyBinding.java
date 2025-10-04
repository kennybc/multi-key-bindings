package us.kenny.core;

import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;

import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Only used for sprint and sneak.
 */
public class StickyMultiKeyBinding extends MultiKeyBinding {
    private final BooleanSupplier toggleGetter;

    public StickyMultiKeyBinding(UUID id, String action, Category category, InputUtil.Key key,
            BooleanSupplier toggleGetter) {
        super(id, action, category, key);
        this.toggleGetter = toggleGetter;
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

    public void untoggle() {
        super.setPressed(false);
    }
}
