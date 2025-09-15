package com.example.examplemod.kpk;

import com.example.examplemod.User;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.MapStorage;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.UUID;

public class KPKServerManager {

    private static final String DATA_NAME = "examplemod_kpk_data";
    private static KPKDataStore dataStore;

    public static void load(MinecraftServer server) {
        MapStorage storage = server.getWorld(0).getMapStorage();
        dataStore = (KPKDataStore) storage.getOrLoadData(KPKDataStore.class, DATA_NAME);
        if (dataStore == null) {
            dataStore = new KPKDataStore(DATA_NAME);
            storage.setData(DATA_NAME, dataStore);
        }
        System.out.println("[ExampleMod] KPKServerManager loaded.");
    }

    public static void save(MinecraftServer server) {
        if (dataStore != null) {
            MapStorage storage = server.getWorld(0).getMapStorage();
            storage.setData(DATA_NAME, dataStore);
            System.out.println("[ExampleMod] KPKServerManager data saved.");
        }
    }

    public static void setUser(UUID uuid, User user) {
        if (dataStore != null) {
            dataStore.setUserData(uuid, user);
        }
    }

    public static User getUser(UUID uuid) {
        return dataStore != null ? dataStore.getUserData(uuid) : null;
    }

    public static Map<UUID, User> getFullKpkDatabase() {
        return dataStore.getFullDatabase();
    }

    public static Pair<UUID, User> findUserByCallsign(String callsign) {
        return dataStore != null ? dataStore.findUserByCallsign(callsign) : null;
    }
}