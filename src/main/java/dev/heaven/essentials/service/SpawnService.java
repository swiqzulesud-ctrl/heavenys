package dev.heaven.essentials.service;

import dev.heaven.essentials.data.DataStore;
import org.bukkit.Location;

/**
 * The single global location system of the plugin: the spawn point managed
 * by {@code /setspawn} and used by {@code /spawn}.
 */
public final class SpawnService {

    private final DataStore dataStore;

    public SpawnService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /** Returns the global spawn, or {@code null} if it has not been set. */
    public Location getSpawn() {
        return dataStore.getSpawn();
    }

    /** Updates the global spawn point; persisted immediately. */
    public void setSpawn(Location location) {
        dataStore.setSpawn(location.clone());
    }
}
