package com.example.examplemod.model;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.renderer.texture.TextureMap;
import com.example.examplemod.item.ItemKPKRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Side.CLIENT)
public class TextureRegistry {

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onTextureStitch(TextureStitchEvent.Pre event) {
        TextureMap textureMap = event.getMap();

        textureMap.registerSprite(new ResourceLocation(ExampleMod.MODID, "items/kpk_item"));

        ExampleMod.logger.debug("Registered texture: {}:items/kpk_item", ExampleMod.MODID);
        // Reset cached atlas params to re-apply after stitch
        try {
            java.lang.reflect.Field f = ItemKPKRenderer.class.getDeclaredField("atlasParamsInitialized");
            f.setAccessible(true);
            f.setBoolean(null, false);
        } catch (Throwable ignored) {}
    }
}