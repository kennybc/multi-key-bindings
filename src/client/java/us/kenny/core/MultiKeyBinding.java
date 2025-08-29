package us.kenny.core;

import net.minecraft.client.util.InputUtil;

import java.util.UUID;

/**
 * Need to create this class to mimic a KeyBinding because the native KeyBinding
 * constructor will overwrite existing keys bound to that action.
 */
public class MultiKeyBinding {

    private final UUID id;
    private final String action;
    private String category;
    private InputUtil.Key key;

    private boolean pressed;
    private int timesPressed;

    public MultiKeyBinding(UUID id, String action, String category, InputUtil.Key key) {
        this.id = id;
        this.action = action;
        this.category = category;
        this.key = key;
    }

    public void reset() {
        this.timesPressed = 0;
        this.setPressed(false);
    }

    public UUID getId() {
        return this.id;
    }

    public String getAction() {
        return this.action;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public InputUtil.Key getKey() {
        return this.key;
    }

    public void setKey(InputUtil.Key key) {
        this.key = key;
    }

    public int getTimesPressed() {
        return this.timesPressed;
    }

    public void incrementTimesPressed() {
        this.timesPressed++;
    }

    public void decrementTimesPressed() {
        this.timesPressed--;
    }

    public boolean getPressed() {
        return this.pressed;
    }

    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }
}