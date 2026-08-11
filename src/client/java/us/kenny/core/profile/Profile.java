package us.kenny.core.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * In-memory representation of a single profile. Holds the raw JSON blobs
 * for bindings and modifiers plus the vanilla-mapping snapshot. File
 * paths are owned by ProfileManager, not Profile.
 */
public final class Profile {
    private final String name;
    private JsonArray bindings;
    private JsonObject modifiers;
    private JsonObject vanilla;

    public Profile(String name) {
        this.name = name;
        this.bindings = new JsonArray();
        this.modifiers = new JsonObject();
        this.vanilla = new JsonObject();
    }

    public String getName() {
        return name;
    }

    public JsonArray getBindings() {
        return bindings;
    }

    public void setBindings(JsonArray bindings) {
        this.bindings = bindings;
    }

    public JsonObject getModifiers() {
        return modifiers;
    }

    public void setModifiers(JsonObject modifiers) {
        this.modifiers = modifiers;
    }

    public JsonObject getVanilla() {
        return vanilla;
    }

    public void setVanilla(JsonObject vanilla) {
        this.vanilla = vanilla;
    }
}
