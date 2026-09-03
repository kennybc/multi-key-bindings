package us.kenny;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class HiddenBindingManager {
    private static Set<String> hidden = new LinkedHashSet<>();
    private static boolean editMode = false;

    private HiddenBindingManager() {
    }

    /**
     * Check whether a key binding is hidden.
     *
     * @param translationKey The KeyMapping translation key to check.
     */
    public static boolean isHidden(String translationKey) {
        return hidden.contains(translationKey);
    }

    /**
     * Get the current hidden set as an unmodifiable view.
     */
    public static Set<String> getAll() {
        return Collections.unmodifiableSet(hidden);
    }

    /**
     * Get the number of hidden key bindings.
     */
    public static int size() {
        return hidden.size();
    }

    /**
     * Replace the current hidden set. Does not trigger a persist.
     *
     * @param next The full set to install.
     */
    public static void setAll(Set<String> next) {
        hidden = new LinkedHashSet<>(next);
    }

    /**
     * Hide a key binding and persist the change.
     *
     * @param translationKey The KeyMapping translation key to hide.
     */
    public static void hide(String translationKey) {
        if (hidden.add(translationKey)) {
            ProfileManager.persistHiddenSetToIndex();
        }
    }

    /**
     * Unhide a key binding and persist the change.
     *
     * @param translationKey The KeyMapping translation key to unhide.
     */
    public static void unhide(String translationKey) {
        if (hidden.remove(translationKey)) {
            ProfileManager.persistHiddenSetToIndex();
        }
    }

    /**
     * Toggle whether a key binding is hidden.
     *
     * @param translationKey The KeyMapping translation key to toggle.
     */
    public static void toggle(String translationKey) {
        if (isHidden(translationKey)) {
            unhide(translationKey);
        } else {
            hide(translationKey);
        }
    }

    /**
     * Check whether the Key Binds screen is in edit-visibility mode.
     */
    public static boolean isEditMode() {
        return editMode;
    }

    /**
     * Set the edit-visibility mode. Transient; not persisted.
     *
     * @param value The new edit-mode state.
     */
    public static void setEditMode(boolean value) {
        editMode = value;
    }

    /**
     * Flip the edit-visibility mode.
     */
    public static void toggleEditMode() {
        editMode = !editMode;
    }
}
