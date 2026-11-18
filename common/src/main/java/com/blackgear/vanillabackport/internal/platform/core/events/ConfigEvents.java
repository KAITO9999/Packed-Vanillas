package com.blackgear.vanillabackport.internal.platform.core.events;

import com.blackgear.vanillabackport.internal.platform.core.util.config.ModConfig;
import com.blackgear.vanillabackport.internal.platform.core.util.event.Event;

public interface ConfigEvents {
    Event<ConfigEvents> LOADING = Event.create(ConfigEvents.class);
    Event<ConfigEvents> RELOADING = Event.create(ConfigEvents.class);

    void accept(ModConfig config);
}