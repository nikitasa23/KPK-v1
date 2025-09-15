package com.example.examplemod.kpk;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.chat.ChatManager;
import com.example.examplemod.network.PacketHandler;
import com.example.examplemod.network.PacketSyncKpkDatabaseToClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class CapabilityHandler {

    @SubscribeEvent
    public static void onPlayerLogin(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (!player.world.isRemote && player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            // Синхронизируем чаты как и раньше
            ChatManager.onPlayerLogin(playerMP);
            // А теперь дополнительно отправляем всю базу данных КПК
            PacketHandler.INSTANCE.sendTo(new PacketSyncKpkDatabaseToClient(KPKServerManager.getFullKpkDatabase()), playerMP);
        }
    }
}