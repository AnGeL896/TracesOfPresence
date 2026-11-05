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
    private static final String RETRY_CRYSTAL_KEY = "traces_life_echo_retry_crystal";
    private static final String RETRY_AFTER_KEY = "traces_life_echo_retry_after";
    private static final int BLINDNESS_TICKS = 40;
    private static final int SPAWN_RETRY_DELAY_TICKS = 100;
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
            if (apparition == null) {
                scheduleRetry(data, pending, level);
                return;
            }

            double playerX = pending.getDouble("playerX");
            double playerY = pending.getDouble("playerY");
            double playerZ = pending.getDouble("playerZ");
            float yaw = pending.getFloat("yaw");
            double radians = Math.toRadians(yaw);
            double targetX = playerX - Math.sin(radians) * SPAWN_DISTANCE;
            double targetZ = playerZ + Math.cos(radians) * SPAWN_DISTANCE;
            BlockPos spawnPos = findSafeSpawn(level, targetX, targetZ, playerY, apparition);
            if (spawnPos == null) {
                scheduleRetry(data, pending, level);
                return;
            }

            apparition.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                    yaw, 0.0F);
            apparition.setPurpleEcho();
            if (!level.addFreshEntity(apparition)) {
                scheduleRetry(data, pending, level);
                return;
            }

            CompoundTag seen = data.getCompound(SEEN_KEY);
            seen.putBoolean(pending.getString("crystal"), true);
            data.put(SEEN_KEY, seen);
            data.remove(RETRY_CRYSTAL_KEY);
            data.remove(RETRY_AFTER_KEY);
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
                    if (crystalKey.equals(data.getString(RETRY_CRYSTAL_KEY))
                            && level.getGameTime() < data.getLong(RETRY_AFTER_KEY)) continue;

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
                    pending.putString("crystal", crystalKey);
                    data.put(PENDING_KEY, pending);
                    return;
                }
            }
        }
    }

    private static BlockPos findSafeSpawn(ServerLevel level, double targetX, double targetZ,
                                          double playerY, ApparitionEntity apparition) {
        int centerX = Mth.floor(targetX);
        int centerZ = Mth.floor(targetZ);
        for (int radius = 0; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    BlockPos feet = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            BlockPos.containing(x, playerY, z));
                    BlockPos ground = feet.below();
                    BlockPos head = feet.above();
                    if (feet.getY() <= level.getMinBuildHeight()
                            || head.getY() >= level.getMaxBuildHeight()
                            || !level.getWorldBorder().isWithinBounds(feet)
                            || !level.getBlockState(ground).blocksMotion()
                            || !level.getFluidState(feet).isEmpty()
                            || !level.getFluidState(head).isEmpty()
                            || !level.isEmptyBlock(feet)
                            || !level.isEmptyBlock(head)) continue;

                    apparition.setPos(x + 0.5D, feet.getY(), z + 0.5D);
                    if (level.noCollision(apparition)) return feet;
                }
            }
        }
        return null;
    }

    private static void scheduleRetry(CompoundTag data, CompoundTag pending, ServerLevel level) {
        data.putString(RETRY_CRYSTAL_KEY, pending.getString("crystal"));
        data.putLong(RETRY_AFTER_KEY, level.getGameTime() + SPAWN_RETRY_DELAY_TICKS);
    }

    private LifeEchoVisionEvents() { }
}
