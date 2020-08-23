/*
 * WorldGuard, a suite of tools for Minecraft
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldGuard team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldguard.commands;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.util.formatting.component.CodeFormat;
import com.sk89q.worldedit.util.formatting.component.ErrorFormat;
import com.sk89q.worldedit.util.formatting.component.LabelFormat;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.util.formatting.text.event.HoverEvent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.util.formatting.text.format.TextDecoration;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.config.ConfigurationManager;
import com.sk89q.worldguard.config.WorldConfiguration;
import com.sk89q.worldguard.util.Entities;

public class ToggleCommands {
    private final WorldGuard worldGuard;

    public ToggleCommands(WorldGuard worldGuard) {
        this.worldGuard = worldGuard;
    }

    @Command(aliases = {"stopfire"}, usage = "[<мир>]",
            desc = "Временно отключить все распространения огня", max = 1)
    @CommandPermissions({"worldguard.fire-toggle.stop"})
    public void stopFire(CommandContext args, Actor sender) throws CommandException {
        
        World world;
        
        if (args.argsLength() == 0) {
            world = worldGuard.checkPlayer(sender).getWorld();
        } else {
            world = worldGuard.getPlatform().getMatcher().matchWorld(sender, args.getString(0));
        }
        
        WorldConfiguration wcfg = WorldGuard.getInstance().getPlatform().getGlobalStateManager().get(world);

        if (!wcfg.fireSpreadDisableToggle) {
            worldGuard.getPlatform().broadcastNotification(
                    LabelFormat.wrap("Распространение огня в мире '" + world.getName() + "' было отключено "
                    + sender.getDisplayName() + "."));
        } else {
            sender.print("Распространение огня отключено во всех мирах.");
        }

        wcfg.fireSpreadDisableToggle = true;
    }

    @Command(aliases = {"allowfire"}, usage = "[<мир>]",
            desc = "Включить распространение огня", max = 1)
    @CommandPermissions({"worldguard.fire-toggle.stop"})
    public void allowFire(CommandContext args, Actor sender) throws CommandException {
        
        World world;
        
        if (args.argsLength() == 0) {
            world = worldGuard.checkPlayer(sender).getWorld();
        } else {
            world = worldGuard.getPlatform().getMatcher().matchWorld(sender, args.getString(0));
        }
        
        WorldConfiguration wcfg = WorldGuard.getInstance().getPlatform().getGlobalStateManager().get(world);

        if (wcfg.fireSpreadDisableToggle) {
            worldGuard.getPlatform().broadcastNotification(LabelFormat.wrap("Распространение онгня в мире '" + world.getName() + "' было включено "
                    + sender.getDisplayName() + "."));
        } else {
            sender.print("Распространение огня включеново всех мирах.");
        }

        wcfg.fireSpreadDisableToggle = false;
    }

    @Command(aliases = {"halt-activity", "stoplag", "haltactivity"}, usage = "[подтвердить]",
            desc = "Остановить/возобновить всю активность на сервере", flags = "cis", max = 1)
    @CommandPermissions({"worldguard.halt-activity"})
    public void stopLag(CommandContext args, Actor sender) throws CommandException {

        ConfigurationManager configManager = WorldGuard.getInstance().getPlatform().getGlobalStateManager();

        if (args.hasFlag('i')) {
            if (configManager.activityHaltToggle) {
                 sender.print("Вся активность на сервере была остановлена.");
            } else {
                 sender.print("Вся активность на сервере была возобновлена.");
            }
        } else {
            boolean activityHaltToggle = !args.hasFlag('c');

            if (activityHaltToggle && (args.argsLength() == 0 || !args.getString(0).equalsIgnoreCase("confirm"))) {
                String confirmCommand = "/" + args.getCommand() + " confirm";

                TextComponent message = TextComponent.builder("")
                        .append(ErrorFormat.wrap("Эта команда "))
                        .append(ErrorFormat.wrap("ПЕРМАНЕНТНА")
                                .decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                        .append(ErrorFormat.wrap(" стереть ВСЕХ животных во ВСЕХ загруженных чанках и во ВСЕХ загруженных мирах. "))
                        .append(TextComponent.newline())
                        .append(TextComponent.of("[Нажми]", TextColor.GREEN)
                                .clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND, confirmCommand))
                                .hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, TextComponent.of("Нажми, чтобы подтвердить /" + args.getCommand()))))
                        .append(ErrorFormat.wrap(" или введи "))
                        .append(CodeFormat.wrap(confirmCommand)
                                .clickEvent(ClickEvent.of(ClickEvent.Action.SUGGEST_COMMAND, confirmCommand)))
                        .append(ErrorFormat.wrap(" для подтверждения."))
                        .build();

                sender.print(message);
                return;
            }

            configManager.activityHaltToggle = activityHaltToggle;

            if (activityHaltToggle) {
                if (!(sender instanceof LocalPlayer)) {
                    sender.print("ВСЕ интенсивные серверные операции остановлены.");
                }

                if (!args.hasFlag('s')) {
                    worldGuard.getPlatform().broadcastNotification(LabelFormat.wrap("ВСЯ интенсивная активность сервера теперь остановлена " + sender.getDisplayName() + "."));
                } else {
                    sender.print("(Бесшумно) Вся интенсивная активность сервера теперь остановлена " + sender.getDisplayName() + ".");
                }

                for (World world : WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.GAME_HOOKS).getWorlds()) {
                    int removed = 0;

                    for (Entity entity : world.getEntities()) {
                        if (Entities.isIntensiveEntity(entity)) {
                            entity.remove();
                            removed++;
                        }
                    }

                    if (removed > 10) {
                        sender.printRaw("" + removed + " (>10) было удалено в мире "
                                + world.getName());
                    }
                }
            } else {
                if (!args.hasFlag('s')) {
                    worldGuard.getPlatform().broadcastNotification(LabelFormat.wrap("ВСЯ интенсивная активность на сервере теперь возобновлена."));
                    
                    if (!(sender instanceof LocalPlayer)) {
                        sender.print("ВСЯ интенсивная активность на сервере теперь возобновлена.");
                    }
                } else {
                    sender.print("(Бесшумно) ВСЯ интенсивная активность на сервере теперь возобновлена.");
                }
            }
        }
    }
}
