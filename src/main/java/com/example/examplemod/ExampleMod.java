package com.example.examplemod;

import com.example.examplemod.chat.ChatManager;
import com.example.examplemod.command.CommandKPKSet;
import com.example.examplemod.gui.GuiHandler;
import com.example.examplemod.discord.DiscordBridge;
import com.example.examplemod.kpk.KPKServerManager;
import com.example.examplemod.network.PacketHandler;
import com.example.examplemod.proxy.CommonProxy;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.Logger;

@Mod(modid = ExampleMod.MODID, name = ExampleMod.NAME, version = ExampleMod.VERSION)
public class ExampleMod
{
    public static final String MODID = "examplemod";
    public static final String NAME = "Example Mod";
    public static final String VERSION = "1.0";
    public static ExampleMod instance;
    public static Logger logger;

    @SidedProxy(clientSide = "com.example.examplemod.proxy.ClientProxy", serverSide = "com.example.examplemod.proxy.CommonProxy")
    public static CommonProxy proxy;

    public ExampleMod() {
        instance = this;
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        PacketHandler.init();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandKPKSet());
        ChatManager.load(event.getServer());
        KPKServerManager.load(event.getServer());
        java.io.File configDir = event.getServer().getFile("config");
        DiscordBridge.init(configDir);
    }

    @EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            ChatManager.save(server);
            KPKServerManager.save(server);
            DiscordBridge.shutdown();
        } else {
            ExampleMod.logger.warn("Could not save data: MinecraftServer instance is null during FMLServerStoppedEvent.");
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());
    }
}