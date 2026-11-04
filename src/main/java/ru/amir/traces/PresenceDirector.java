Warning: truncated output (original token count: 1028)
Total output lines: 84

package ru.amir.traces;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TracesOfPresence.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PresenceDirector {
    private static final int CHECK_INTERVAL_TICKS = 100;
    private static final int SPAWN_COOLDOWN_TICKS = 1_800;
    private static final float SPAWN_CHANCE_PER_CHECK = 0.03F;
    private static final String COOLDOWN_KEY = "traces_apparition_next_spawn";

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD || level.getGameTime() % CHECK_INTERVAL_TICKS != 0) return;

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative() || player.isSleeping()) continue;
            if (level.getGameTime() < player.getPersistentData().getLong(COOLDOWN_KEY)) continue;
            if (!isNight(level) || !isSuitableBiome(level, player.blockPosition())) continue;
            if (level.random.nextFloat() >= SPAWN_CHANCE_PER_CHECK) continue;
            if (trySpawn(level, player)) {
                player.getPersistentData().putLong(COOLDOWN_KEY, level.getGameTime() + SPAWN_COOLDOWN_TICKS);
            }
        }
    }

    private static boolean isNight(ServerLevel level) {
        long timeOfDay = level.getDayTime() %…28 tokens truncated…tableBiome(ServerLevel level, BlockPos position) {
        var biome = level.getBiome(position);
        return biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA)
                || biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP);
    }

    private static boolean trySpawn(ServerLevel level, ServerPlayer player) {
        if (!level.getEntitiesOfClass(ApparitionEntity.class, player.getBoundingBox().inflate(48.0D)).isEmpty()) return false;

        for (int attempt = 0; attempt < 8; attempt++) {
            float yaw = player.getYRot() + (level.random.nextFloat() * 110.0F - 55.0F);
            double distance = 12.0D + level.random.nextDouble() * 12.0D;
            double radians = Math.toRadians(yaw);
            double x = player.getX() - Math.sin(radians) * distance;
            double z = player.getZ() + Math.cos(radians) * distance;
            BlockPos sample = BlockPos.containing(x, player.getY(), z);
            BlockPos feet = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sample);

            if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) continue;
            if (!level.getBlockState(feet.below()).blocksMotion()) continue;
            if (level.getMaxLocalRawBrightness(feet) > 7) continue;

            ApparitionEntity apparition = ModEntities.APPARITION.get().create(level);
            if (apparition == null) return false;
            apparition.moveTo(x, feet.getY(), z, yaw, 0.0F);
            if (!level.noCollision(apparition)) continue;

            Vec3 target = apparition.position().add(0.0D, 1.0D, 0.0D);
            HitResult sight = level.clip(new ClipContext(player.getEyePosition(), target,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (sight.getType() != HitResult.Type.MISS) continue;

            return level.addFreshEntity(apparition);
        }
        return false;
    }

    private PresenceDirector() { }
}
