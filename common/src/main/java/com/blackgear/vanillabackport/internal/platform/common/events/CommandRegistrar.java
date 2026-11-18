package com.blackgear.vanillabackport.internal.platform.common.events;

import com.blackgear.vanillabackport.internal.platform.core.util.event.Event;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public interface CommandRegistrar {
    Event<CommandRegistrar> EVENT = Event.create(CommandRegistrar.class);

    void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection selection);
}