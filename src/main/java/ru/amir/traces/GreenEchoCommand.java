package ru.amir.traces;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Operator-only, world-safe visual test for the green work echo. */
@Mod.EventBusSubscriber(modid = TracesOfPresence.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GreenEchoCommand {
    private GreenEchoCommand() { }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("traces")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("test")
                        .then(Commands.literal("green").executes(context -> spawnTest(context.getSource())))));
    }

    private static int spawnTest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        if (!level.getEntitiesOfClass(ApparitionEntity.class,
                player.getBoundingBox().inflate(48.0D)).isEmpty()) {
            source.sendFailure(Component.translatable("message.traces.green_echo_test_busy"));
            return 0;
        }

        BlockPos base = player.blockPosition();
        ListTag actions = new ListTag();
        addAction(actions, level.getGameTime(), "break", "minecraft:stone",
                "minecraft:iron_pickaxe", player, base.offset(-2, 0, 0), -2.5D, -1.5D);
        addAction(actions, level.getGameTime() + 1, "break", "minecraft:coal_ore",
                "minecraft:iron_pickaxe", player, base.offset(-1, 0, 0), -1.5D, -0.5D);
        addAction(actions, level.getGameTime() + 2, "break", "minecraft:copper_ore",
                "minecraft:iron_pickaxe", player, base, -0.5D, 0.5D);
        addAction(actions, level.getGameTime() + 3, "place", "minecraft:oak_planks",
                "minecraft:oak_planks", player, base.offset(1, 0, 0), 0.5D, 0.5D);
        addAction(actions, level.getGameTime() + 4, "craft", "minecraft:crafting_table",
                "minecraft:crafting_table", player, base.offset(1, 0, 1), 1.5D, 1.0D);

        ApparitionEntity echo = ModEntities.APPARITION.get().create(level);
        if (echo == null || !echo.setWorkReplay(actions)) return 0;
        echo.moveTo(player.getX() - 2.5D, player.getY(), player.getZ() - 1.5D,
                player.getYRot(), 0.0F);
        if (!level.addFreshEntity(echo)) return 0;

        player.displayClientMessage(Component.translatable("message.traces.green_echo_test_spawned"), true);
        return 1;
    }

    private static void addAction(ListTag actions, long time, String type, String id, String heldItem,
                                  ServerPlayer player, BlockPos target, double actorOffsetX, double actorOffsetZ) {
        CompoundTag action = new CompoundTag();
        action.putLong("time", time);
        action.putString("dimension", player.level().dimension().location().toString());
        action.putString("type", type);
        action.putString("id", id);
        action.putInt("x", target.getX());
        action.putInt("y", target.getY());
        action.putInt("z", target.getZ());
        action.putDouble("actor_x", player.getX() + actorOffsetX);
        action.putDouble("actor_y", player.getY());
        action.putDouble("actor_z", player.getZ() + actorOffsetZ);
        action.putFloat("yaw", player.getYRot());
        action.putString("main_hand", heldItem);
        action.putString("off_hand", "");
        actions.add(action);
    }
}
