package com.example.kpkmod.chat;

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
    private List<String> memberCallsigns;
    private String creatorCallsign;
    private int maxMembers;

    public static final String COMMON_CHANNEL_ID_PREFIX = "common_server_global";

    public ChatChannel() {
        this.memberCallsigns = new ArrayList<>();
    }

    public ChatChannel(String channelId, String displayName, ChatChannelType type) {
        this.channelId = channelId;
        this.displayName = displayName;
        this.type = type;
        this.memberCallsigns = new ArrayList<>();
        this.creatorCallsign = null;
        this.maxMembers = Integer.MAX_VALUE;
    }

    public ChatChannel(String channelId, String displayName, ChatChannelType type, String creatorCallsign, List<String> initialMemberCallsigns, int maxMembers) {
        this.channelId = channelId;
        this.displayName = displayName;
        this.type = type;
        this.creatorCallsign = creatorCallsign;
        this.memberCallsigns = new ArrayList<>(initialMemberCallsigns);
        if (creatorCallsign != null && !this.memberCallsigns.contains(creatorCallsign) && type != ChatChannelType.COMMON_SERVER_WIDE) {
            this.memberCallsigns.add(creatorCallsign);
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

    public List<String> getMemberCallsigns() {
        return new ArrayList<>(memberCallsigns);
    }

    public String getCreatorCallsign() {
        return creatorCallsign;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean addMember(String memberCallsign) {
        if (memberCallsign == null || memberCallsign.isEmpty()) return false;
        if (memberCallsigns.size() < maxMembers && !memberCallsigns.contains(memberCallsign)) {
            memberCallsigns.add(memberCallsign);
            return true;
        }
        return false;
    }

    public boolean removeMember(String memberCallsign) {
        return memberCallsigns.remove(memberCallsign);
    }

    public boolean isMember(String memberCallsign) {
        if (type == ChatChannelType.COMMON_SERVER_WIDE) return true;
        if (memberCallsign == null || memberCallsign.isEmpty()) return false;
        return memberCallsigns.contains(memberCallsign);
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("channelId", channelId);
        nbt.setString("displayName", displayName);
        nbt.setString("type", type.name());
        if (creatorCallsign != null) {
            nbt.setString("creatorCallsign", creatorCallsign);
        }
        nbt.setInteger("maxMembers", maxMembers);

        NBTTagList memberListNBT = new NBTTagList();
        for (String memberCallsign : memberCallsigns) {
            memberListNBT.appendTag(new NBTTagString(memberCallsign));
        }
        nbt.setTag("memberCallsigns", memberListNBT);
        return nbt;
    }

    public static ChatChannel fromNBT(NBTTagCompound nbt) {
        ChatChannel channel = new ChatChannel();
        channel.channelId = nbt.getString("channelId");
        channel.displayName = nbt.getString("displayName");
        channel.type = ChatChannelType.valueOf(nbt.getString("type"));
        
        // Поддержка миграции старых данных (UUID -> позывные)
        if (nbt.hasKey("creatorCallsign")) {
            channel.creatorCallsign = nbt.getString("creatorCallsign");
        } else if (nbt.hasKey("creatorUuid")) {
            // Старый формат - попытка найти позывной по UUID
            try {
                UUID oldCreatorUuid = UUID.fromString(nbt.getString("creatorUuid"));
                // Позывной будет установлен позже при загрузке через KPKServerManager
                channel.creatorCallsign = null;
            } catch (IllegalArgumentException e) {
                channel.creatorCallsign = null;
            }
        }
        
        channel.maxMembers = nbt.getInteger("maxMembers");

        // Поддержка миграции старых данных
        if (nbt.hasKey("memberCallsigns")) {
            NBTTagList memberListNBT = nbt.getTagList("memberCallsigns", Constants.NBT.TAG_STRING);
            for (int i = 0; i < memberListNBT.tagCount(); i++) {
                channel.memberCallsigns.add(memberListNBT.getStringTagAt(i));
            }
        } else if (nbt.hasKey("members")) {
            // Старый формат - UUID будут конвертированы позже
            // Пока оставляем пустым, будет заполнено при загрузке через KPKServerManager
            channel.memberCallsigns.clear();
        }
        return channel;
    }

    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, channelId);
        ByteBufUtils.writeUTF8String(buf, displayName);
        ByteBufUtils.writeUTF8String(buf, type.name());
        buf.writeBoolean(creatorCallsign != null);
        if (creatorCallsign != null) {
            ByteBufUtils.writeUTF8String(buf, creatorCallsign);
        }
        buf.writeInt(maxMembers);
        buf.writeInt(memberCallsigns.size());
        for (String memberCallsign : memberCallsigns) {
            ByteBufUtils.writeUTF8String(buf, memberCallsign);
        }
    }

    public static ChatChannel fromBytes(ByteBuf buf) {
        ChatChannel channel = new ChatChannel();
        channel.channelId = ByteBufUtils.readUTF8String(buf);
        channel.displayName = ByteBufUtils.readUTF8String(buf);
        channel.type = ChatChannelType.valueOf(ByteBufUtils.readUTF8String(buf));
        if (buf.readBoolean()) {
            channel.creatorCallsign = ByteBufUtils.readUTF8String(buf);
        }
        channel.maxMembers = buf.readInt();
        int memberCount = buf.readInt();
        for (int i = 0; i < memberCount; i++) {
            channel.memberCallsigns.add(ByteBufUtils.readUTF8String(buf));
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

    public static String generatePMChannelId(String callsign1, String callsign2) {
        if (callsign1 == null || callsign2 == null) {
            throw new IllegalArgumentException("Callsigns cannot be null");
        }
        String id1 = callsign1.toLowerCase();
        String id2 = callsign2.toLowerCase();
        if (id1.compareTo(id2) > 0) {
            return "pm_" + id2 + "_" + id1;
        } else {
            return "pm_" + id1 + "_" + id2;
        }
    }

    public static String generateGroupChannelId(String creatorCallsign, String channelName) {
        if (creatorCallsign == null) {
            throw new IllegalArgumentException("Creator callsign cannot be null");
        }
        String safeName = channelName.replaceAll("[^a-zA-Z0-9А-Яа-яЁё]", "").toLowerCase();
        if(safeName.length() > 10) safeName = safeName.substring(0, 10);
        String safeCallsign = creatorCallsign.replaceAll("[^a-zA-Z0-9А-Яа-яЁё]", "").toLowerCase();
        if(safeCallsign.length() > 8) safeCallsign = safeCallsign.substring(0, 8);
        return "grp_" + safeCallsign + "_" + safeName + "_" + (System.currentTimeMillis() % 10000);
    }
}