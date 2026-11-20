package com.btmi.command;

import com.btmi.BTMI;
import com.btmi.belltower.BellTowerClass;
import com.btmi.belltower.BellTowerClasses;
import com.btmi.belltower.BellTowerEnergy;
import com.btmi.belltower.BellTowerPermission;
import com.btmi.belltower.BellTowerQuests;
import com.btmi.belltower.skill.BellTowerSkillManager;
import com.btmi.bloodrain.BloodRainHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = BTMI.MODID)
public final class BTMICommands {
    private BTMICommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("btmi")
                .then(Commands.literal("bell")
                        .then(Commands.literal("grant")
                                .executes(context -> grant(context.getSource()))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .requires(source -> source.hasPermission(2))
                                        .executes(context -> grantTargets(context.getSource(),
                                                EntityArgument.getPlayers(context, "targets")))))
                        .then(Commands.literal("revoke").executes(context -> revoke(context.getSource())))
                        .then(Commands.literal("status").executes(context -> status(context.getSource()))))
                .then(Commands.literal("bloodrain")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("start").executes(context -> bloodRain(context.getSource(), true)))
                        .then(Commands.literal("stop").executes(context -> bloodRain(context.getSource(), false))))
                .then(Commands.literal("class")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("class", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            builder.suggest("harbinger");
                                            builder.suggest("agent");
                                            builder.suggest("none");
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> setClass(context.getSource(),
                                                StringArgumentType.getString(context, "class")))))
                        .then(Commands.literal("reset").executes(context -> setClass(context.getSource(), "none"))))
                .then(Commands.literal("quest")
                        .then(Commands.literal("status").executes(context -> questStatus(context.getSource())))
                        .then(Commands.literal("complete")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> questComplete(context.getSource())))
                        .then(Commands.literal("reset")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> questReset(context.getSource()))))
                .then(Commands.literal("energy")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("fill").executes(context -> energyFill(context.getSource())))
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, BellTowerEnergy.MAX))
                                        .executes(context -> energySet(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "value")))))));
    }

    private static int grant(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!player.isCreative()) {
            source.sendFailure(Component.translatable("commands.btmi.bell.creative"));
            return 0;
        }
        BellTowerPermission.grant(player);
        source.sendSuccess(() -> Component.translatable("commands.btmi.bell.granted"), true);
        return 1;
    }

    private static int grantTargets(CommandSourceStack source, java.util.Collection<ServerPlayer> targets) {
        int granted = 0;
        for (ServerPlayer target : targets) {
            if (!target.isCreative()) {
                source.sendFailure(Component.translatable("commands.btmi.bell.target_creative", target.getDisplayName()));
                continue;
            }
            BellTowerPermission.grant(target);
            granted++;
        }
        if (granted > 0) {
            int count = granted;
            source.sendSuccess(() -> Component.translatable("commands.btmi.bell.granted_count", count), true);
        }
        return granted;
    }

    private static int revoke(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BellTowerPermission.revoke(player);
        source.sendSuccess(() -> Component.translatable("commands.btmi.bell.revoked"), true);
        return 1;
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean granted = BellTowerPermission.has(player);
        source.sendSuccess(() -> Component.translatable(
                granted ? "commands.btmi.bell.status.on" : "commands.btmi.bell.status.off"), false);
        return granted ? 1 : 0;
    }

    private static int bloodRain(CommandSourceStack source, boolean start) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (start) {
            BloodRainHandler.start(player.serverLevel());
            source.sendSuccess(() -> Component.translatable("commands.btmi.bloodrain.started"), true);
            return 1;
        }
        if (!BloodRainHandler.stop(player.serverLevel())) {
            source.sendFailure(Component.translatable("commands.btmi.bloodrain.inactive"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.btmi.bloodrain.stopped"), true);
        return 1;
    }

    private static int setClass(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BellTowerClass choice = BellTowerClass.byId(id);
        BellTowerClasses.set(player, choice);
        BellTowerSkillManager.sendState(player);
        source.sendSuccess(() -> Component.translatable("commands.btmi.class.set",
                Component.translatable(choice.translationKey())), true);
        return 1;
    }

    private static int questStatus(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BellTowerQuests.sendStatus(player);
        source.sendSuccess(() -> Component.translatable("commands.btmi.quest.status"), false);
        return 1;
    }

    private static int questReset(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BellTowerQuests.reset(player);
        source.sendSuccess(() -> Component.translatable("commands.btmi.quest.reset"), true);
        return 1;
    }

    private static int questComplete(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BellTowerQuests.completeAll(player);
        source.sendSuccess(() -> Component.translatable("commands.btmi.quest.complete"), true);
        return 1;
    }

    private static int energyFill(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!BellTowerEnergy.set(player, BellTowerEnergy.MAX)) {
            source.sendFailure(Component.translatable("commands.btmi.energy.no_item"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.btmi.energy.set", BellTowerEnergy.MAX), true);
        return 1;
    }

    private static int energySet(CommandSourceStack source, int value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!BellTowerEnergy.set(player, value)) {
            source.sendFailure(Component.translatable("commands.btmi.energy.no_item"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.btmi.energy.set", value), true);
        return 1;
    }
}
