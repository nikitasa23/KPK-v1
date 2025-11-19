package com.example.kpkmod.model;

import com.example.kpkmod.ExampleMod;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Side.CLIENT)
public class ModelBakeHandler {

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onModelBake(ModelBakeEvent event) {
        IRegistry<ModelResourceLocation, IBakedModel> registry = event.getModelRegistry();

        ModelResourceLocation kpkModelLocation = new ModelResourceLocation(
                ExampleMod.MODID + ":kpk_device", "inventory");

        IBakedModel originalModel = registry.getObject(kpkModelLocation);

        if (originalModel != null) {
            CustomKPKModel customModel = new CustomKPKModel(originalModel);

            registry.putObject(kpkModelLocation, customModel);

            System.out.println("Successfully wrapped KPK model with custom renderer");
        } else {
            System.err.println("Could not find KPK model to wrap!");
        }
    }
}