package us.kenny;

import org.lwjgl.glfw.GLFW;

/**
 * Tracks the GLFW action of the currently in-progress KeyboardHandler.keyPress
 * call so KeyMapping click()/set() gating can distinguish a fresh PRESS from
 * GLFW auto-repeat (REPEAT) — vanilla calls both KeyMapping.click() and
 * KeyMapping.set(true) for both, which makes modifier-augmented one-click
 * actions fire continuously and toggle bindings flip rapidly when held.
 */
public final class KeyEventManager {
    private static int currentAction = -1;

    private KeyEventManager() {
    }

    public static void setCurrentAction(int action) {
        currentAction = action;
    }

    public static boolean isRepeat() {
        return currentAction == GLFW.GLFW_REPEAT;
    }
}
