package com.blackgear.vanillabackport.core.forge;

import com.blackgear.vanillabackport.internal.platform.core.Environment;
import com.blackgear.vanillabackport.core.VanillaBackport;
import com.blackgear.vanillabackport.internal.platform.Platform;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(VanillaBackport.MOD_ID)
public final class VanillaBackportForge {
    public VanillaBackportForge() {
        Platform.bootstrap();
        VanillaBackport.bootstrap();
        
        if (Environment.isClientSide()) ClientEnumData.bootstrap();

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::commonSetup);
    }

    public void commonSetup(FMLCommonSetupEvent event) {
        if (Environment.hasModLoaded("terrablender")) {
            VanillaBackportTerrablender.onTerraBlenderInitialized();
        }
    }
}