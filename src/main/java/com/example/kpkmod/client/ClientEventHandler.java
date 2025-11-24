package com.example.kpkmod.client;

import com.example.kpkmod.ExampleMod;
import com.example.kpkmod.User;
import com.example.kpkmod.chat.ClientChatCache;
import com.example.kpkmod.gui.KPKModelInteractionGui;
import com.example.kpkmod.item.ItemKPK;
import com.example.kpkmod.network.PacketHandler;
import com.example.kpkmod.network.PacketRequestChannelSync;
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
    private static long lastIdentityCheckMs = 0L;
    private static final long IDENTITY_CHECK_INTERVAL_MS = 250L; // throttle to 4x per second

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != Side.CLIENT || event.phase != TickEvent.Phase.END) return;

        EntityPlayer player = event.player;
        if (player == null || player.world == null) return;

        // Эта проверка выполняется постоянно, но с троттлингом
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastIdentityCheckMs >= IDENTITY_CHECK_INTERVAL_MS) {
            lastIdentityCheckMs = nowMs;
            checkActiveKpkIdentity(player);
        }

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
        // Проверяем, что игрок в валидном мире и сервер доступен (проверка соединения для одиночной игры)
        if (player.world == null || Minecraft.getMinecraft().getConnection() == null) {
            return;
        }

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
            // Дополнительная проверка перед отправкой пакета
            if (Minecraft.getMinecraft().getConnection() != null && PacketHandler.INSTANCE != null) {
                PacketHandler.INSTANCE.sendToServer(new PacketRequestChannelSync());
            }
        }
    }
}