package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.item.ModItems;
import com.example.examplemod.item.ItemKPKRenderer;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Side.CLIENT)
public class ModelRegistryHandler {

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        System.out.println("=== MODEL REGISTRATION DEBUG ===");

        OBJLoader.INSTANCE.addDomain(ExampleMod.MODID);
        System.out.println("→ OBJ Loader enabled for domain: " + ExampleMod.MODID);

        if (ModItems.KPK_DEVICE != null) {
            ModItems.KPK_DEVICE.setTileEntityItemStackRenderer(new ItemKPKRenderer());
            System.out.println("→ Set TileEntityItemStackRenderer for KPK_DEVICE");
        }

        if (ModItems.KPK_DEVICE != null && ModItems.KPK_DEVICE.getRegistryName() != null) {
            ModelResourceLocation inventoryVariant = new ModelResourceLocation(
                    ModItems.KPK_DEVICE.getRegistryName(), "inventory");
            ModelLoader.setCustomModelResourceLocation(ModItems.KPK_DEVICE, 0, inventoryVariant);
            ModelBakery.registerItemVariants(ModItems.KPK_DEVICE,
                    new ResourceLocation(ExampleMod.MODID, "kpk_device"));

            System.out.println("→ Registered KPK device model: " + inventoryVariant);
        }

        for (Item item : ModItems.ITEMS) {
            if (item == ModItems.KPK_DEVICE) continue;

            if (item.getRegistryName() == null) {
                System.err.println("Item " + item + " has null registryName! Skipping model registration.");
                continue;
            }

            ModelResourceLocation mrl = new ModelResourceLocation(item.getRegistryName(), "inventory");
            ModelLoader.setCustomModelResourceLocation(item, 0, mrl);

            System.out.println("→ Registered model for: " + item.getRegistryName());
        }

        System.out.println("Model registration complete!");
    }
}