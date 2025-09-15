package com.example.examplemod.proxy;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.gui.GuiHandler;
import com.example.examplemod.gui.KPKModelInteractionGui;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void sendMessageToPlayer(EntityPlayer player, TextComponentString message) {
    }

    @Override
    public void openKPKGui(EntityPlayer player, World world, int x, int y, int z) {
        player.openGui(ExampleMod.instance, GuiHandler.KPK_MODEL_INTERACTION_GUI_ID, world, x, y, z);
    }

    @Override
    public boolean isKPKGuiOpen() {
        return Minecraft.getMinecraft().currentScreen instanceof KPKModelInteractionGui;
    }

    @Override
    public void closeKPKGui(EntityPlayer player) {
        if (player == Minecraft.getMinecraft().player && Minecraft.getMinecraft().currentScreen != null) {
            player.closeScreen();
        }
    }

    @Override
    public boolean isClient() {
        return true;
    }
}