package com.example.examplemod.chat;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ChatChannel {
    private String channelId;
    private String displayName;
    private ChatChannelType type;
    private List<UUID> members;
    private UUID creatorUuid;
    private int maxMembers;

    public static final String COMMON_CHANNEL_ID_PREFIX = "common_server_global";

    public ChatChannel() {
        this.members = new ArrayList<>();
    }

    public ChatChannel(String channelId, String displayName, ChatChannelType type) {
        this.channelId = channelId;
        this.displayName = displayName;
        this.type = type;
        this.members = new ArrayList<>();
        this.creatorUuid = null;
        this.maxMembers = Integer.MAX_VALUE;
    }

    public ChatChannel(String channelId, String displayName, ChatChannelType type, UUID creatorUuid, List<UUID> initialMembers, int maxMembers) {
        this.channelId = channelId;
        this.displayName = displayName;
        this.type = type;
        this.creatorUuid = creatorUuid;
        this.members = new ArrayList<>(initialMembers);
        if (!this.members.contains(creatorUuid) && type != ChatChannelType.COMMON_SERVER_WIDE) {
            this.members.add(creatorUuid);
        }
        this.maxMembers = maxMembers;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatChannelType getType() {
        return type;
    }

    public List<UUID> getMembers() {
        return new ArrayList<>(members);
    }

    public UUID getCreatorUuid() {
        return creatorUuid;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean addMember(UUID memberUuid) {
        if (members.size() < maxMembers && !members.contains(memberUuid)) {
            members.add(memberUuid);
            return true;
        }
        return false;
    }

    public boolean removeMember(UUID memberUuid) {
        return members.remove(memberUuid);
    }

    public boolean isMember(UUID memberUuid) {
        if (type == ChatChannelType.COMMON_SERVER_WIDE) return true;
        return members.contains(memberUuid);
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("channelId", channelId);
        nbt.setString("displayName", displayName);
        nbt.setString("type", type.name());
        if (creatorUuid != null) {
            nbt.setString("creatorUuid", creatorUuid.toString());
        }
        nbt.setInteger("maxMembers", maxMembers);

        NBTTagList memberListNBT = new NBTTagList();
        for (UUID member : members) {
            memberListNBT.appendTag(new NBTTagString(member.toString()));
        }
        nbt.setTag("members", memberListNBT);
        return nbt;
    }

    public static ChatChannel fromNBT(NBTTagCompound nbt) {
        ChatChannel channel = new ChatChannel();
        channel.channelId = nbt.getString("channelId");
        channel.displayName = nbt.getString("displayName");
        channel.type = ChatChannelType.valueOf(nbt.getString("type"));
        if (nbt.hasKey("creatorUuid")) {
            try {
                channel.creatorUuid = UUID.fromString(nbt.getString("creatorUuid"));
            } catch (IllegalArgumentException e) {
                channel.creatorUuid = null;
            }
        }
        channel.maxMembers = nbt.getInteger("maxMembers");

        NBTTagList memberListNBT = nbt.getTagList("members", Constants.NBT.TAG_STRING);
        for (int i = 0; i < memberListNBT.tagCount(); i++) {
            try {
                channel.members.add(UUID.fromString(memberListNBT.getStringTagAt(i)));
            } catch (IllegalArgumentException e) {
            }
        }
        return channel;
    }

    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, channelId);
        ByteBufUtils.writeUTF8String(buf, displayName);
        ByteBufUtils.writeUTF8String(buf, type.name());
        buf.writeBoolean(creatorUuid != null);
        if (creatorUuid != null) {
            ByteBufUtils.writeUTF8String(buf, creatorUuid.toString());
        }
        buf.writeInt(maxMembers);
        buf.writeInt(members.size());
        for (UUID member : members) {
            ByteBufUtils.writeUTF8String(buf, member.toString());
        }
    }

    public static ChatChannel fromBytes(ByteBuf buf) {
        ChatChannel channel = new ChatChannel();
        channel.channelId = ByteBufUtils.readUTF8String(buf);
        channel.displayName = ByteBufUtils.readUTF8String(buf);
        channel.type = ChatChannelType.valueOf(ByteBufUtils.readUTF8String(buf));
        if (buf.readBoolean()) {
            channel.creatorUuid = UUID.fromString(ByteBufUtils.readUTF8String(buf));
        }
        channel.maxMembers = buf.readInt();
        int memberCount = buf.readInt();
        for (int i = 0; i < memberCount; i++) {
            channel.members.add(UUID.fromString(ByteBufUtils.readUTF8String(buf)));
        }
        return channel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatChannel that = (ChatChannel) o;
        return Objects.equals(channelId, that.channelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId);
    }

    public static String generatePMChannelId(UUID user1, UUID user2) {
        String id1 = user1.toString();
        String id2 = user2.toString();
        if (id1.compareTo(id2) > 0) {
            return "pm_" + id2 + "_" + id1;
        } else {
            return "pm_" + id1 + "_" + id2;
        }
    }

    public static String generateGroupChannelId(UUID creatorUuid, String channelName) {
        String safeName = channelName.replaceAll("[^a-zA-Z0-9А-Яа-яЁё]", "").toLowerCase();
        if(safeName.length() > 10) safeName = safeName.substring(0, 10);
        return "grp_" + creatorUuid.toString().substring(0, 4) + "_" + safeName + "_" + (System.currentTimeMillis() % 10000);
    }
}