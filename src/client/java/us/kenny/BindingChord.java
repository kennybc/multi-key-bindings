package us.kenny;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * The complete input gesture represented by a key binding.
 *
 * Minecraft stores debug actions as a primary key plus the separately
 * configurable debug modifier. Conflict checks need both parts even though the
 * primary {@code KeyMapping} only exposes the former.
 */
record BindingChord(InputConstants.Key primaryKey, Set<InputConstants.Key> modifiers) {
    BindingChord {
        LinkedHashSet<InputConstants.Key> normalized = new LinkedHashSet<>(modifiers);
        normalized.remove(primaryKey);
        modifiers = Set.copyOf(normalized);
    }

    static BindingChord of(InputConstants.Key primaryKey,
            Collection<InputConstants.Key> configuredModifiers,
            boolean debugChord,
            InputConstants.Key debugModifier) {
        LinkedHashSet<InputConstants.Key> modifiers = new LinkedHashSet<>(configuredModifiers);
        if (debugChord) {
            modifiers.add(debugModifier);
        }
        return new BindingChord(primaryKey, modifiers);
    }

    boolean conflictsWith(BindingChord other) {
        return this.primaryKey.equals(other.primaryKey) && this.modifiers.equals(other.modifiers);
    }
}
