package ru.amir.traces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/** Starts a saved death replay when its owner returns to the place where they died. */
@Mod.EventBusSubscriber(modid = TracesOfPresence.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DeathEchoDirector {
    private static final String TRIGGER_TICK_KEY = "traces_death_echo_trigger_tick";
    private static final int APPROACH_RADIUS = 12;
    private static final int PRELUDE_TICKS = 40;
    private static final DustParticleOptions RED_GLOW =
            new DustParticleOptions(new Vector3f(0.95F, 0.035F, 0.02F), 1.1F);

    private DeathEchoDirector() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) return;
        if (level.getGameTime() % 5L != 0L) return;

        CompoundTag memory = EchoRecorder.getDeathMemory(player);
        if (!memory.contains("frames", Tag.TAG_LIST)
                || memory.getList("frames", Tag.TAG_COMPOUND).size() < 2) return;
        if (!memory.getString("dimension").equals(level.dimension().location().toString())) return;

        CompoundTag playerData = player.getPersistentData();
        long triggerTick = playerData.getLong(TRIGGER_TICK_KEY);
        BlockPos deathPos = BlockPos.containing(memory.getDouble("death_x"),
                memory.getDouble("death_y"), memory.getDouble("death_z"));

        if (triggerTick <= 0L) {
            if (player.distanceToSqr(Vec3.atCenterOf(deathPos)) > APPROACH_RADIUS * APPROACH_RADIUS) return;
            triggerTick = level.getGameTime() + PRELUDE_TICKS;
            playerData.putLong(TRIGGER_TICK_KEY, triggerTick);
        }

        if (level.getGameTime() < triggerTick) {
            level.sendParticles(RED_GLOW, deathPos.getX() + 0.5D, deathPos.getY() + 0.08D,
                    deathPos.getZ() + 0.5D, 4, 0.55D, 0.025D, 0.55D, 0.01D);
            return;
        }

        ApparitionEntity replay = ModEntities.APPARITION.get().create(level);
        if (replay == null || !replay.setDeathReplay(memory)) return;
        replay.setNoGravity(true);
        if (!level.addFreshEntity(replay)) return;

        EchoRecorder.clearDeathMemory(player);
        playerData.remove(TRIGGER_TICK_KEY);
    }
}
