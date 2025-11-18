package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
    public static SimpleNetworkWrapper INSTANCE;

    public static void init() {
        INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(ExampleMod.MODID);
        registerMessages();
    }

    private static void registerMessages() {
        int id = 0;
        INSTANCE.registerMessage(PacketAddContactRequest.Handler.class, PacketAddContactRequest.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketRemoveContactRequest.Handler.class, PacketRemoveContactRequest.class, id++, Side.SERVER);

        INSTANCE.registerMessage(PacketChatMessageToServer.Handler.class, PacketChatMessageToServer.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketBroadcastChatMessageToClient.Handler.class, PacketBroadcastChatMessageToClient.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(PacketSyncChatHistoryToClient.Handler.class, PacketSyncChatHistoryToClient.class, id++, Side.CLIENT);

        INSTANCE.registerMessage(PacketRequestCreatePMChannel.Handler.class, PacketRequestCreatePMChannel.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketRequestCreateGroupChannel.Handler.class, PacketRequestCreateGroupChannel.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketRequestAddUserToGroupChannel.Handler.class, PacketRequestAddUserToGroupChannel.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketNotifyChannelCreatedOrUpdated.Handler.class, PacketNotifyChannelCreatedOrUpdated.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(PacketSyncSubscribedChannels.Handler.class, PacketSyncSubscribedChannels.class, id++, Side.CLIENT);

        INSTANCE.registerMessage(PacketRequestDeleteChannel.Handler.class, PacketRequestDeleteChannel.class, id++, Side.SERVER);
        INSTANCE.registerMessage(PacketNotifyChannelDeleted.Handler.class, PacketNotifyChannelDeleted.class, id++, Side.CLIENT);

        INSTANCE.registerMessage(PacketRequestRemoveMember.Handler.class, PacketRequestRemoveMember.class, id++, Side.SERVER);

        INSTANCE.registerMessage(PacketSyncKpkDatabaseToClient.Handler.class, PacketSyncKpkDatabaseToClient.class, id++, Side.CLIENT);
        INSTANCE.registerMessage(PacketRequestChannelSync.Handler.class, PacketRequestChannelSync.class, id++, Side.SERVER);

        INSTANCE.registerMessage(PacketNotifyNewMessage.Handler.class, PacketNotifyNewMessage.class, id++, Side.CLIENT);

        // Registration
        INSTANCE.registerMessage(PacketRegisterKpkUser.Handler.class, PacketRegisterKpkUser.class, id++, Side.SERVER);
    }
}