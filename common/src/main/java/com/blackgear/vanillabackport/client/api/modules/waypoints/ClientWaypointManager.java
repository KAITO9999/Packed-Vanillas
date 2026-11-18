package com.blackgear.vanillabackport.client.api.modules.waypoints;

import com.blackgear.vanillabackport.internal.platform.client.event.screen.HudInteractions;
import com.blackgear.vanillabackport.client.api.modules.waypoints.provider.CardinalWaypointProvider;
import com.blackgear.vanillabackport.client.api.modules.waypoints.provider.ItemWaypointProvider;
import com.blackgear.vanillabackport.client.api.modules.waypoints.provider.WaypointProvider;
import com.blackgear.vanillabackport.common.api.modules.waypoints.TrackedWaypoint;
import com.blackgear.vanillabackport.common.api.modules.waypoints.TrackedWaypointManager;
import com.mojang.datafixers.util.Either;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ClientWaypointManager implements TrackedWaypointManager {
    public static final ClientWaypointManager INSTANCE = new ClientWaypointManager();

    private final Map<Either<UUID, String>, TrackedWaypoint> waypoints = new ConcurrentHashMap<>();
    private final List<WaypointProvider> providers = new CopyOnWriteArrayList<>();
    private List<TrackedWaypoint> sortedWaypoints = List.of();

    private long waypointRevision;
    private long sortedWaypointRevision = Long.MIN_VALUE;
    private Entity sortedFromEntity;
    private int sortedEntityTick = Integer.MIN_VALUE;
    private double sortedEntityX = Double.NaN;
    private double sortedEntityY = Double.NaN;
    private double sortedEntityZ = Double.NaN;
    private long lastTickUpdate = -1;

    public static void bootstrap() {
        HudInteractions.CONTAINER_TICK.register((minecraft, screen) -> {
            LocalPlayer player = minecraft.player;
            if (player == null) return;
            if (screen instanceof EnchantmentScreen || screen instanceof AnvilScreen) {
                ExperienceDisplay.of(player).setExperienceDisplayStartTick(player.tickCount);
            }
        });

        INSTANCE.registerProvider(new ItemWaypointProvider());
        INSTANCE.registerProvider(new CardinalWaypointProvider());
    }

    public void registerProvider(WaypointProvider provider) {
        this.providers.add(provider);
    }

    public void tick(LocalPlayer player) {
        if (player == null) return;

        if (player.tickCount == this.lastTickUpdate) return;
        this.lastTickUpdate = player.tickCount;

        for (WaypointProvider provider : this.providers) {
            provider.tick(player, this);
        }
    }

    @Override
    public void trackWaypoint(TrackedWaypoint waypoint) {
        this.waypoints.put(waypoint.id(), waypoint);
        this.waypointRevision++;
    }

    @Override
    public void updateWaypoint(TrackedWaypoint waypoint) {
        TrackedWaypoint trackedWaypoint = this.waypoints.get(waypoint.id());
        if (trackedWaypoint != null) {
            trackedWaypoint.update(waypoint);
            this.waypointRevision++;
        }
    }

    @Override
    public void untrackWaypoint(TrackedWaypoint waypoint) {
        if (this.waypoints.remove(waypoint.id()) != null) {
            this.waypointRevision++;
        }
    }

    public boolean hasWaypoints() {
        return !this.waypoints.isEmpty();
    }

    public void forEachWaypoint(Entity fromEntity, Consumer<TrackedWaypoint> consumer) {
        if (this.sortedWaypointRevision != this.waypointRevision
            || this.sortedFromEntity != fromEntity
            || this.sortedEntityTick != fromEntity.tickCount
            || this.sortedEntityX != fromEntity.getX()
            || this.sortedEntityY != fromEntity.getY()
            || this.sortedEntityZ != fromEntity.getZ()) {
            List<TrackedWaypoint> sorted = new ArrayList<>(this.waypoints.values());
            sorted.sort(Comparator.comparingDouble(
                (TrackedWaypoint waypoint) -> waypoint.distanceSquared(fromEntity)
            ).reversed());

            this.sortedWaypoints = sorted;
            this.sortedWaypointRevision = this.waypointRevision;
            this.sortedFromEntity = fromEntity;
            this.sortedEntityTick = fromEntity.tickCount;
            this.sortedEntityX = fromEntity.getX();
            this.sortedEntityY = fromEntity.getY();
            this.sortedEntityZ = fromEntity.getZ();
        }

        for (TrackedWaypoint waypoint : this.sortedWaypoints) {
            consumer.accept(waypoint);
        }
    }

    public Map<Either<UUID, String>, TrackedWaypoint> waypoints() {
        // External callers receive a mutable map; invalidate before exposing it so
        // mutations made immediately after retrieval are reflected on the next render.
        this.waypointRevision++;
        return this.waypoints;
    }

    public void clear() {
        if (!this.waypoints.isEmpty()) {
            this.waypoints.clear();
            this.waypointRevision++;
        }
    }
}
