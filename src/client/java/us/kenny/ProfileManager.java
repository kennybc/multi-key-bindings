package us.kenny;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.loader.api.FabricLoader;
import us.kenny.core.profile.Profile;
import us.kenny.core.profile.ProfileNameValidator;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Owns the on-disk multi-key-bindings/ directory. Tracks the active
 * profile and exposes list/load/save/create/rename/duplicate/delete.
 */
public final class ProfileManager {
    private static final int CONFIG_VERSION = 4;
    private static final String DEFAULT_PROFILE = "default";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path ROOT = FabricLoader.getInstance().getConfigDir()
            .resolve("multi-key-bindings");
    private static final Path INDEX = ROOT.resolve("config.json");
    private static final Path PROFILES_DIR = ROOT.resolve("profiles");

    private static String activeProfileName = DEFAULT_PROFILE;
    private static boolean isLoading = false;

    private ProfileManager() {
    }

    public static Path getRoot() {
        return ROOT;
    }

    public static Path getProfilesDir() {
        return PROFILES_DIR;
    }

    public static Path getProfilePath(String name) {
        return PROFILES_DIR.resolve(name + ".json");
    }

    public static String getActiveProfileName() {
        return activeProfileName;
    }

    public static boolean isLoading() {
        return isLoading;
    }

    static void setLoading(boolean loading) {
        isLoading = loading;
    }

