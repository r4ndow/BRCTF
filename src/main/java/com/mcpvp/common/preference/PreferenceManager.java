package com.mcpvp.common.preference;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpvp.common.task.EasyTask;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages storing and loading of Player preferences. A preference is a key (name) and type.
 */
public class PreferenceManager {

    private final Plugin plugin;
    private final ObjectMapper objectMapper;
    private final File file;
    private final Map<UUID, Map<String, JsonNode>> stored = new HashMap<>();

    /**
     * Creates a new PreferenceManager. Preferences will be loaded immediately.
     *
     * @param plugin The plugin controlling the preferences.
     * @param objectMapper The object mapper to use for deserializing and serializing.
     * @param file The file to load and save preferences in. Will be created if needed.
     */
    public PreferenceManager(Plugin plugin, ObjectMapper objectMapper, File file) {
        this.plugin = plugin;
        this.objectMapper = objectMapper;
        this.file = file;
        this.load();
    }

    /**
     * Stores a preference for a given player. Immediately after this call, {@link #find(Player, Preference)} will
     * return the stored value, but saving to the preference file is run asynchronously.
     *
     * @param player The player to associate the preference with.
     * @param preference The preference to store.
     * @param value The value of the preference.
     * @param <T> The type being stored.
     */
    public <T> void store(
        Player player, Preference<T> preference, T value
    ) {
        this.stored.computeIfAbsent(player.getUniqueId(), p -> new HashMap<>())
            .put(preference.getKey(), this.objectMapper.valueToTree(value));

        // Avoid file writes on the main thread
        EasyTask.of(this::save).runTaskAsynchronously(this.plugin);
    }

    /**
     * Attempts to find the value stored for a given player and preference.
     *
     * @param player The player to find the value for.
     * @param preference The preference to find the value for.
     * @return The stored value or an empty optional if no value could be found.
     * @param <T> The type of value stored.
     */
    public <T> Optional<T> find(Player player, Preference<T> preference) {
        if (!this.stored.containsKey(player.getUniqueId())) {
            return Optional.empty();
        }

        if (!this.stored.get(player.getUniqueId()).containsKey(preference.getKey())) {
            return Optional.empty();
        }

        return Optional.ofNullable(
            this.objectMapper.convertValue(
                this.stored.get(player.getUniqueId()).get(preference.getKey()),
                preference.getType()
            )
        );
    }

    private void load() {
        synchronized (this) {
            try {
                if (this.file.createNewFile()) {
                    this.objectMapper.writeValue(this.file, this.stored);
                }

                Map<UUID, Map<String, JsonNode>> loaded = this.objectMapper.readValue(this.file, new TypeReference<>() {
                });
                this.stored.putAll(loaded);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load preferences file from " + this.file, e);
            }
        }
    }

    private void save() {
        synchronized (this) {
            try {
                this.objectMapper.writeValue(this.file, this.stored);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save preferences file to " + this.file, e);
            }
        }
    }

}
