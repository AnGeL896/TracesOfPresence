package ru.amir.traces;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Plays a brief vision the first time a player passes through each Life Echo. */
@Mod.EventBusSubscriber(modid = TracesOfPresence.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LifeEchoVisionEvents {
    private static final String SEEN_KEY = "traces_seen_life_echo_crystals";
    private static final String PENDING_KEY = "traces_pending_life_echo_vision";
    private static final int BLINDNESS_TICKS = 20;

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
                apparition.moveTo(pending.getDouble("x"), pending.getDouble("y"), pending.getDouble("z"),
                        player.getYRot(), 0.0F);
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
                    pending.putDouble("x", pos.getX() + 0.5D);
                    pending.putDouble("y", pos.getY());
                    pending.putDouble("z", pos.getZ() + 0.5D);
                    data.put(PENDING_KEY, pending);
                    return;
                }
            }
        }
    }

    private LifeEchoVisionEvents() { }
}
