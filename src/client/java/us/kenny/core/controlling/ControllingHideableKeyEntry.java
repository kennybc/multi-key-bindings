package us.kenny.core.controlling;

/**
 * A key entry is hidden when it does not pass a filter but its associated
 * bindings do (so it is treated as a read-only label).
 */
public interface ControllingHideableKeyEntry {
    void setHidden(boolean hidden);
}