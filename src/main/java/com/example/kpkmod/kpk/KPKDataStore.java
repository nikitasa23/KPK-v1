package com.example.kpkmod.kpk;

import com.example.kpkmod.Gender;
import com.example.kpkmod.User;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KPKDataStore extends WorldSavedData {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final Map<UUID, User> playerData = new ConcurrentHashMap<>();

    public KPKDataStore(String name) {
        super(name);
    }

    public void setUserData(UUID uuid, User user) {
        if (user == null) {
            playerData.remove(uuid);
        } else {
            playerData.put(uuid, user);
        }
        markDirty();
    }

    public User getUserData(UUID uuid) {
        return playerData.get(uuid);
    }

    public Map<UUID, User> getFullDatabase() {
        return Collections.unmodifiableMap(playerData);
    }

    public Pair<UUID, User> findUserByCallsign(String callsign) {
        if (callsign == null || callsign.isEmpty()) {
            return null;
        }
        for (Map.Entry<UUID, User> entry : playerData.entrySet()) {
            if (callsign.equalsIgnoreCase(entry.getValue().pozivnoy)) {
                return Pair.of(entry.getKey(), entry.getValue());
            }
        }
        return null;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        playerData.clear();
        NBTTagList playerList = nbt.getTagList("PlayerData", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < playerList.tagCount(); i++) {
            NBTTagCompound playerTag = playerList.getCompoundTagAt(i);
            try {
                UUID uuid = UUID.fromString(playerTag.getString("UUID"));
                User user = new User(
                        playerTag.getString("familiya"),
                        playerTag.getString("name"),
                        playerTag.getString("pozivnoy"),
                        Gender.valueOf(playerTag.getString("gender")),
                        LocalDate.parse(playerTag.getString("birthdate"), DATE_FORMATTER)
                );
                playerData.put(uuid, user);
            } catch (Exception e) {
                System.err.println("Could not load KPK user data from NBT: " + e.getMessage());
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList playerList = new NBTTagList();
        for (Map.Entry<UUID, User> entry : playerData.entrySet()) {
            NBTTagCompound playerTag = new NBTTagCompound();
            User user = entry.getValue();
            playerTag.setString("UUID", entry.getKey().toString());
            playerTag.setString("familiya", user.familiya);
            playerTag.setString("name", user.name);
            playerTag.setString("pozivnoy", user.pozivnoy);
            playerTag.setString("gender", user.gender.name());
            playerTag.setString("birthdate", user.birthdate.format(DATE_FORMATTER));
            playerList.appendTag(playerTag);
        }
        compound.setTag("PlayerData", playerList);
        return compound;
    }
}