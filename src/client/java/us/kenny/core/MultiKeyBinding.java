package us.kenny.core;

import net.minecraft.client.util.InputUtil;

import java.util.UUID;

public class MultiKeyBinding {

    private final UUID id;
    private final String action;
    private InputUtil.Key key;

    private boolean pressed;
    private int timesPressed;

    public MultiKeyBinding(UUID id, String action, InputUtil.Key key) {
        this.id = id;
        this.action = action;
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