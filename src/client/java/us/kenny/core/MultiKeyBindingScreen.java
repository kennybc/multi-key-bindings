package us.kenny.core;

/**
 * This is for tracking the selected custom key binding so we can modify it
 * through the options screen.
 */
public interface MultiKeyBindingScreen {
    void setSelectedMultiKeyBinding(MultiKeyBinding binding);

    MultiKeyBinding getSelectedMultiKeyBinding();
}
