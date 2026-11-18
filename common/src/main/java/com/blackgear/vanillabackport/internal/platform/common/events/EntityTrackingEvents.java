package com.blackgear.vanillabackport.internal.platform.common.events;

import com.blackgear.vanillabackport.internal.platform.core.util.event.Event;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public interface EntityTrackingEvents {
    Event<EntityTrackingEvents> START_TRACKING = Event.create(EntityTrackingEvents.class);
    Event<EntityTrackingEvents> STOP_TRACKING = Event.create(EntityTrackingEvents.class);
    
    void onTracking(Entity entity, Player player);
}