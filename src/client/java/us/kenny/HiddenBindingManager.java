package us.kenny;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * In-memory reflection of the active profile's hidden set. Mutations
 * fire a callback (set by ProfileManager on load) so changes persist
 * back to the active profile file.
 */
public final class HiddenBindingManager {
    private static Set<String> hidden = new LinkedHashSet<>();
    private static Runnable onMutated = () -> {
    };

    private HiddenBindingManager() {
    }

    /**
     * @param translationKey The KeyMapping translation key to check.
     */
    public static boolean isHidden(String translationKey) {
        return hidden.contains(translationKey);
    }

    public static Set<String> getAll() {
        return Collections.unmodifiableSet(hidden);
    }

    public static int size() {
        return hidden.size();
    }

    /**
     * Replace the current hidden set. Does not fire the mutation callback.
     *
     * @param next The full set to install.
     */
    public static void setAll(Set<String> next) {
        hidden = new LinkedHashSet<>(next);
    }

    /**
     * Register a callback that fires on each hide/unhide mutation. Null
     * clears the callback.
     *
     * @param callback The callback to install.
     */
    public static void setOnMutated(Runnable callback) {
        onMutated = callback == null ? () -> {
        } : callback;
    }

    public static void hide(String translationKey) {
        if (hidden.add(translationKey)) {
            onMutated.run();
        }
    }

    public static void unhide(String translationKey) {
        if (hidden.remove(translationKey)) {
            onMutated.run();
        }
    }

    public static void toggle(String translationKey) {
        if (isHidden(translationKey)) {
            unhide(translationKey);
        } else {
            hide(translationKey);
        }
    }
}
