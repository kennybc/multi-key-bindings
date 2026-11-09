package us.kenny.core.profile;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import us.kenny.HiddenBindingManager;

public final class KeyEntryVisibilityController {
    private final KeyMapping key;
    private final Button addButton;
    private final Button visibilityButton;

    /**
     * Create a controller bound to a specific KeyEntry.
     *
     * @param key           The bound KeyMapping this row represents.
     * @param onAdd         Fires when the "+" button is clicked; the mixin
     *                      adds a sub-binding to its list.
     * @param onListChanged Fires after toggling visibility so the list can
     *                      rebuild derived state (collision markers etc.).
     * @param onBlurEntry   Fires before both actions to clear focus on the
     *                      entry (avoids stale keyboard focus after a click).
     */
    public KeyEntryVisibilityController(KeyMapping key, Runnable onAdd, Runnable onListChanged,
            Runnable onBlurEntry) {
        this.key = key;
        this.addButton = Button.builder(Component.nullToEmpty("+"), b -> {
            onBlurEntry.run();
            onAdd.run();
        }).size(20, 20).build();
        this.visibilityButton = Button.builder(Component.literal(visibilityLabel()), b -> {
            onBlurEntry.run();
            HiddenBindingManager.toggle(key.getName());
            onListChanged.run();
        }).size(60, 20).build();
    }

    /**
     * Build the "Visible" or "Hidden" label for this row's key binding.
     */
    public String visibilityLabel() {
        return HiddenBindingManager.isHidden(key.getName()) ? "Hidden" : "Visible";
    }

    /**
     * Get the button to render in the row's action slot: the visibility
     * toggle in edit mode, or the "+" button otherwise.
     */
    public Button rowActionButton() {
        return HiddenBindingManager.isEditMode() ? visibilityButton : addButton;
    }

    /**
     * Refresh the visibility button's label from the current hidden set.
     * Call before rendering the visibility button in edit mode.
     */
    public void refreshVisibilityLabel() {
        visibilityButton.setMessage(Component.literal(visibilityLabel()));
    }
}
