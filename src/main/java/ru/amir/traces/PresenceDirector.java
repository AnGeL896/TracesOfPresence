package ru.amir.traces;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TracesOfPresence.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PresenceDirector {
    private static final int CHECK_INTERVAL_TICKS = 100;
    private static final int SPAWN_COOLDOWN_TICKS = 400; // Temporary test cooldown: 20 seconds.
    private static final float SPAWN_CHANCE_PER_CHECK = 0.50F; // Temporary test chance: 50% every 5 seconds.
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
        long timeOfDay = level.getDayTime() % 24_000L;
        return timeOfDay >= 13_000L && timeOfDay < 23_000L;
    }

    private static boolean isSuitableBiome(ServerLevel level, BlockPos position) {
        var biome = level.getBiome(position);
        return biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA)
                || biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP);
    }

    private static boolean trySpawn(ServerLevel level, ServerPlayer player) {
        if (!level.getEntitiesOfClass(ApparitionEntity.class, player.getBoundingBox().inflate(48.0D)).isEmpty()) {
            return false;
        }

        for (int attempt = 0; attempt < 10; attempt++) {
            float yaw = player.getYRot() + (level.random.nextFloat() * 70.0F - 35.0F);
            EscapeSpot spot = findEscapeSpot(level, player, yaw);
            if (spot == null) continue;

            ApparitionEntity apparition = ModEntities.APPARITION.get().create(level);
            if (apparition == null) return false;
            apparition.moveTo(spot.startX(), spot.startY(), spot.startZ(), yaw, 0.0F);
            apparition.setDestination(spot.destination());
            if (!level.noCollision(apparition)) continue;

            return level.addFreshEntity(apparition);
        }
        return false;
    }

    private static EscapeSpot findEscapeSpot(ServerLevel level, ServerPlayer player, float yaw) {
        double radians = Math.toRadians(yaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        double searchDistance = 16.0D + level.random.nextDouble() * 7.0D;
        int centerX = (int) Math.floor(player.getX() + forwardX * searchDistance);
        int centerZ = (int) Math.floor(player.getZ() + forwardZ * searchDistance);

        for (int radius = 0; radius <= 5; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            new BlockPos(x, player.getBlockY(), z));

                    for (int y = surface.getY() - 1; y <= surface.getY() + 5; y++) {
                        BlockPos trunk = new BlockPos(x, y, z);
                        if (!level.getBlockState(trunk).is(BlockTags.LOGS)) continue;

                        EscapeSpot spot = makeEscapeSpot(level, player, trunk);
                        if (spot != null) return spot;
                    }
                }
            }
        }
        return null;
    }

    private static EscapeSpot makeEscapeSpot(ServerLevel level, ServerPlayer player, BlockPos trunk) {
        double treeX = trunk.getX() + 0.5D;
        double treeZ = trunk.getZ() + 0.5D;
        double towardX = treeX - player.getX();
        double towardZ = treeZ - player.getZ();
        double distance = Math.sqrt(towardX * towardX + towardZ * towardZ);
        if (distance < 8.0D) return null;
        towardX /= distance;
        towardZ /= distance;

        double startX = treeX - towardX * 3.25D;
        double startZ = treeZ - towardZ * 3.25D;
        double targetX = treeX + towardX * 3.75D;
        double targetZ = treeZ + towardZ * 3.75D;

        BlockPos startGround = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(startX, player.getY(), startZ));
        BlockPos targetGround = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(targetX, trunk.getY(), targetZ));
        if (Math.abs(startGround.getY() - targetGround.getY()) > 1) return null;
        if (!isSafeGround(level, startGround) || !isSafeGround(level, targetGround)) return null;

        Vec3 start = new Vec3(startX, startGround.getY(), startZ);
        Vec3 target = new Vec3(targetX, targetGround.getY() + 1.0D, targetZ);
        HitResult startSight = level.clip(new ClipContext(player.getEyePosition(), start.add(0.0D, 1.0D, 0.0D),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (startSight.getType() != HitResult.Type.MISS) return null;

        HitResult targetSight = level.clip(new ClipContext(player.getEyePosition(), target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (targetSight.getType() != HitResult.Type.BLOCK) return null;
        BlockPos cover = ((BlockHitResult) targetSight).getBlockPos();
        if (!level.getBlockState(cover).is(BlockTags.LOGS)
                && !level.getBlockState(cover).is(BlockTags.LEAVES)) return null;

        return new EscapeSpot(startX, startGround.getY(), startZ,
                BlockPos.containing(targetX, targetGround.getY(), targetZ));
    }

    private static boolean isSafeGround(ServerLevel level, BlockPos feet) {
        return level.getFluidState(feet).isEmpty()
                && level.getFluidState(feet.above()).isEmpty()
                && level.getBlockState(feet.below()).blocksMotion()
                && level.getMaxLocalRawBrightness(feet) <= 7;
    }

    private record EscapeSpot(double startX, double startY, double startZ, BlockPos destination) { }

    private PresenceDirector() { }
}
