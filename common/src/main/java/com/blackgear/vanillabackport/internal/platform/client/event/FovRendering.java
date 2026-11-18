package com.blackgear.vanillabackport.internal.platform.client.event;

import com.blackgear.vanillabackport.internal.platform.core.util.event.Event;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.player.Player;

@Environment(EnvType.CLIENT)
public interface FovRendering {
    Event<FovRendering> EVENT = Event.create(FovRendering.class, callback -> (player, fov) -> {
        for (FovRendering render : callback) {
            float newFov = render.setFov(player, fov);
            if (newFov != fov) return newFov;
        }

        return fov;
    });

    float setFov(Player player, float fov);
}