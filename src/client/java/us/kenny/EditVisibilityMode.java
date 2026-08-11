package us.kenny;

/**
 * Global flag: is the Key Binds screen in edit-visibility mode? Not
 * persisted; always starts false when the screen opens.
 */
public final class EditVisibilityMode {
    private static boolean active = false;

    private EditVisibilityMode() {
    }

    public static boolean isActive() {
        return active;
    }

    /**
     * @param value The new active state.
     */
    public static void setActive(boolean value) {
        active = value;
    }

    public static void toggle() {
        active = !active;
    }
}
