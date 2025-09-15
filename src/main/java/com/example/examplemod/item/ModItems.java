package com.example.examplemod.item;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class ModItems {
    public static final ItemKPK KPK_DEVICE = new ItemKPK();

    public static final List<Item> ITEMS = new ArrayList<>();

    static {
        ITEMS.add(KPK_DEVICE);
    }

    /**
     * Called automatically by Forge at RegistryEvent.Register<Item>.
     * Instead of doing event.getRegistry().register(...) per‐item, we do registerAll(...) over the entire list.
     */
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        Item[] array = ITEMS.toArray(new Item[0]);
        event.getRegistry().registerAll(array);
        System.out.println("=== REGISTERED ITEMS ===");
        for (Item i : ITEMS) {
            System.out.println("→ " + i.getRegistryName());
        }
    }
}