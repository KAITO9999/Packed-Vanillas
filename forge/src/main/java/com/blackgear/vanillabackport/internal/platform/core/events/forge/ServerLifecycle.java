package com.blackgear.vanillabackport.internal.platform.core.events.forge;

import com.blackgear.vanillabackport.internal.platform.Platform;
import com.blackgear.vanillabackport.internal.platform.core.events.ServerLifecycleEvents;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "vanillabackport")
public class ServerLifecycle {
    @SubscribeEvent
    public static void onServerStartup(ServerAboutToStartEvent event) {
        ServerLifecycleEvents.PRE_STARTING.invoker().onLifecycle(event.getServer());
    }
    
    @SubscribeEvent
    public static void onServerStartup(ServerStartingEvent event) {
        ServerLifecycleEvents.STARTING.invoker().onLifecycle(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLifecycleEvents.STARTED.invoker().onLifecycle(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ServerLifecycleEvents.STOPPING.invoker().onLifecycle(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ServerLifecycleEvents.STOPPED.invoker().onLifecycle(event.getServer());
    }
}