package com.example.chipper_chopper;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.server.command.CommandManager.literal;

public class ChipperChopperMod implements ModInitializer {
    public static final String MOD_ID = "chipper-chopper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Chipper Chopper Mod!");

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("chipper")
                .executes(context -> {
                    // Default action when just "/chipper" is used - toggle the AI
                    ServerCommandSource source = context.getSource();
                    if (source.getEntity() instanceof ServerPlayerEntity player) {
                        if (TreeChopperAI.isActive(player)) {
                            TreeChopperAI.stop(player);
                            source.sendFeedback(() -> Text.literal("§cChipper Chopper AI deactivated!"), false);
                        } else {
                            TreeChopperAI.start(player);
                            source.sendFeedback(() -> Text.literal("§aChipper Chopper AI activated!"), false);
                        }
                        return 1;
                    }
                    return 0;
                })
                .then(literal("start")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        if (source.getEntity() instanceof ServerPlayerEntity player) {
                            TreeChopperAI.start(player);
                            source.sendFeedback(() -> Text.literal("§aChipper Chopper AI activated!"), false);
                            return 1;
                        }
                        return 0;
                    })
                )
                .then(literal("stop")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        if (source.getEntity() instanceof ServerPlayerEntity player) {
                            TreeChopperAI.stop(player);
                            source.sendFeedback(() -> Text.literal("§cChipper Chopper AI deactivated!"), false);
                            return 1;
                        }
                        return 0;
                    })
                )
                .then(literal("toggle")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        if (source.getEntity() instanceof ServerPlayerEntity player) {
                            if (TreeChopperAI.isActive(player)) {
                                TreeChopperAI.stop(player);
                                source.sendFeedback(() -> Text.literal("§cChipper Chopper AI deactivated!"), false);
                            } else {
                                TreeChopperAI.start(player);
                                source.sendFeedback(() -> Text.literal("§aChipper Chopper AI activated!"), false);
                            }
                            return 1;
                        }
                        return 0;
                    })
                )
                .then(literal("status")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        if (source.getEntity() instanceof ServerPlayerEntity player) {
                            boolean active = TreeChopperAI.isActive(player);
                            String status = active ? "§aACTIVE" : "§cINACTIVE";
                            source.sendFeedback(() -> Text.literal("Chipper Chopper AI status: " + status), false);
                            return 1;
                        }
                        return 0;
                    })
                )
            );
        });

        // Register server tick event for AI processing
        ServerTickEvents.END_SERVER_TICK.register(TreeChopperAI::tick);

        LOGGER.info("Chipper Chopper Mod initialized successfully!");
    }
} 