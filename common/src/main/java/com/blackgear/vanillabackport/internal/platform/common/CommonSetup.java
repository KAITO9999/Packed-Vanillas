package com.blackgear.vanillabackport.internal.platform.common;

import com.blackgear.vanillabackport.internal.platform.Platform;
import com.blackgear.vanillabackport.internal.platform.core.events.DataLifecycleEvents;

public class CommonSetup {
    public static void setup() {
        DataLifecycleEvents.DATA_RELOAD.register(Platform::afterDataReload);
    }
}