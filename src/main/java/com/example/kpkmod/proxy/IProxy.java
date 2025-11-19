package com.example.kpkmod.proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public interface IProxy {
    void playSound(String soundName, float volume, float pitch);
    void sendMessageToPlayer(EntityPlayer player, TextComponentString message);
    void openKPKGui(EntityPlayer player, World world, int x, int y, int z);
    boolean isKPKGuiOpen();
    void closeKPKGui(EntityPlayer player);
    boolean isClient();
}