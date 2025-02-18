package us.kenny;

import java.util.UUID;

public class MultiKeyBinding {
    private int keyCode;
    private final String action;
    private final UUID id;

    // For loading an existing binding from config
    public MultiKeyBinding(String action, int keyCode, UUID id) {
        this.action = action;
        this.keyCode = keyCode;
        this.id = id;
    }

    public MultiKeyBinding(String action, int keyCode) {
        this.action = action;
        this.keyCode = keyCode;
        this.id = UUID.randomUUID();
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int newKeyCode) {
        this.keyCode = newKeyCode;
    }

    public String getAction() {
        return action;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiKeyBinding that = (MultiKeyBinding) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}