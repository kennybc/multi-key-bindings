package us.kenny.core;

import net.minecraft.client.KeyMapping;

/**
 * This is for tracking the selected custom key binding so we can modify it
 * through the options screen.
 */
public interface MultiKeyBindingScreen {
    void setSelectedKey(KeyMapping keyMapping);

    void setSelectedMultiKeyBinding(MultiKeyBinding multiKeyBinding);

    void setLastKeySelection(long time);

    KeyMapping getSelectedKey();

    MultiKeyBinding getSelectedMultiKeyBinding();
}
