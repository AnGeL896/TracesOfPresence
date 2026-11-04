package ru.amir.traces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** A harmless apparition that walks toward cover before fading away. */
public final class ApparitionEntity extends Entity {
    private static final int APPEAR_TICKS = 16;
    private static final int WALK_START_TICK = 30;
    private static final int FLARE_DURATION_TICKS = 12;
    private static final int VANISH_DURATION_TICKS = 18;
    private static final int MAX_AGE_TICKS = 220;
    private static final double MAX_WALK_SPEED = 0.05D;
    private static final double WALK_ACCELERATION = 0.0025D;
    private static final double WALK_DECELERATION = 0.0035D;
    private static final double ARRIVAL_DISTANCE = 0.55D;
    private static final double REPLAY_TIME_SCALE = 1.0D;
    private static final int DEATH_CUE_TICKS = 40;

    private static final EntityDataAccessor<BlockPos> DESTINATION =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Boolean> HAS_DESTINATION =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WALKING =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> WALK_BLEND =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> FLARE_TICK =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VANISH_TICK =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DEATH_REPLAY =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> REPLAY_CROUCHING =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> REPLAY_HEAD_YAW =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> REPLAY_LIMB_PHASE =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> REPLAY_SPRINTING =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> GREEN_ECHO =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> KILLER_TYPE =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> KILLER_X =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> KILLER_Y =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> KILLER_Z =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> KILLER_YAW =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DEATH_CUE_TICK =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.INT);

    private double currentWalkSpeed;
    private float previousWalkBlend;
    private float previousReplayLimbPhase;
    private float replayCrouchBlend;
    private float previousReplayCrouchBlend;
    private float replaySprintBlend;
    private float previousReplaySprintBlend;
    private List<ReplayFrame> replayFrames = List.of();
    private double replayElapsedTime;
    private boolean replayMessageSent;
    private boolean killerMessageSent;

    public ApparitionEntity(EntityType<? extends ApparitionEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DESTINATION, BlockPos.ZERO);
        entityData.define(HAS_DESTINATION, false);
        entityData.define(WALKING, false);
        entityData.define(WALK_BLEND, 0.0F);
        entityData.define(FLARE_TICK, -1);
        entityData.define(VANISH_TICK, -1);
        entityData.define(DEATH_REPLAY, false);
        entityData.define(REPLAY_CROUCHING, false);
        entityData.define(REPLAY_HEAD_YAW, 0.0F);
        entityData.define(REPLAY_LIMB_PHASE, 0.0F);
        entityData.define(REPLAY_SPRINTING, false);
        entityData.define(GREEN_ECHO, false);
        entityData.define(KILLER_TYPE, "");
        entityData.define(KILLER_X, 0.0F);
        entityData.define(KILLER_Y, 0.0F);
        entityData.define(KILLER_Z, 0.0F);
        entityData.define(KILLER_YAW, 0.0F);
        entityData.define(DEATH_CUE_TICK, -1);
    }

    public void setDestination(BlockPos destination) {
        entityData.set(DESTINATION, destination.immutable());
        entityData.set(HAS_DESTINATION, true);
    }

    public boolean isWalking() {
        return entityData.get(WALKING);
    }

    public boolean isDeathReplay() {
        return entityData.get(DEATH_REPLAY);
    }

    public float getReplayCrouchBlend(float partialTick) {
        return Mth.lerp(partialTick, previousReplayCrouchBlend, replayCrouchBlend);
    }

    public float getReplaySprintBlend(float partialTick) {
        return Mth.lerp(partialTick, previousReplaySprintBlend, replaySprintBlend);
    }

    public float getReplayHeadYaw() {
        return entityData.get(REPLAY_HEAD_YAW);
    }

    public float getReplayLimbPhase(float partialTick) {
        return Mth.lerp(partialTick, previousReplayLimbPhase, entityData.get(REPLAY_LIMB_PHASE));
    }

    public boolean isReplaySprinting() {
        return entityData.get(REPLAY_SPRINTING);
    }

    public String getKillerType() { return entityData.get(KILLER_TYPE); }
    public float getKillerX() { return entityData.get(KILLER_X); }
    public float getKillerY() { return entityData.get(KILLER_Y); }
    public float getKillerZ() { return entityData.get(KILLER_Z); }
    public float getKillerYaw() { return entityData.get(KILLER_YAW); }
    public float getDeathCueTime(float partialTick) {
        int start = entityData.get(DEATH_CUE_TICK);
        return start < 0 ? -1.0F : tickCount + partialTick - start;
    }
    public boolean isCreeperDeath() { return "minecraft:creeper".equals(getKillerType()); }

    public boolean isGreenEcho() {
        return entityData.get(GREEN_ECHO);
    }

    public void setGreenEcho() {
        entityData.set(GREEN_ECHO, true);
    }

    public boolean setDeathReplay(CompoundTag memory) {
        ListTag frames = memory.getList("frames", Tag.TAG_COMPOUND);
        if (frames.size() < 2) return false;
        setReplayFrames(frames);
        ReplayFrame first = replayFrames.get(0);
        setPos(first.x(), first.y(), first.z());
        setYRot(first.yaw());
        setXRot(first.pitch());
        entityData.set(REPLAY_HEAD_YAW, first.headYaw());
        entityData.set(REPLAY_CROUCHING, first.crouching());
        entityData.set(REPLAY_LIMB_PHASE, 0.0F);
        entityData.set(REPLAY_SPRINTING, first.sprinting());
        entityData.set(GREEN_ECHO, false);
        entityData.set(KILLER_TYPE, memory.getString("attacker_type"));
        entityData.set(KILLER_X, (float) memory.getDouble("attacker_x"));
        entityData.set(KILLER_Y, (float) memory.getDouble("attacker_y"));
        entityData.set(KILLER_Z, (float) memory.getDouble("attacker_z"));
        entityData.set(KILLER_YAW, memory.getFloat("attacker_yaw"));
        entityData.set(DEATH_CUE_TICK, -1);
        entityData.set(DEATH_REPLAY, true);
        replayElapsedTime = 0.0D;
        return true;
    }

    private void setReplayFrames(ListTag frames) {
        List<ReplayFrame> loaded = new ArrayList<>(frames.size());
        for (int i = 0; i < frames.size(); i++) {
            CompoundTag frame = frames.getCompound(i);
            loaded.add(new ReplayFrame(frame.getLong("time"), frame.getDouble("x"),
                    frame.getDouble("y"), frame.getDouble("z"), frame.getFloat("yaw"),
                    frame.getFloat("pitch"), frame.getFloat("head_yaw"), frame.getBoolean("crouching"),
                    frame.getBoolean("sprinting")));
        }
        replayFrames = loaded;
    }

    public float getWalkBlend(float partialTick) {
        return Mth.lerp(partialTick, previousWalkBlend, entityData.get(WALK_BLEND));
    }

    private void updateWalkBlend(float target) {
        float current = entityData.get(WALK_BLEND);
        float next = Mth.approach(current, target, 0.065F);
        entityData.set(WALK_BLEND, next);
        entityData.set(WALKING, next > 0.02F);
    }

    private boolean isDissolving() {
        return entityData.get(VANISH_TICK) >= 0;
    }

    @Override
    public void tick() {
        previousWalkBlend = entityData.get(WALK_BLEND);
        previousReplayLimbPhase = entityData.get(REPLAY_LIMB_PHASE);
        previousReplayCrouchBlend = replayCrouchBlend;
        previousReplaySprintBlend = replaySprintBlend;
        super.tick();
        float targetCrouch = entityData.get(REPLAY_CROUCHING) ? 1.0F : 0.0F;
        float targetSprint = entityData.get(REPLAY_SPRINTING) ? 1.0F : 0.0F;
        replayCrouchBlend = Mth.approach(replayCrouchBlend, targetCrouch, 0.12F);
        replaySprintBlend = Mth.approach(replaySprintBlend, targetSprint, 0.12F);

        if (!level().isClientSide) {
            tickServer();
        } else if (getGlowIntensity(0.0F) > 0.02F) {
            for (int i = 0; i < 4; i++) {
                double x = getX() + (random.nextDouble() - 0.5D) * 0.75D;
                double y = getY() + 0.15D + random.nextDouble() * 1.55D;
                double z = getZ() + (random.nextDouble() - 0.5D) * 0.75D;
                level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z,
                        (random.nextDouble() - 0.5D) * 0.018D,
                        0.015D + random.nextDouble() * 0.025D,
                        (random.nextDouble() - 0.5D) * 0.018D);
            }
            if (tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.9D, getZ(),
                        (random.nextDouble() - 0.5D) * 0.025D, 0.025D,
                        (random.nextDouble() - 0.5D) * 0.025D);
            }
        } else if (tickCount % 5 == 0 && random.nextFloat() < 0.55F) {
            double x = getX() + (random.nextDouble() - 0.5D) * 0.45D;
            double y = getY() + 0.2D + random.nextDouble() * 1.35D;
            double z = getZ() + (random.nextDouble() - 0.5D) * 0.45D;
            level().addParticle(ParticleTypes.SOUL, x, y, z,
                    (random.nextDouble() - 0.5D) * 0.008D,
                    0.006D + random.nextDouble() * 0.01D,
                    (random.nextDouble() - 0.5D) * 0.008D);
        }
    }

    private void tickDeathReplay() {
        if (replayFrames.size() < 2) {
            beginFlare();
            return;
        }

        ReplayFrame first = replayFrames.get(0);
        ReplayFrame last = replayFrames.get(replayFrames.size() - 1);
        double targetTime = first.time() + replayElapsedTime;
        replayElapsedTime += REPLAY_TIME_SCALE;
        if (targetTime >= last.time()) {
            applyReplayFrame(last);
            updateWalkBlend(0.0F);
            beginDeathCue();
            return;
        }

        int index = 0;
        while (index + 1 < replayFrames.size() - 1
                && replayFrames.get(index + 1).time() <= targetTime) {
            index++;
        }
        ReplayFrame from = replayFrames.get(index);
        ReplayFrame to = replayFrames.get(index + 1);
        float amount = (float) (targetTime - from.time()) / Math.max(1L, to.time() - from.time());
        double oldX = getX();
        double oldZ = getZ();
        double x = interpolatePosition(index, amount, 0);
        double y = interpolatePosition(index, amount, 1);
        double z = interpolatePosition(index, amount, 2);
        setPos(x, y, z);
        setYRot(Mth.rotLerp(amount, from.yaw(), to.yaw()));
        setXRot(Mth.lerp(amount, from.pitch(), to.pitch()));
        entityData.set(REPLAY_HEAD_YAW, Mth.rotLerp(amount, from.headYaw(), to.headYaw()));
        entityData.set(REPLAY_CROUCHING, amount < 0.5F ? from.crouching() : to.crouching());
        entityData.set(REPLAY_SPRINTING, amount < 0.5F ? from.sprinting() : to.sprinting());

        float horizontalMovement = (float) Math.hypot(x - oldX, z - oldZ);
        entityData.set(REPLAY_LIMB_PHASE,
                entityData.get(REPLAY_LIMB_PHASE) + horizontalMovement * 4.0F);
        updateWalkBlend(Mth.clamp(horizontalMovement / 0.18F, 0.0F, 1.0F));
    }

    private double interpolatePosition(int frameIndex, float amount, int axis) {
        ReplayFrame from = replayFrames.get(frameIndex);
        ReplayFrame to = replayFrames.get(frameIndex + 1);
        ReplayFrame previous = frameIndex > 0 ? replayFrames.get(frameIndex - 1) : from;
        ReplayFrame next = frameIndex + 2 < replayFrames.size()
                ? replayFrames.get(frameIndex + 2) : to;

        double duration = Math.max(1L, to.time() - from.time());
        double previousDuration = Math.max(1L, to.time() - previous.time());
        double nextDuration = Math.max(1L, next.time() - from.time());
        double start = replayCoordinate(from, axis);
        double end = replayCoordinate(to, axis);
        double startTangent = (end - replayCoordinate(previous, axis)) * duration / previousDuration;
        double endTangent = (replayCoordinate(next, axis) - start) * duration / nextDuration;

        double t2 = amount * amount;
        double t3 = t2 * amount;
        return (2.0D * t3 - 3.0D * t2 + 1.0D) * start
                + (t3 - 2.0D * t2 + amount) * startTangent
                + (-2.0D * t3 + 3.0D * t2) * end
                + (t3 - t2) * endTangent;
    }

    private static double replayCoordinate(ReplayFrame frame, int axis) {
        return switch (axis) {
            case 0 -> frame.x();
            case 1 -> frame.y();
            default -> frame.z();
        };
    }

    private void beginDeathCue() {
        String typeId = entityData.get(KILLER_TYPE);
        if (typeId.isEmpty()) { beginFlare(); return; }
        entityData.set(DEATH_CUE_TICK, tickCount);
        if (level() instanceof ServerLevel serverLevel && !killerMessageSent) {
            killerMessageSent = true;
            ResourceLocation id = ResourceLocation.tryParse(typeId);
            EntityType<?> killer = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
            Component name = killer == null ? Component.literal(typeId) : Component.translatable(killer.getDescriptionId());
            for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32.0D))) {
                player.displayClientMessage(Component.translatable(isCreeperDeath()
                        ? "message.traces.death_echo_creeper" : "message.traces.death_echo_killer", name)
                        .withStyle(ChatFormatting.RED), true);
            }
            if (isCreeperDeath()) {
                serverLevel.playSound(null, blockPosition(), SoundEvents.CREEPER_PRIMED,
                        SoundSource.AMBIENT, 0.7F, 0.8F);
            }
        }
    }

    private void applyReplayFrame(ReplayFrame frame) {
        setPos(frame.x(), frame.y(), frame.z());
        setYRot(frame.yaw());
        setXRot(frame.pitch());
        entityData.set(REPLAY_HEAD_YAW, frame.headYaw());
        entityData.set(REPLAY_CROUCHING, frame.crouching());
        entityData.set(REPLAY_SPRINTING, frame.sprinting());
    }

    private void tickServer() {
        int vanishTick = entityData.get(VANISH_TICK);
        if (isDeathReplay() && entityData.get(FLARE_TICK) < 0) {
            int cueTick = entityData.get(DEATH_CUE_TICK);
            if (cueTick >= 0) {
                if (tickCount - cueTick >= DEATH_CUE_TICKS) beginFlare();
                return;
            }
            tickDeathReplay();
            return;
        }

        if (vanishTick >= 0) {
            currentWalkSpeed = 0.0D;
            updateWalkBlend(0.0F);
            if (tickCount - vanishTick >= VANISH_DURATION_TICKS) {
                if (isDeathReplay() && !replayMessageSent && level() instanceof ServerLevel serverLevel) {
                    replayMessageSent = true;
                    for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class,
                            getBoundingBox().inflate(24.0D))) {
                        player.displayClientMessage(Component.translatable("message.traces.death_echo_ending")
                                .withStyle(ChatFormatting.RED), true);
                    }
                }
                discard();
            }
            return;
        }

        int flareTick = entityData.get(FLARE_TICK);
        if (flareTick >= 0) {
            currentWalkSpeed = 0.0D;
            updateWalkBlend(0.0F);
            if (tickCount - flareTick >= FLARE_DURATION_TICKS) beginDissolve();
            return;
        }

        if (tickCount >= MAX_AGE_TICKS) {
            beginFlare();
            return;
        }

        if (!entityData.get(HAS_DESTINATION) || tickCount < WALK_START_TICK) {
            updateWalkBlend(0.0F);
            return;
        }

        Vec3 destination = Vec3.atBottomCenterOf(entityData.get(DESTINATION));
        Vec3 offset = destination.subtract(position());
        double distance = offset.length();
        if (distance <= ARRIVAL_DISTANCE) {
            currentWalkSpeed = 0.0D;
            updateWalkBlend(0.0F);
            beginFlare();
            return;
        }

        double remainingDistance = Math.max(0.0D, distance - ARRIVAL_DISTANCE);
        double desiredSpeed = Math.min(MAX_WALK_SPEED,
                Math.sqrt(2.0D * WALK_DECELERATION * remainingDistance));
        double speedChange = desiredSpeed > currentWalkSpeed ? WALK_ACCELERATION : WALK_DECELERATION;
        currentWalkSpeed += Math.max(-speedChange, Math.min(speedChange, desiredSpeed - currentWalkSpeed));
        updateWalkBlend((float) (currentWalkSpeed / MAX_WALK_SPEED));

        float desiredYaw = (float) Math.toDegrees(Math.atan2(-offset.x, offset.z));
        setYRot(Mth.approachDegrees(getYRot(), desiredYaw, 3.0F));
        Vec3 step = offset.normalize().scale(Math.min(currentWalkSpeed, distance));
        setPos(position().add(step));
    }

    private void beginFlare() {
        if (entityData.get(FLARE_TICK) >= 0 || isDissolving()) return;
        entityData.set(FLARE_TICK, tickCount);
        currentWalkSpeed = 0.0D;
        updateWalkBlend(0.0F);
        if (level() instanceof ServerLevel serverLevel) {
            Vec3 at = position().add(0.0D, 0.9D, 0.0D);
            if (isDeathReplay()) {
                if (isCreeperDeath()) {
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    serverLevel.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 34, 0.7D, 0.8D, 0.7D, 0.08D);
                    serverLevel.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 24, 0.55D, 0.65D, 0.55D, 0.04D);
                    serverLevel.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE,
                            SoundSource.AMBIENT, 0.8F, 0.85F);
                }
                DustParticleOptions redDust = new DustParticleOptions(new Vector3f(0.95F, 0.06F, 0.035F), 1.35F);
                serverLevel.sendParticles(redDust, at.x, at.y, at.z, 28, 0.38D, 0.8D, 0.38D, 0.025D);
                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        at.x, at.y, at.z, 8, 0.28D, 0.45D, 0.28D, 0.08D);
            } else {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z,
                        12, 0.34D, 0.75D, 0.34D, 0.018D);
                serverLevel.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z,
                        5, 0.22D, 0.65D, 0.22D, 0.025D);
            }
        }
    }

    private void beginDissolve() {
        if (isDissolving()) return;
        entityData.set(VANISH_TICK, tickCount);
        entityData.set(WALKING, false);
        if (level() instanceof ServerLevel serverLevel) {
            Vec3 at = position().add(0.0D, 0.9D, 0.0D);
            if (isDeathReplay()) {
                DustParticleOptions redDust = new DustParticleOptions(new Vector3f(0.95F, 0.04F, 0.025F), 1.15F);
                serverLevel.sendParticles(redDust, at.x, at.y, at.z, 24, 0.34D, 0.72D, 0.34D, 0.018D);
                serverLevel.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z,
                        10, 0.25D, 0.55D, 0.25D, 0.015D);
            } else {
                serverLevel.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 14, 0.32D, 0.65D, 0.32D, 0.018D);
                serverLevel.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 12, 0.24D, 0.62D, 0.24D, 0.014D);
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z,
                        18, 0.35D, 0.8D, 0.35D, 0.022D);
                serverLevel.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z,
                        8, 0.24D, 0.7D, 0.24D, 0.035D);
            }
            serverLevel.playSound(null, blockPosition(), SoundEvents.GRASS_HIT, SoundSource.AMBIENT, 0.45F, 0.72F);
        }
    }

    public float getGlowIntensity(float partialTick) {
        int flareTick = entityData.get(FLARE_TICK);
        if (flareTick < 0) return 0.0F;
        float age = tickCount + partialTick;
        float glow = smoothStep((age - flareTick) / 4.0F);
        int vanishTick = entityData.get(VANISH_TICK);
        if (vanishTick >= 0) glow *= 1.0F - smoothStep((age - vanishTick) / VANISH_DURATION_TICKS);
        return glow;
    }

    public float getFadeAlpha(float partialTick) {
        float age = tickCount + partialTick;
        float fadeIn = smoothStep(age / APPEAR_TICKS);
        int vanishTick = entityData.get(VANISH_TICK);
        float fadeOut = vanishTick >= 0
                ? 1.0F - smoothStep((age - vanishTick) / VANISH_DURATION_TICKS)
                : (isDeathReplay() || entityData.get(HAS_DESTINATION))
                    ? 1.0F
                    : smoothStep((160.0F - age) / 60.0F);
        float glow = getGlowIntensity(partialTick);
        float opacity = isDeathReplay() ? (glow > 0.0F ? 0.48F + 0.42F * glow : 0.48F)
                : (glow > 0.0F ? 0.42F + 0.40F * glow : 0.42F);
        return Math.min(fadeIn, fadeOut) * opacity;
    }

    private static float smoothStep(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.getBoolean("traces_death_replay")) {
            ListTag frames = tag.getList("traces_replay_frames", Tag.TAG_COMPOUND);
            if (frames.size() >= 2) {
                setReplayFrames(frames);
                replayElapsedTime = tag.getDouble("traces_replay_elapsed");
                entityData.set(DEATH_REPLAY, true);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (!isDeathReplay() || replayFrames.isEmpty()) return;
        tag.putBoolean("traces_death_replay", true);
        tag.putDouble("traces_replay_elapsed", replayElapsedTime);
        ListTag frames = new ListTag();
        for (ReplayFrame frame : replayFrames) {
            CompoundTag saved = new CompoundTag();
            saved.putLong("time", frame.time());
            saved.putDouble("x", frame.x());
            saved.putDouble("y", frame.y());
            saved.putDouble("z", frame.z());
            saved.putFloat("yaw", frame.yaw());
            saved.putFloat("pitch", frame.pitch());
            saved.putFloat("head_yaw", frame.headYaw());
            saved.putBoolean("crouching", frame.crouching());
            saved.putBoolean("sprinting", frame.sprinting());
            frames.add(saved);
        }
        tag.put("traces_replay_frames", frames);
    }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private record ReplayFrame(long time, double x, double y, double z, float yaw, float pitch,
                               float headYaw, boolean crouching, boolean sprinting) { }
}
