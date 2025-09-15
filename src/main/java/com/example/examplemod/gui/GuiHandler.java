package com.example.examplemod.gui;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class GuiHandler implements IGuiHandler {
    public static final int KPK_MODEL_INTERACTION_GUI_ID = 0;
    public static final int KPK_MAIN_GUI_ID = 1;
    public static final int TEXT_INPUT_GUI_ID = 2;

    public static String textInputTitle;
    public static Consumer<String> textInputCallback;
    public static GuiScreen textInputParent;

    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    @Nullable
    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        switch (ID) {
            case KPK_MODEL_INTERACTION_GUI_ID:
                return new KPKModelInteractionGui();
            case KPK_MAIN_GUI_ID:
                return new KPKGui();
            case TEXT_INPUT_GUI_ID:
                return new GuiTextInput(textInputParent, textInputTitle, textInputCallback);
            default:
                return null;
        }
    }
}