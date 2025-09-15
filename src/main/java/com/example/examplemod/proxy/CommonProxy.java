package com.example.examplemod.proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class CommonProxy implements IProxy {
    @Override
    public void playSound(String soundName, float volume, float pitch) {
    }

    @Override
    public void sendMessageToPlayer(EntityPlayer player, TextComponentString message) {
        player.sendMessage(message);
    }

    @Override
    public void openKPKGui(EntityPlayer player, World world, int x, int y, int z) {
    }

    @Override
    public boolean isKPKGuiOpen() {
        return false;
    }

    @Override
    public void closeKPKGui(EntityPlayer player) {
    }

    @Override
    public boolean isClient() {
        return false;
    }
}