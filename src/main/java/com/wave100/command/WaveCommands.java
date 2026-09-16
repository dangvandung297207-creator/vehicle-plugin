package com.wave100.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wave100.WaveConfig;
import com.wave100.entity.WaveMotorcycleEntity;
import com.wave100.registry.WaveEntities;
import com.wave100.registry.WaveItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * /wave commands: give, spawn, remove, removeall, info, reload, lock, unlock.
 *
 * <p>Admin commands require permission level 2 (3 for removeall); lock/unlock
 * work for the owner of the nearest motorcycle.</p>
 */
public final class WaveCommands {

    private static final double SEARCH_RANGE = 8.0;

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("wave")
                .then(Commands.literal("give")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> give(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> give(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("spawn")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> spawn(ctx.getSource())))
                .then(Commands.literal("remove")
                        .requires(src -> src.hasPermission(0))
                        .executes(ctx -> remove(ctx.getSource())))
                .then(Commands.literal("removeall")
                        .requires(src -> src.hasPermission(3))
                        .executes(ctx -> removeAll(ctx.getSource())))
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource())))
                .then(Commands.literal("reload")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> reload(ctx.getSource())))
                .then(Commands.literal("lock")
                        .executes(ctx -> setLock(ctx.getSource(), true)))
                .then(Commands.literal("unlock")
                        .executes(ctx -> setLock(ctx.getSource(), false)))
                .then(Commands.literal("fuel")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> setFuel(ctx.getSource(), 100))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 10000))
                                .executes(ctx -> setFuel(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "amount"))))));
    }

    // ------------------------------------------------------------------
    // implementations
    // ------------------------------------------------------------------

    private static int give(CommandSourceStack source, ServerPlayer player) {
        player.getInventory().add(new ItemStack(WaveItems.WAVE_MOTORCYCLE.get()));
        source.sendSuccess(() -> Component.translatable("commands.wave100.give", player.getDisplayName()), true);
        return 1;
    }

    private static int spawn(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        Vec3 pos = player.position();
        float yaw = player.getYRot();

        WaveMotorcycleEntity bike = new WaveMotorcycleEntity(WaveEntities.WAVE_MOTORCYCLE.get(), level);
        bike.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        bike.setYRot(yaw);
        bike.yRotO = yaw;
        bike.setOwner(player.getUUID());
        bike.setFuel((float) WaveConfig.Fuel.capacity());
        bike.setHealth((float) WaveConfig.Damage.maxHealth());

        if (!level.noCollision(bike, bike.getBoundingBox())) {
            source.sendFailure(Component.translatable("commands.wave100.spawn_blocked"));
            return 0;
        }
        level.addFreshEntity(bike);
        level.playSound(null, pos.x, pos.y, pos.z,
                com.wave100.registry.WaveSounds.REPAIR.get(), SoundSource.BLOCKS, 0.9F, 0.7F);
        source.sendSuccess(() -> Component.translatable("commands.wave100.spawned"), true);
        return 1;
    }

    private static int remove(CommandSourceStack source) throws CommandSyntaxException {
        WaveMotorcycleEntity bike = findTarget(source);
        if (bike == null) {
            source.sendFailure(Component.translatable("commands.wave100.not_found"));
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        if (!bike.isOwner(player) && !source.hasPermission(2)) {
            source.sendFailure(Component.translatable("commands.wave100.not_yours"));
            return 0;
        }
        bike.discard();
        bike.spawnAtLocation(new ItemStack(WaveItems.WAVE_MOTORCYCLE.get()));
        source.sendSuccess(() -> Component.translatable("commands.wave100.removed"), true);
        return 1;
    }

    private static int removeAll(CommandSourceStack source) {
        int count = 0;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            List<WaveMotorcycleEntity> bikes = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof WaveMotorcycleEntity bike && bike.isAlive()) {
                    bikes.add(bike);
                }
            }
            for (WaveMotorcycleEntity bike : bikes) {
                bike.discard();
                count++;
            }
        }
        int finalCount = count;
        source.sendSuccess(() -> Component.translatable("commands.wave100.removed_all", finalCount), true);
        return count;
    }

    private static int info(CommandSourceStack source) {
        WaveMotorcycleEntity bike = findTarget(source);
        if (bike == null) {
            source.sendFailure(Component.translatable("commands.wave100.not_found"));
            return 0;
        }
        bike.sendInfoTo(source);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        // values are read live from the config spec; NeoForge reloads the TOML
        // automatically when it changes, so this just confirms
        source.sendSuccess(() -> Component.translatable("commands.wave100.reloaded"), true);
        return 1;
    }

    private static int setLock(CommandSourceStack source, boolean locked) throws CommandSyntaxException {
        WaveMotorcycleEntity bike = findTarget(source);
        if (bike == null) {
            source.sendFailure(Component.translatable("commands.wave100.not_found"));
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        if (!bike.isOwner(player) && !source.hasPermission(2)) {
            source.sendFailure(Component.translatable("commands.wave100.not_yours"));
            return 0;
        }
        bike.setLocked(locked);
        source.sendSuccess(() -> Component.translatable(
                locked ? "commands.wave100.locked" : "commands.wave100.unlocked"), true);
        return 1;
    }

    private static int setFuel(CommandSourceStack source, int amount) {
        WaveMotorcycleEntity bike = findTarget(source);
        if (bike == null) {
            source.sendFailure(Component.translatable("commands.wave100.not_found"));
            return 0;
        }
        bike.setFuel(amount);
        source.sendSuccess(() -> Component.translatable("commands.wave100.fuel_set", amount), true);
        return 1;
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /** The motorcycle the source player is riding, else the nearest one. */
    @Nullable
    private static WaveMotorcycleEntity findTarget(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            if (player.getVehicle() instanceof WaveMotorcycleEntity ridden) {
                return ridden;
            }
        } catch (CommandSyntaxException ignored) {
        }
        Vec3 pos = source.getPosition();
        AABB box = new AABB(pos.add(-SEARCH_RANGE, -SEARCH_RANGE, -SEARCH_RANGE),
                pos.add(SEARCH_RANGE, SEARCH_RANGE, SEARCH_RANGE));
        WaveMotorcycleEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (WaveMotorcycleEntity bike : source.getLevel()
                .getEntitiesOfClass(WaveMotorcycleEntity.class, box)) {
            double d = bike.distanceToSqr(pos);
            if (d < bestDist) {
                bestDist = d;
                best = bike;
            }
        }
        return best;
    }

    private WaveCommands() {
    }
}