    /**
     * List all profile names, sorted case-insensitively.
     */
    public static List<String> listProfileNames() {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(PROFILES_DIR)) {
            return names;
        }
        try (Stream<Path> stream = Files.list(PROFILES_DIR)) {
            stream
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .map(p -> {
                        String n = p.getFileName().toString();
                        return n.substring(0, n.length() - ".json".length());
                    })
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(names::add);
        } catch (IOException e) {
            MultiKeyBindingClient.LOGGER.error("Failed to list profiles", e);
        }
        return names;
    }

    /**
     * Read the active_profile field from the index file, falling back to
     * "default" if the file is missing or malformed.
     */
    static String readActiveFromIndex() {
        JsonObject json = readIndex();
        if (json != null && json.has("active_profile")) {
            return json.get("active_profile").getAsString();
        }
        return DEFAULT_PROFILE;
    }

    /**
     * Read the global hidden set from the index file. Returns an empty
     * set if the field is missing.
     */
    static Set<String> readHiddenFromIndex() {
        Set<String> hidden = new LinkedHashSet<>();
        JsonObject json = readIndex();
        if (json != null && json.has("hidden")) {
            json.getAsJsonArray("hidden").forEach(el -> hidden.add(el.getAsString()));
        }
        return hidden;
    }

    private static JsonObject readIndex() {
        if (!Files.exists(INDEX)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(INDEX)) {
            return GSON.fromJson(reader, JsonObject.class);
        } catch (IOException | RuntimeException e) {
            MultiKeyBindingClient.LOGGER.error("Failed to read config.json", e);
            return null;
        }
    }

    /**
     * Atomic write of the index file. Persists the active profile plus
     * the current in-memory global hidden set.
     */
    static void writeIndex(String activeProfile) {
        JsonObject json = new JsonObject();
        json.addProperty("config_version", CONFIG_VERSION);
        json.addProperty("active_profile", activeProfile);
        JsonArray hidden = new JsonArray();
        HiddenBindingManager.getAll().forEach(hidden::add);
        json.add("hidden", hidden);
        atomicWrite(INDEX, GSON.toJson(json));
    }

    static void setActiveProfileName(String name) {
        activeProfileName = name;
    }

    static void ensureDirectories() {
        try {
            Files.createDirectories(PROFILES_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create multi-key-bindings config directory", e);
        }
    }

    /**
     * Write content to path via a temp file + atomic move. Prevents
     * partially-written files on crash.
     *
     * @param path    The destination path.
     * @param content The content to write.
     */
    static void atomicWrite(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp)) {
                writer.write(content);
            }
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailed) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            MultiKeyBindingClient.LOGGER.error("Failed to write {}", path, e);
        }
    }

    /**
     * Read a profile file. Returns null if not found.
     *
     * @param name The profile to read.
     */
    public static Profile readProfile(String name) {
        Path path = getProfilePath(name);
        if (!Files.exists(path)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) {
                return null;
            }
            Profile profile = new Profile(name);
            if (json.has("bindings")) {
                profile.setBindings(json.getAsJsonArray("bindings"));
            }
            if (json.has("modifiers")) {
                profile.setModifiers(json.getAsJsonObject("modifiers"));
            }
            if (json.has("vanilla")) {
                profile.setVanilla(json.getAsJsonObject("vanilla"));
            }
            return profile;
        } catch (IOException | RuntimeException e) {
            MultiKeyBindingClient.LOGGER.error("Failed to read profile {}", name, e);
            return null;
        }
    }

    /**
     * Write a profile to disk atomically.
     *
     * @param profile The profile to write.
     */
    public static void writeProfile(Profile profile) {
        JsonObject json = new JsonObject();
        json.add("bindings", profile.getBindings());
        json.add("modifiers", profile.getModifiers());
        json.add("vanilla", profile.getVanilla());
        atomicWrite(getProfilePath(profile.getName()), GSON.toJson(json));
    }

    /**
     * Build a Profile from the current in-memory state (every registered
     * KeyMapping, all MKB bindings and modifiers). Hidden is left empty.
     *
     * @param name The name to give the new profile.
     */
    public static Profile snapshotCurrent(String name) {
        Profile profile = new Profile(name);
        profile.setBindings(ConfigManager.getFormattedKeyBindings());
        profile.setModifiers(ConfigManager.getFormattedModifiers());
        profile.setVanilla(snapshotVanilla());
        return profile;
    }

    /**
     * Snapshot every registered KeyMapping's current key as translationKey
     * -> keyName.
     */
    private static JsonObject snapshotVanilla() {
        JsonObject vanilla = new JsonObject();
        net.minecraft.client.Options options = MultiKeyBindingManager.getGameOptions();
        if (options == null) {
            return vanilla;
        }
        for (net.minecraft.client.KeyMapping mapping : options.keyMappings) {
            vanilla.addProperty(mapping.getName(), mapping.saveString());
        }
        return vanilla;
    }

    /**
     * Build an empty Profile with the vanilla map seeded from each
     * KeyMapping's default key. No MKB bindings, modifiers, or hidden entries.
     *
     * @param name The name to give the new profile.
     */
    public static Profile snapshotDefaults(String name) {
        Profile profile = new Profile(name);
        JsonObject vanilla = new JsonObject();
        net.minecraft.client.Options options = MultiKeyBindingManager.getGameOptions();
        if (options != null) {
            for (net.minecraft.client.KeyMapping mapping : options.keyMappings) {
                vanilla.addProperty(mapping.getName(), mapping.getDefaultKey().getName());
            }
        }
        profile.setVanilla(vanilla);
        return profile;
    }

    /**
     * Client-init entry point. Migrates any legacy config, hydrates the
     * global hidden set, then loads the active profile.
     */
    public static void bootstrap() {
        ensureDirectories();
        migrateLegacyIfPresent();

        // Hydrate global hidden set BEFORE loading a profile so the first
        // list build sees the filter applied.
        HiddenBindingManager.setAll(readHiddenFromIndex());

        if (listProfileNames().isEmpty()) {
            // Fresh install: seed a default from current state.
            writeProfile(snapshotCurrent(DEFAULT_PROFILE));
        }

        String requested = readActiveFromIndex();
        List<String> profiles = listProfileNames();
        String active = profiles.stream()
                .filter(p -> p.equalsIgnoreCase(requested))
                .findFirst()
                .orElseGet(() -> profiles.stream()
                        .filter(p -> p.equalsIgnoreCase(DEFAULT_PROFILE))
                        .findFirst()
                        .orElse(profiles.isEmpty() ? DEFAULT_PROFILE : profiles.get(0)));
        activeProfileName = active;
        load(active);
        if (!Files.exists(INDEX)) {
            writeIndex(active);
        }
    }

    private static void migrateLegacyIfPresent() {
        Path legacy = FabricLoader.getInstance().getConfigDir().resolve("multi-key-bindings.json");
        if (!Files.exists(legacy)) {
            return;
        }
        if (Files.exists(getProfilePath(DEFAULT_PROFILE))) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(legacy)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) {
                return;
            }
            int version = json.has("config_version") ? json.get("config_version").getAsInt() : 1;
            if (version < 3) {
                json = ConfigManager.migrateJsonToV3(json, version);
            }

            Profile defaultProfile = new Profile(DEFAULT_PROFILE);
            if (json.has("bindings")) {
                defaultProfile.setBindings(json.getAsJsonArray("bindings"));
            }
            if (json.has("modifiers")) {
                defaultProfile.setModifiers(json.getAsJsonObject("modifiers"));
            }
            // v3 has no vanilla snapshot — leave empty; the next rebind
            // will populate it via the Options.save chain.
            writeProfile(defaultProfile);
            writeIndex(DEFAULT_PROFILE);

            Files.move(legacy, legacy.resolveSibling("multi-key-bindings.json.bak"),
                    StandardCopyOption.REPLACE_EXISTING);
            MultiKeyBindingClient.LOGGER.info("Migrated legacy multi-key-bindings.json to profiles/default.json");
        } catch (IOException | RuntimeException e) {
            MultiKeyBindingClient.LOGGER.error("Migration from legacy config failed", e);
        }
    }

    /**
     * Switch to the named profile. Applies its snapshot, rebuilds MKB state,
     * refreshes the key -> mapping index, and persists to options.txt.
     *
     * @param name The profile to load.
     */
    public static boolean load(String name) {
        Profile profile = readProfile(name);
        if (profile == null) {
            MultiKeyBindingClient.LOGGER.warn("Profile not found: {}", name);
            return false;
        }

        // Persist the outgoing profile so any unsaved changes to it survive
        // the switch. Skip if it's the same profile (no-op) or if there's
        // nothing to save yet (first-launch bootstrap).
        if (!name.equalsIgnoreCase(activeProfileName)) {
            saveActive();
        }

        isLoading = true;
        try {
            MultiKeyBindingManager.clearAll();
            ModifierManager.clearAll();
            ToggleManager.clearAll();

            applyProfile(profile);
            ToggleManager.ensurePrimaries();

            net.minecraft.client.KeyMapping.resetMapping();

            net.minecraft.client.Options options = MultiKeyBindingManager.getGameOptions();
            if (options != null) {
                options.save();
            }

            activeProfileName = name;
            writeIndex(name);
        } finally {
            isLoading = false;
        }
        return true;
    }

    /**
     * Write the current in-memory hidden set into config.json. Called on
     * every hide/unhide mutation from HiddenBindingManager.
     */
    static void persistHiddenSetToIndex() {
        if (isLoading) {
            return;
        }
        writeIndex(activeProfileName);
    }

    /**
     * Populate the key bindings from a profile. Callers must clear the managers
     * first.
     */
    private static void applyProfile(Profile profile) {
        // Populate vanilla key bindings
        net.minecraft.client.Options options = MultiKeyBindingManager.getGameOptions();
        if (options != null) {
            JsonObject vanilla = profile.getVanilla();
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (net.minecraft.client.KeyMapping mapping : options.keyMappings) {
                String translationKey = mapping.getName();
                if (!vanilla.has(translationKey)) {
                    continue;
                }
                String keyName = vanilla.get(translationKey).getAsString();
                com.mojang.blaze3d.platform.InputConstants.Key key = com.mojang.blaze3d.platform.InputConstants
                        .getKey(keyName);
                mapping.setKey(key);
                seen.add(translationKey);
            }
            for (String recorded : vanilla.keySet()) {
                if (!seen.contains(recorded)) {
                    MultiKeyBindingClient.LOGGER.info(
                            "Skipping stale profile entry for unregistered key mapping: {}", recorded);
                }
            }
        }

        // Populate modifiers
        for (var entry : profile.getModifiers().entrySet()) {
            List<InputConstants.Key> modifiers = ConfigManager.parseModifiers(entry.getValue().getAsJsonArray());
            if (!modifiers.isEmpty()) {
                ModifierManager.setModifiers(entry.getKey(), modifiers);
            }
        }
        for (JsonElement element : profile.getBindings()) {
            JsonObject keyBindingJson = element.getAsJsonObject();
            UUID id = UUID.fromString(keyBindingJson.get("id").getAsString());
            String action = keyBindingJson.get("action").getAsString();
            String translationKey = keyBindingJson.get("key").getAsString();
            MultiKeyBindingManager.addKeyBinding(action, null, translationKey, id);
            if (keyBindingJson.has("primary") && keyBindingJson.get("primary").getAsBoolean()) {
                ToggleManager.setPrimary(action, id);
            }
        }
    }

    /**
     * Snapshot current state and write it to the active profile's file.
     * The hidden set is global (config.json), not per-profile.
     */
    public static void saveActive() {
        if (isLoading) {
            return;
        }
        ensureDirectories();
        writeProfile(snapshotCurrent(activeProfileName));
    }

    /**
     * Create a new profile with vanilla defaults and no MKB customization,
     * then switch to it. Use duplicate to copy from an existing profile.
     *
     * @param name The profile name to create.
     */
    public static boolean create(String name) {
        if (!ProfileNameValidator.isValid(name)) {
            return false;
        }
        if (ProfileNameValidator.isTaken(name, listProfileNames())) {
            return false;
        }
        ensureDirectories();
        writeProfile(snapshotDefaults(name));
        return load(name);
    }

    /**
     * Delete a profile file. Fails if the profile is active.
     *
     * @param name The profile to delete.
     */
    public static boolean delete(String name) {
        if (name.equalsIgnoreCase(activeProfileName)) {
            return false;
        }
        try {
            return Files.deleteIfExists(getProfilePath(name));
        } catch (IOException e) {
            MultiKeyBindingClient.LOGGER.error("Failed to delete profile {}", name, e);
            return false;
        }
    }

    /**
     * Rename a profile file. Updates the active profile if renaming it.
     *
     * @param oldName The existing profile name.
     * @param newName The new name to give it.
     */
    public static boolean rename(String oldName, String newName) {
        if (!ProfileNameValidator.isValid(newName)) {
            return false;
        }
        if (!oldName.equalsIgnoreCase(newName) && ProfileNameValidator.isTaken(newName, listProfileNames())) {
            return false;
        }
        Path src = getProfilePath(oldName);
        Path dst = getProfilePath(newName);
        if (!Files.exists(src)) {
            return false;
        }
        try {
            Files.move(src, dst, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            MultiKeyBindingClient.LOGGER.error("Failed to rename profile {} -> {}", oldName, newName, e);
            return false;
        }
        if (oldName.equalsIgnoreCase(activeProfileName)) {
            activeProfileName = newName;
            writeIndex(newName);
        }
        return true;
    }

    /**
     * Duplicate a profile file. A null newName picks "<name>_copy" (numbered
     * _copy2, _copy3, ... if taken). Returns the resolved target name on
     * success, or null if the source doesn't exist, the target name is
     * invalid or taken, or the copy fails.
     *
     * @param name    The profile to duplicate.
     * @param newName The name of the new copy, or null to auto-name.
     */
    public static String duplicate(String name, String newName) {
        if (!Files.exists(getProfilePath(name))) {
            return null;
        }
        String target = newName;
        if (target == null || target.isEmpty()) {
            List<String> existing = listProfileNames();
            target = name + "_copy";
            int n = 2;
            while (ProfileNameValidator.isTaken(target, existing)) {
                target = name + "_copy" + n++;
            }
        }
        if (!ProfileNameValidator.isValid(target)) {
            return null;
        }
        if (ProfileNameValidator.isTaken(target, listProfileNames())) {
            return null;
        }
        try {
            Files.copy(getProfilePath(name), getProfilePath(target));
        } catch (IOException e) {
            MultiKeyBindingClient.LOGGER.error("Failed to duplicate profile {} -> {}", name, target, e);
            return null;
        }
        return target;
    }
}
