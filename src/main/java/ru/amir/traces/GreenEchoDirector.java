package ru.amir.traces;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TracesOfPresence.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GreenEchoDirector {
    private static final int CHECK_INTERVAL_TICKS = 40;
    private static final int WORK_LOOKBACK_TICKS = 5 * 60 * 20;
    private static final int MIN_IDLE_TICKS = 80;
    private static final int MIN_ACTIONS = 3;
    private static final int MAX_REPLAY_ACTIONS = 12;
    private static final String COOLDOWN_KEY = "traces_green_echo_next_spawn";

    private GreenEchoDirector() { }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || !TracesConfig.GREEN_ECHO_ENABLED.get()
                || level.getGameTime() % CHECK_INTERVAL_TICKS != 0L) return;

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isSleeping()
                    || level.getGameTime() < player.getPersistentData().getLong(COOLDOWN_KEY)) continue;
            if (!level.getEntitiesOfClass(ApparitionEntity.class,
                    player.getBoundingBox().inflate(48.0D)).isEmpty()) continue;

            ListTag replay = collectNearbyWork(player, level.getGameTime());
            if (replay.size() < MIN_ACTIONS || level.random.nextDouble() >= TracesConfig.GREEN_CHANCE_PER_CHECK.get()) continue;

            ApparitionEntity echo = ModEntities.APPARITION.get().create(level);
            if (echo == null || !echo.setWorkReplay(replay)) continue;
            if (!level.addFreshEntity(echo)) continue;

            player.getPersistentData().putLong(COOLDOWN_KEY, level.getGameTime() + TracesConfig.GREEN_COOLDOWN_TICKS.get());
            Vec3 at = echo.position().add(0.0D, 0.9D, 0.0D);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    at.x, at.y, at.z, 12, 0.35D, 0.65D, 0.35D, 0.015D);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    at.x, at.y, at.z, 5, 0.22D, 0.5D, 0.22D, 0.01D);
            level.playSound(null, echo.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.AMBIENT, 0.45F, 1.18F + level.random.nextFloat() * 0.18F);

            String lastType = replay.getCompound(replay.size() - 1).getString("type");
            String key = switch (lastType) {
                case "break" -> "message.traces.green_echo_mining";
                case "place" -> "message.traces.green_echo_building";
                default -> "message.traces.green_echo_crafting";
            };
            player.displayClientMessage(Component.translatable(key), true);
        }
    }

    private static ListTag collectNearbyWork(ServerLevel player, long now) {
        ListTag saved = EchoRecorder.getRecentWorkActions(player);
        ListTag nearby = new ListTag();
        long newestAction = Long.MIN_VALUE;
        CompoundTag newest = null;
        Vec3 playerPos = player.position();
        String dimension = player.level().dimension().location().toString();

        for (int i = 0; i < saved.size(); i++) {
            CompoundTag action = saved.getCompound(i);
            long time = action.getLong("time");
            if (now - time > WORK_LOOKBACK_TICKS
                    || !dimension.equals(action.getString("dimension"))) continue;
            double dx = action.getDouble("actor_x") - playerPos.x;
            double dz = action.getDouble("actor_z") - playerPos.z;
            if (dx * dx + dz * dz > 20.0D * 20.0D) continue;
            nearby.add(action.copy());
            if (time > newestAction) {
                newestAction = time;
                newest = action;
            }
        }

        if (newest == null || now - newestAction < MIN_IDLE_TICKS) return new ListTag();

        // Keep the memory centered on the most recent work site so separate
        // projects visited during the lookback window do not become one route.
        double centerX = newest.getDouble("actor_x");
        double centerY = newest.getDouble("actor_y");
        double centerZ = newest.getDouble("actor_z");
        ListTag cluster = new ListTag();
        for (int i = 0; i < nearby.size(); i++) {
            CompoundTag action = nearby.getCompound(i);
            double dx = action.getDouble("actor_x") - centerX;
            double dy = action.getDouble("actor_y") - centerY;
            double dz = action.getDouble("actor_z") - centerZ;
            if (dx * dx + dz * dz <= 12.0D * 12.0D && Math.abs(dy) <= 6.0D) {
                cluster.add(action.copy());
            }
        }
        if (cluster.size() < MIN_ACTIONS) return new ListTag();
        while (cluster.size() > MAX_REPLAY_ACTIONS) cluster.remove(0);
        return cluster;
    }
}
