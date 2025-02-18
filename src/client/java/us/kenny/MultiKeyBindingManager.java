package us.kenny;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static us.kenny.MultiKeyBindings.LOGGER;

public class MultiKeyBindingManager {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("multi-key-bindings.json");
    private static final Gson GSON = new Gson();
    private static final Map<String, Map<UUID, MultiKeyBinding>> MULTI_KEY_BINDINGS = new HashMap<>();

    static {
        load();
    }

    public static UUID addKeyBinding(String action, int keyCode) {
        MultiKeyBinding multiKeyBinding = new MultiKeyBinding(action, keyCode);
        MULTI_KEY_BINDINGS.computeIfAbsent(action, k -> new HashMap<>()).put(multiKeyBinding.getId(), multiKeyBinding);
        save();
        return multiKeyBinding.getId();
    }

    public static Collection<MultiKeyBinding> getKeyBindings(String action) {
        return MULTI_KEY_BINDINGS.getOrDefault(action, new HashMap<>()).values();
    }

    public static void setKeyBinding(String action, UUID multiKeyBindingId, int newKeyCode) {
        Map<UUID, MultiKeyBinding> keyBindings = MULTI_KEY_BINDINGS.get(action);
        MultiKeyBinding binding = keyBindings.get(multiKeyBindingId);
        if (binding != null) {
            binding.setKeyCode(newKeyCode);
            save();
        }
    }

    public static void removeKeyBinding(String action, UUID multiKeyBindingId) {
        Map<UUID, MultiKeyBinding> keyBindings = MULTI_KEY_BINDINGS.get(action);
        if (keyBindings != null) {
            keyBindings.remove(multiKeyBindingId);
            if (keyBindings.isEmpty()) {
                MULTI_KEY_BINDINGS.remove(action);
            }
            save();
        }
    }

    private static void save() {
        try {
            // Structure: Map<Action, Map<UUID, MultiKeyBinding>>
            Map<String, Map<String, SerializableMultiKeyBinding>> serialized = new HashMap<>();

            for (Map.Entry<String, Map<UUID, MultiKeyBinding>> entry : MULTI_KEY_BINDINGS.entrySet()) {
                String action = entry.getKey();
                Map<UUID, MultiKeyBinding> innerMap = entry.getValue();

                // Convert inner map to a map of UUID strings to SerializableMultiKeyBinding
                Map<String, SerializableMultiKeyBinding> serializedInner = new HashMap<>();
                for (Map.Entry<UUID, MultiKeyBinding> innerEntry : innerMap.entrySet()) {
                    UUID id = innerEntry.getKey();
                    MultiKeyBinding binding = innerEntry.getValue();
                    serializedInner.put(id.toString(), new SerializableMultiKeyBinding(binding));
                }

                serialized.put(action, serializedInner);
            }

            Files.writeString(CONFIG_PATH, GSON.toJson(serialized));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String content = Files.readString(CONFIG_PATH);
                Type type = new TypeToken<Map<String, Map<UUID, SerializableMultiKeyBinding>>>() {
                }.getType();
                Map<String, Map<UUID, SerializableMultiKeyBinding>> serialized = GSON.fromJson(content, type);

                // Clear existing bindings to avoid duplicates
                MULTI_KEY_BINDINGS.clear();

                for (Map.Entry<String, Map<UUID, SerializableMultiKeyBinding>> entry : serialized.entrySet()) {
                    String action = entry.getKey();
                    Map<UUID, MultiKeyBinding> innerMap = getInnerMultiKeyBindingMap(entry, action);

                    MULTI_KEY_BINDINGS.put(action, innerMap);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load config file: ", e);
            }
        }
    }

    private static @NotNull Map<UUID, MultiKeyBinding> getInnerMultiKeyBindingMap(Map.Entry<String, Map<UUID, SerializableMultiKeyBinding>> entry, String action) {
        Map<UUID, SerializableMultiKeyBinding> innerSerialized = entry.getValue();

        // Convert inner map to UUID → MultiKeyBinding
        Map<UUID, MultiKeyBinding> innerMap = new HashMap<>();
        for (Map.Entry<UUID, SerializableMultiKeyBinding> innerEntry : innerSerialized.entrySet()) {
            UUID id = innerEntry.getKey();
            MultiKeyBinding binding = innerEntry.getValue().toMultiKeyBinding(action, id);
            innerMap.put(id, binding);
        }
        return innerMap;
    }

    private static class SerializableMultiKeyBinding {
        private final int keyCode;

        public SerializableMultiKeyBinding(MultiKeyBinding multiKeyBinding) {
            this.keyCode = multiKeyBinding.getKeyCode(); // Only need to serialize keyCode
        }

        public MultiKeyBinding toMultiKeyBinding(String action, UUID multiKeyBindingId) {
            return new MultiKeyBinding(action, keyCode, multiKeyBindingId);
        }
    }
}