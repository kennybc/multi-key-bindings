package us.kenny.core;

import net.minecraft.client.util.InputUtil;

import java.util.UUID;
import java.util.function.BooleanSupplier;

public class StickyMultiKeyBinding extends MultiKeyBinding {
    private final BooleanSupplier toggleGetter;

    public StickyMultiKeyBinding(UUID id, String action, String category, InputUtil.Key key,
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
