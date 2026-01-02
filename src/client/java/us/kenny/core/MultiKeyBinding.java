package us.kenny.core;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.UUID;

/**
 * Need to create this class to mimic a KeyBinding because the native KeyBinding
 * constructor will overwrite existing keys bound to that action.
 */
public class MultiKeyBinding {

    private final UUID id;
    private final String action;
    private String category;
    private InputConstants.Key key;

    private boolean pressed;
    private int timesPressed;

    public MultiKeyBinding(UUID id, String action, String category, InputConstants.Key key) {
        this.id = id;
        this.action = action;
        this.category = category;
        this.key = key;
    }

    public void release() {
        this.timesPressed = 0;
        this.setPressed(false);
    }

    public boolean shouldSetOnIngameFocus() {
      return this.key.getType() == InputConstants.Type.KEYSYM && this.key.getValue() != InputConstants.UNKNOWN.getValue();
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

    public InputConstants.Key getKey() {
        return this.key;
    }

    public void setKey(InputConstants.Key key) {
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