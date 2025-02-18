package us.kenny;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

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
    private static final Map<String, List<MultiKeyBinding>> MULTI_KEY_BINDINGS = new HashMap<>();

    static {
        load();
    }

    public static UUID addKeyBinding(String action, int keyCode) {
        MultiKeyBinding multiKeyBinding = new MultiKeyBinding(action, keyCode);
        MULTI_KEY_BINDINGS.computeIfAbsent(action, k -> new ArrayList<>()).add(multiKeyBinding);
        save();
        return multiKeyBinding.getId();
    }

    public static List<MultiKeyBinding> getKeyBindings(String action) {
        return MULTI_KEY_BINDINGS.getOrDefault(action, new ArrayList<>());
    }

    public static void removeKeyBinding(String action, UUID multiKeyBindingId) {
        List<MultiKeyBinding> keyBindings = MULTI_KEY_BINDINGS.get(action);
        if (keyBindings != null) {
            keyBindings.removeIf(multiKeyBinding -> multiKeyBindingId.equals(multiKeyBinding.getId()));
            if (keyBindings.isEmpty()) {
                MULTI_KEY_BINDINGS.remove(action);
            }
            save();
        }
    }

    private static void save() {
        try {
            // Store key bindings as JSON in config file
            Map<String, List<SerializableMultiKeyBinding>> serialized = new HashMap<>();
            for (Map.Entry<String, List<MultiKeyBinding>> entry : MULTI_KEY_BINDINGS.entrySet()) {
                List<SerializableMultiKeyBinding> serializedBindings = entry.getValue().stream()
                        .map(SerializableMultiKeyBinding::new)
                        .collect(Collectors.toList());
                serialized.put(entry.getKey(), serializedBindings);
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
                Type type = new TypeToken<Map<String, List<SerializableMultiKeyBinding>>>() {
                }.getType();
                Map<String, List<SerializableMultiKeyBinding>> serialized = GSON.fromJson(content, type);

                // Deserialize the multi key bindings
                for (Map.Entry<String, List<SerializableMultiKeyBinding>> entry : serialized.entrySet()) {
                    List<MultiKeyBinding> deserializedBindings = entry.getValue().stream()
                            .map(SerializableMultiKeyBinding::toMultiKeyBinding)
                            .collect(Collectors.toList());
                    MULTI_KEY_BINDINGS.put(entry.getKey(), deserializedBindings);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Helper class for serialization
    private static class SerializableMultiKeyBinding {
        private final String action;
        private final int keyCode;
        private final UUID id;

        public SerializableMultiKeyBinding(MultiKeyBinding multiKeyBinding) {
            this.action = multiKeyBinding.getAction();
            this.keyCode = multiKeyBinding.getKeyCode();
            this.id = multiKeyBinding.getId();
        }

        public MultiKeyBinding toMultiKeyBinding() {
            return new MultiKeyBinding(action, keyCode, id);
        }
    }
}