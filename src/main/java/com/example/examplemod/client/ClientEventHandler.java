package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.User;
import com.example.examplemod.chat.ClientChatCache;
import com.example.examplemod.gui.KPKModelInteractionGui;
import com.example.examplemod.item.ItemKPK;
import com.example.examplemod.network.PacketHandler;
import com.example.examplemod.network.PacketRequestChannelSync;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Side.CLIENT)
public class ClientEventHandler {

    private static UUID lastCheckedKpkOwnerUUID = null;

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != Side.CLIENT || event.phase != TickEvent.Phase.END) return;

        EntityPlayer player = event.player;
        if (player == null || player.world == null) return;

        // Эта проверка выполняется постоянно, а не только когда открыт GUI
        checkActiveKpkIdentity(player);

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;

        if (mc.currentScreen instanceof KPKModelInteractionGui) {
            boolean holdingEnabledKPK = false;
            ItemStack kpkStackInHand = null;

            ItemStack mainHand = player.getHeldItemMainhand();
            ItemStack offHand = player.getHeldItemOffhand();

            if (mainHand.getItem() instanceof ItemKPK && ItemKPK.isEnabled(mainHand)) {
                holdingEnabledKPK = true;
                kpkStackInHand = mainHand;
            } else if (offHand.getItem() instanceof ItemKPK && ItemKPK.isEnabled(offHand)) {
                holdingEnabledKPK = true;
                kpkStackInHand = offHand;
            }

            KPKModelInteractionGui currentGui = (KPKModelInteractionGui) mc.currentScreen;
            ItemStack currentScreenStack = currentGui.getKpkStack();

            if (!holdingEnabledKPK) {
                player.closeScreen();
            } else {
                if (currentScreenStack != kpkStackInHand) {
                    player.closeScreen();
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private static void checkActiveKpkIdentity(EntityPlayer player) {
        ItemStack kpkStack = player.getHeldItemMainhand();
        if (!(kpkStack.getItem() instanceof ItemKPK) || !ItemKPK.isEnabled(kpkStack)) {
            kpkStack = player.getHeldItemOffhand();
            if (!(kpkStack.getItem() instanceof ItemKPK) || !ItemKPK.isEnabled(kpkStack)) {
                kpkStack = ItemStack.EMPTY;
            }
        }

        UUID currentOwnerUUID = null;
        if (!kpkStack.isEmpty()) {
            User kpkUser = ItemKPK.getUserData(kpkStack);
            if (kpkUser != null) {
                currentOwnerUUID = ClientChatCache.findUserByCallsign(kpkUser.pozivnoy);
            }
        }

        if (currentOwnerUUID == null) {
            currentOwnerUUID = player.getUniqueID();
        }

        if (lastCheckedKpkOwnerUUID == null || !lastCheckedKpkOwnerUUID.equals(currentOwnerUUID)) {
            lastCheckedKpkOwnerUUID = currentOwnerUUID;
            ClientChatCache.activeKpkOwnerUUID = currentOwnerUUID;
            PacketHandler.INSTANCE.sendToServer(new PacketRequestChannelSync());
        }
    }
}