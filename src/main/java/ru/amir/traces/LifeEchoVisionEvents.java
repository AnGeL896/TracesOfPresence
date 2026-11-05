package ru.amir.traces;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Plays a brief vision the first time a player passes through each Life Echo. */
@Mod.EventBusSubscriber(modid = TracesOfPresence.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LifeEchoVisionEvents {
    private static final String SEEN_KEY = "traces_seen_life_echo_crystals";
    private static final String PENDING_KEY = "traces_pending_life_echo_vision";
    private static final int BLINDNESS_TICKS = 40;
    private static final double SPAWN_DISTANCE = 7.5D;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        CompoundTag data = player.getPersistentData();
        if (data.contains(PENDING_KEY)) {
            CompoundTag pending = data.getCompound(PENDING_KEY);
            if (!level.dimension().location().toString().equals(pending.getString("dimension"))) {
                data.remove(PENDING_KEY);
                return;
            }
            int ticksLeft = pending.getInt("ticks") - 1;
            if (ticksLeft > 0) {
                pending.putInt("ticks", ticksLeft);
                return;
            }

            data.remove(PENDING_KEY);
            ApparitionEntity apparition = ModEntities.APPARITION.get().create(level);
            if (apparition != null) {
                double playerX = pending.getDouble("playerX");
                double playerY = pending.getDouble("playerY");
                double playerZ = pending.getDouble("playerZ");
                float yaw = pending.getFloat("yaw");

                double radians = Math.toRadians(yaw);
                double forwardX = -Math.sin(radians);
                double forwardZ = Math.cos(radians);
                double targetX = playerX + forwardX * SPAWN_DISTANCE;
                double targetZ = playerZ + forwardZ * SPAWN_DISTANCE;

                BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        BlockPos.containing(targetX, playerY, targetZ));
                BlockPos spawnPos = surface.above();

                if (!level.isEmptyBlock(spawnPos) || !level.isEmptyBlock(spawnPos.above())) {
                    BlockPos fallback = findSafeSpawn(level, targetX, targetZ, surface);
                    if (fallback != null) {
                        spawnPos = fallback;
                    }
                }

                apparition.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                        yaw, 0.0F);
                apparition.setPurpleEcho();
                level.addFreshEntity(apparition);
            }
            return;
        }

        AABB playerBounds = player.getBoundingBox();
        CompoundTag seen = data.getCompound(SEEN_KEY);
        for (int x = Mth.floor(playerBounds.minX); x <= Mth.floor(playerBounds.maxX); x++) {
            for (int y = Mth.floor(playerBounds.minY); y <= Mth.floor(playerBounds.maxY); y++) {
                for (int z = Mth.floor(playerBounds.minZ); z <= Mth.floor(playerBounds.maxZ); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).is(ModBlocks.LIFE_ECHO.get())) continue;
                    if (!playerBounds.intersects(new AABB(pos))) continue;

                    String crystalKey = level.dimension().location() + "|" + pos.asLong();
                    if (seen.getBoolean(crystalKey)) continue;
                    seen.putBoolean(crystalKey, true);
                    data.put(SEEN_KEY, seen);

                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,
                            BLINDNESS_TICKS, 0, false, false, false));
                    player.displayClientMessage(Component.translatable("message.traces.life_echo_who"), true);

                    CompoundTag pending = new CompoundTag();
                    pending.putInt("ticks", BLINDNESS_TICKS);
                    pending.putString("dimension", level.dimension().location().toString());
                    pending.putDouble("playerX", player.getX());
                    pending.putDouble("playerY", player.getY());
                    pending.putDouble("playerZ", player.getZ());
                    pending.putFloat("yaw", player.getYRot());
                    data.put(PENDING_KEY, pending);
                    return;
                }
            }
        }
    }

    private static BlockPos findSafeSpawn(ServerLevel level, double targetX, double targetZ, BlockPos surface) {
        for (int radius = 1; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    BlockPos checkPos = surface.offset(dx, 0, dz).above();
                    if (level.isEmptyBlock(checkPos) && level.isEmptyBlock(checkPos.above())) {
                        return checkPos;
                    }
                }
            }
        }
        return null;
    }

    private LifeEchoVisionEvents() { }
}
