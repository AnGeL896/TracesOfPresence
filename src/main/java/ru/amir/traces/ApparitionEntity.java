package ru.amir.traces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.BlockParticleOption;
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
    private static final double REPLAY_TIME_SCALE = 0.68D;
    private static final int DEATH_CUE_TICKS = 40;
    private static final DustParticleOptions GREEN_WORK_DUST =
            new DustParticleOptions(new Vector3f(0.10F, 0.95F, 0.35F), 0.8F);

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
    private static final EntityDataAccessor<Boolean> KILLER_VISIBLE =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> REPLAY_FALLING =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DEATH_CAUSE =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> KILLER_SWINGING =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> KILLER_SWING_TIME =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> KILLER_ATTACK_ANIM =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> REPLAY_MAIN_HAND =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> REPLAY_OFF_HAND =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> KILLER_MAIN_HAND =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> KILLER_OFF_HAND =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> KILLER_HEAD =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> KILLER_CHEST =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> KILLER_LEGS =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> KILLER_FEET =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> KILLER_BABY =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WORK_REPLAY =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> GREEN_ACTION =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockPos> GREEN_TARGET =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.BLOCK_POS);

    private double currentWalkSpeed;
    private float previousWalkBlend;
    private float previousReplayLimbPhase;
    private float replayCrouchBlend;
    private float previousReplayCrouchBlend;
    private float replaySprintBlend;
    private float previousReplaySprintBlend;
    private float replayFallBlend;
    private float previousReplayFallBlend;
    private List<KillerFrame> killerFrames = List.of();
    private List<ReplayFrame> replayFrames = List.of();
    private List<WorkFrame> workFrames = List.of();
    private List<BlockParticleOption> workBlockParticles = List.of();
    private int[] workFrameDurations = new int[0];
    private int replayFrameCursor;
    private int killerFrameCursor;
    private int workStartTick;
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
        entityData.define(KILLER_VISIBLE, false);
        entityData.define(REPLAY_FALLING, false);
        entityData.define(DEATH_CAUSE, "");
        entityData.define(KILLER_SWINGING, false);
        entityData.define(KILLER_SWING_TIME, 0);
        entityData.define(KILLER_ATTACK_ANIM, 0.0F);
        entityData.define(REPLAY_MAIN_HAND, "");
        entityData.define(REPLAY_OFF_HAND, "");
        entityData.define(KILLER_MAIN_HAND, "");
        entityData.define(KILLER_OFF_HAND, "");
        entityData.define(KILLER_HEAD, "");
        entityData.define(KILLER_CHEST, "");
        entityData.define(KILLER_LEGS, "");
        entityData.define(KILLER_FEET, "");
        entityData.define(KILLER_BABY, false);
        entityData.define(WORK_REPLAY, false);
        entityData.define(GREEN_ACTION, 0);
        entityData.define(GREEN_TARGET, BlockPos.ZERO);
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
    public boolean isKillerVisible() { return entityData.get(KILLER_VISIBLE); }
    public String getDeathCause() { return entityData.get(DEATH_CAUSE); }
    public boolean isKillerSwinging() { return entityData.get(KILLER_SWINGING); }
    public int getKillerSwingTime() { return entityData.get(KILLER_SWING_TIME); }
    public float getKillerAttackAnim() { return entityData.get(KILLER_ATTACK_ANIM); }
    public String getReplayMainHand() { return entityData.get(REPLAY_MAIN_HAND); }
    public String getReplayOffHand() { return entityData.get(REPLAY_OFF_HAND); }
    public String getKillerMainHand() { return entityData.get(KILLER_MAIN_HAND); }
    public String getKillerOffHand() { return entityData.get(KILLER_OFF_HAND); }
    public String getKillerHead() { return entityData.get(KILLER_HEAD); }
    public String getKillerChest() { return entityData.get(KILLER_CHEST); }
    public String getKillerLegs() { return entityData.get(KILLER_LEGS); }
    public String getKillerFeet() { return entityData.get(KILLER_FEET); }
    public boolean isKillerBaby() { return entityData.get(KILLER_BABY); }
    public float getReplayFallBlend(float partialTick) {
        return Mth.lerp(partialTick, previousReplayFallBlend, replayFallBlend);
    }

    public boolean isGreenEcho() {
        return entityData.get(GREEN_ECHO);
    }

    public void setGreenEcho() {
        entityData.set(GREEN_ECHO, true);
    }

    public boolean isWorkReplay() { return entityData.get(WORK_REPLAY); }
    public int getGreenAction() { return entityData.get(GREEN_ACTION); }
    public BlockPos getGreenTarget() { return entityData.get(GREEN_TARGET); }

    public boolean setWorkReplay(ListTag actions) {
        if (actions.size() < 3) return false;
        List<WorkFrame> loaded = new ArrayList<>(actions.size());
        List<BlockParticleOption> blockParticles = new ArrayList<>(actions.size());
        for (int i = 0; i < actions.size(); i++) {
            CompoundTag action = actions.getCompound(i);
            double actorX = action.getDouble("actor_x");
            double actorY = action.getDouble("actor_y");
            double actorZ = action.getDouble("actor_z");
            BlockPos target = action.contains("x")
                    ? new BlockPos(action.getInt("x"), action.getInt("y"), action.getInt("z"))
                    : BlockPos.containing(actorX, actorY, actorZ);
            loaded.add(new WorkFrame(actorX, actorY, actorZ, action.getFloat("yaw"),
                    action.getString("type"), target, action.getString("id"),
                    action.getString("main_hand"), action.getString("off_hand")));
            BlockParticleOption blockParticle = null;
            String type = action.getString("type");
            if ("break".equals(type) || "place".equals(type)) {
                ResourceLocation blockId = ResourceLocation.tryParse(action.getString("id"));
                var block = blockId == null ? null : BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
                if (block != null && !block.defaultBlockState().isAir()) {
                    blockParticle = new BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState());
                }
            }
            blockParticles.add(blockParticle);
        }
        workFrames = loaded;
        workBlockParticles = blockParticles;
        workFrameDurations = new int[workFrames.size()];
        for (int i = 0; i < workFrames.size(); i++) {
            int duration = 30;
            if (i + 1 < workFrames.size()) {
                WorkFrame from = workFrames.get(i);
                WorkFrame to = workFrames.get(i + 1);
                double distance = Math.sqrt(square(to.actorX() - from.actorX())
                        + square(to.actorY() - from.actorY())
                        + square(to.actorZ() - from.actorZ()));
                duration = Mth.clamp(24 + (int) Math.ceil(distance * 7.5D), 30, 100);
            }
            workFrameDurations[i] = duration;
        }
        WorkFrame first = workFrames.get(0);
        setPos(first.actorX(), first.actorY(), first.actorZ());
        setYRot(first.yaw());
        entityData.set(GREEN_ECHO, true);
        entityData.set(DEATH_REPLAY, false);
        entityData.set(WORK_REPLAY, true);
        entityData.set(GREEN_ACTION, 0);
        entityData.set(GREEN_TARGET, first.target());
        entityData.set(REPLAY_MAIN_HAND, first.mainHand());
        entityData.set(REPLAY_OFF_HAND, first.offHand());
        workStartTick = tickCount;
        return true;
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
        entityData.set(REPLAY_MAIN_HAND, first.mainHand());
        entityData.set(REPLAY_OFF_HAND, first.offHand());
        entityData.set(GREEN_ECHO, false);
        entityData.set(KILLER_TYPE, memory.getString("attacker_type"));
        entityData.set(KILLER_X, (float) memory.getDouble("attacker_x"));
        entityData.set(KILLER_Y, (float) memory.getDouble("attacker_y"));
        entityData.set(KILLER_Z, (float) memory.getDouble("attacker_z"));
        entityData.set(KILLER_YAW, memory.getFloat("attacker_yaw"));
        entityData.set(DEATH_CAUSE, memory.getString("cause"));
        setKillerFrames(memory.getList("attacker_frames", Tag.TAG_COMPOUND));
        entityData.set(KILLER_VISIBLE, false);
        entityData.set(REPLAY_FALLING, false);
        entityData.set(DEATH_CUE_TICK, -1);
        entityData.set(DEATH_REPLAY, true);
        replayElapsedTime = 0.0D;
        return true;
    }

    private void setKillerFrames(ListTag frames) {
        List<KillerFrame> loaded = new ArrayList<>(frames.size());
        for (int i = 0; i < frames.size(); i++) {
            CompoundTag frame = frames.getCompound(i);
            loaded.add(new KillerFrame(frame.getLong("time"), frame.getDouble("x"),
                    frame.getDouble("y"), frame.getDouble("z"), frame.getFloat("yaw"),
                    frame.getFloat("pitch"), frame.getBoolean("aggressive"),
                    frame.getBoolean("swinging"), frame.getInt("swing_time"),
                    frame.getFloat("attack_anim"), frame.getString("main_hand"),
                    frame.getString("off_hand"), frame.getString("armor_head"),
                    frame.getString("armor_chest"), frame.getString("armor_legs"),
                    frame.getString("armor_feet"), frame.getBoolean("baby")));
        }
        killerFrames = loaded;
        killerFrameCursor = 0;
    }

    private void setReplayFrames(ListTag frames) {
        List<ReplayFrame> loaded = new ArrayList<>(frames.size());
        for (int i = 0; i < frames.size(); i++) {
            CompoundTag frame = frames.getCompound(i);
            loaded.add(new ReplayFrame(frame.getLong("time"), frame.getDouble("x"),
                    frame.getDouble("y"), frame.getDouble("z"), frame.getFloat("yaw"),
                    frame.getFloat("pitch"), frame.getFloat("head_yaw"), frame.getBoolean("crouching"),
                    frame.getBoolean("sprinting"), frame.getBoolean("on_ground"),
                    frame.getString("main_hand"), frame.getString("off_hand")));
        }
        replayFrames = loaded;
        replayFrameCursor = 0;
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
        previousReplayFallBlend = replayFallBlend;
        super.tick();
        float targetFall = entityData.get(REPLAY_FALLING) ? 1.0F : 0.0F;
        replayFallBlend = Mth.approach(replayFallBlend, targetFall, 0.16F);
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
                if (isGreenEcho()) {
                    level().addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z,
                            (random.nextDouble() - 0.5D) * 0.015D, 0.018D,
                            (random.nextDouble() - 0.5D) * 0.015D);
                } else {
                    level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z,
                            (random.nextDouble() - 0.5D) * 0.018D,
                            0.015D + random.nextDouble() * 0.025D,
                            (random.nextDouble() - 0.5D) * 0.018D);
                }
            }
            if (tickCount % 2 == 0) {
                level().addParticle(isGreenEcho() ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.END_ROD,
                        getX(), getY() + 0.9D, getZ(), (random.nextDouble() - 0.5D) * 0.025D, 0.025D,
                        (random.nextDouble() - 0.5D) * 0.025D);
            }
        } else if (tickCount % 5 == 0 && random.nextFloat() < 0.55F) {
            double x = getX() + (random.nextDouble() - 0.5D) * 0.45D;
            double y = getY() + 0.2D + random.nextDouble() * 1.35D;
            double z = getZ() + (random.nextDouble() - 0.5D) * 0.45D;
            if (isGreenEcho()) {
                level().addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0D, 0.008D, 0.0D);
            } else {
                level().addParticle(ParticleTypes.SOUL, x, y, z,
                        (random.nextDouble() - 0.5D) * 0.008D,
                        0.006D + random.nextDouble() * 0.01D,
                        (random.nextDouble() - 0.5D) * 0.008D);
            }
        }
    }

    private void tickWorkReplay() {
        if (workFrames.size() < 3 || workFrameDurations.length != workFrames.size()) {
            beginFlare();
            return;
        }

        int elapsed = tickCount - workStartTick;
        int index = 0;
        while (index < workFrameDurations.length && elapsed >= workFrameDurations[index]) {
            elapsed -= workFrameDurations[index];
            index++;
        }
        if (index >= workFrames.size()) {
            entityData.set(GREEN_ACTION, 0);
            beginFlare();
            return;
        }

        int localTick = elapsed;
        int interval = workFrameDurations[index];
        WorkFrame from = workFrames.get(index);
        WorkFrame to = index + 1 < workFrames.size() ? workFrames.get(index + 1) : from;
        float amount = smoothStep(localTick / (float) interval);
        double oldX = getX();
        double oldZ = getZ();
        double x = Mth.lerp(amount, from.actorX(), to.actorX());
        double y = Mth.lerp(amount, from.actorY(), to.actorY());
        double z = Mth.lerp(amount, from.actorZ(), to.actorZ());
        setPos(x, y, z);
        setYRot(Mth.rotLerp(amount, from.yaw(), to.yaw()));
        entityData.set(REPLAY_HEAD_YAW, getYRot());
        entityData.set(GREEN_TARGET, from.target());
        entityData.set(REPLAY_MAIN_HAND, from.mainHand());
        entityData.set(REPLAY_OFF_HAND, from.offHand());

        int action = localTick < Math.min(17, interval * 3 / 5) ? switch (from.type()) {
            case "break" -> 1;
            case "place" -> 2;
            case "craft" -> 3;
            default -> 0;
        } : 0;
        entityData.set(GREEN_ACTION, action);
        float movement = (float) Math.hypot(x - oldX, z - oldZ);
        entityData.set(REPLAY_LIMB_PHASE, entityData.get(REPLAY_LIMB_PHASE) + movement * 2.2F);
        updateWalkBlend(Mth.clamp(movement / 0.10F, 0.0F, 1.0F));

        if (action != 0 && level() instanceof ServerLevel serverLevel && tickCount % 4 == 0) {
            double px = from.target().getX() + 0.5D;
            double py = from.target().getY() + 0.65D;
            double pz = from.target().getZ() + 0.5D;
            serverLevel.sendParticles(GREEN_WORK_DUST, px, py, pz, 2, 0.18D, 0.18D, 0.18D, 0.008D);
            if ((action == 1 || action == 2) && index < workBlockParticles.size()) {
                BlockParticleOption fragments = workBlockParticles.get(index);
                if (fragments != null) {
                    serverLevel.sendParticles(fragments, px, py, pz, action == 1 ? 5 : 3,
                            0.18D, 0.2D, 0.18D, 0.06D);
                }
            }
            if (action == 1) serverLevel.sendParticles(ParticleTypes.CRIT, px, py, pz, 1, 0.12D, 0.16D, 0.12D, 0.02D);
            if (action == 2) serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, py, pz, 1, 0.16D, 0.2D, 0.16D, 0.005D);
            if (action == 3) serverLevel.sendParticles(ParticleTypes.ENCHANT, px, py, pz, 2, 0.16D, 0.18D, 0.16D, 0.2D);
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
        updateKillerPlayback(targetTime);
        if (targetTime >= last.time()) {
            applyReplayFrame(last);
            updateWalkBlend(0.0F);
            beginDeathCue();
            return;
        }

        int index = Math.min(replayFrameCursor, replayFrames.size() - 2);
        while (index + 1 < replayFrames.size() - 1
                && replayFrames.get(index + 1).time() <= targetTime) {
            index++;
        }
        replayFrameCursor = index;
        ReplayFrame from = replayFrames.get(index);
        ReplayFrame to = replayFrames.get(index + 1);
        float amount = (float) (targetTime - from.time()) / Math.max(1L, to.time() - from.time());
        double oldX = getX();
        double oldZ = getZ();
        double x = Mth.lerp(amount, from.x(), to.x());
        double y = Mth.lerp(amount, from.y(), to.y());
        double z = Mth.lerp(amount, from.z(), to.z());
        setPos(x, y, z);
        setYRot(Mth.rotLerp(amount, from.yaw(), to.yaw()));
        setXRot(Mth.lerp(amount, from.pitch(), to.pitch()));
        entityData.set(REPLAY_HEAD_YAW, Mth.rotLerp(amount, from.headYaw(), to.headYaw()));
        entityData.set(REPLAY_CROUCHING, amount < 0.5F ? from.crouching() : to.crouching());
        entityData.set(REPLAY_SPRINTING, amount < 0.5F ? from.sprinting() : to.sprinting());
        entityData.set(REPLAY_FALLING, !to.onGround() && y < from.y());
        entityData.set(REPLAY_MAIN_HAND, amount < 0.5F ? from.mainHand() : to.mainHand());
        entityData.set(REPLAY_OFF_HAND, amount < 0.5F ? from.offHand() : to.offHand());

        float horizontalMovement = (float) Math.hypot(x - oldX, z - oldZ);
        entityData.set(REPLAY_LIMB_PHASE,
                entityData.get(REPLAY_LIMB_PHASE) + horizontalMovement * 2.4F);
        updateWalkBlend(Mth.clamp(horizontalMovement / 0.18F, 0.0F, 1.0F));
    }

    private void updateKillerPlayback(double targetTime) {
        if (killerFrames.isEmpty()) { entityData.set(KILLER_VISIBLE, false); return; }
        int index = Math.min(killerFrameCursor, killerFrames.size() - 1);
        while (index + 1 < killerFrames.size() && killerFrames.get(index + 1).time() <= targetTime) index++;
        killerFrameCursor = index;
        KillerFrame from = killerFrames.get(index);
        if (targetTime < from.time() || targetTime > from.time() + 2.0D) {
            entityData.set(KILLER_VISIBLE, false);
            return;
        }
        KillerFrame to = index + 1 < killerFrames.size() ? killerFrames.get(index + 1) : from;
        float amount = to.time() == from.time() ? 0.0F
                : Mth.clamp((float) ((targetTime - from.time()) / (to.time() - from.time())), 0.0F, 1.0F);
        KillerFrame attack = amount < 0.5F ? from : to;
        entityData.set(KILLER_X, (float) Mth.lerp(amount, from.x(), to.x()));
        entityData.set(KILLER_Y, (float) Mth.lerp(amount, from.y(), to.y()));
        entityData.set(KILLER_Z, (float) Mth.lerp(amount, from.z(), to.z()));
        entityData.set(KILLER_YAW, Mth.rotLerp(amount, from.yaw(), to.yaw()));
        entityData.set(KILLER_SWINGING, attack.swinging());
        entityData.set(KILLER_SWING_TIME, attack.swingTime());
        entityData.set(KILLER_MAIN_HAND, attack.mainHand());
        entityData.set(KILLER_OFF_HAND, attack.offHand());
        entityData.set(KILLER_HEAD, attack.head());
        entityData.set(KILLER_CHEST, attack.chest());
        entityData.set(KILLER_LEGS, attack.legs());
        entityData.set(KILLER_FEET, attack.feet());
        entityData.set(KILLER_BABY, attack.baby());
        entityData.set(KILLER_ATTACK_ANIM, attack.attackAnim());
        entityData.set(KILLER_VISIBLE, true);
    }

    private void beginDeathCue() {
        String typeId = entityData.get(KILLER_TYPE);
        if (typeId.isEmpty()) {
            showEnvironmentalCause();
            beginFlare();
            return;
        }
        entityData.set(KILLER_VISIBLE, true);
        entityData.set(DEATH_CUE_TICK, tickCount);
        if (level() instanceof ServerLevel serverLevel && !killerMessageSent) {
            killerMessageSent = true;
            ResourceLocation id = ResourceLocation.tryParse(typeId);
            EntityType<?> killer = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
            Component name = killer == null ? Component.literal(typeId) : Component.translatable(killer.getDescriptionId());
            for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32.0D))) {
                player.displayClientMessage((isCreeperDeath()
                        ? Component.translatable("message.traces.death_echo_creeper", name)
                        : Component.translatable("message.traces.death_echo_killer_cause", name, getDeathCauseText()))
                        .withStyle(ChatFormatting.RED), true);
            }
            if (isCreeperDeath()) {
                serverLevel.playSound(null, blockPosition(), SoundEvents.CREEPER_PRIMED,
                        SoundSource.AMBIENT, 0.7F, 0.8F);
            }
        }
    }

    private Component getDeathCauseText() {
        return switch (getDeathCause()) {
            case "arrow" -> Component.translatable("message.traces.death_cause_arrow");
            case "mob" -> Component.translatable("message.traces.death_cause_melee");
            case "player" -> Component.translatable("message.traces.death_cause_player");
            case "explosion" -> Component.translatable("message.traces.death_cause_explosion");
            case "fall" -> Component.translatable("message.traces.death_cause_fall");
            case "drown" -> Component.translatable("message.traces.death_cause_drown");
            case "lava", "hotFloor" -> Component.translatable("message.traces.death_cause_lava");
            case "inFire", "onFire" -> Component.translatable("message.traces.death_cause_fire");
            case "magic", "wither" -> Component.translatable("message.traces.death_cause_magic");
            default -> Component.literal(getDeathCause().isEmpty() ? "unknown" : getDeathCause());
        };
    }

    private void showEnvironmentalCause() {
        String cause = getDeathCause();
        String key = switch (cause) {
            case "fall" -> "message.traces.death_echo_fall";
            case "drown" -> "message.traces.death_echo_drown";
            case "lava", "hotFloor" -> "message.traces.death_echo_lava";
            case "inFire", "onFire" -> "message.traces.death_echo_fire";
            default -> "message.traces.death_echo_cause";
        };
        if (!(level() instanceof ServerLevel serverLevel)) return;
        var message = key.endsWith("_cause")
                ? Component.translatable(key, Component.literal(cause)) : Component.translatable(key);
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32.0D))) {
            player.displayClientMessage(message.withStyle(ChatFormatting.RED), true);
        }
    }

    private void applyReplayFrame(ReplayFrame frame) {
        setPos(frame.x(), frame.y(), frame.z());
        setYRot(frame.yaw());
        setXRot(frame.pitch());
        entityData.set(REPLAY_HEAD_YAW, frame.headYaw());
        entityData.set(REPLAY_CROUCHING, frame.crouching());
        entityData.set(REPLAY_SPRINTING, frame.sprinting());
        entityData.set(REPLAY_FALLING, !frame.onGround());
    }

    private void tickServer() {
        int vanishTick = entityData.get(VANISH_TICK);
        if (isDeathReplay() && entityData.get(FLARE_TICK) < 0) {
            int cueTick = entityData.get(DEATH_CUE_TICK);
            if (cueTick >= 0) {
                int elapsed = tickCount - cueTick;
                if (elapsed >= DEATH_CUE_TICKS) {
                    beginFlare();
                    if (level() instanceof ServerLevel serverLevel && tickCount % 2 == 0) {
                        double x = entityData.get(KILLER_X);
                        double y = entityData.get(KILLER_Y) + 0.8D;
                        double z = entityData.get(KILLER_Z);
                        DustParticleOptions spectralRed =
                                new DustParticleOptions(new Vector3f(0.95F, 0.07F, 0.045F), 0.9F);
                        serverLevel.sendParticles(spectralRed, x, y, z, 3, 0.32D, 0.48D, 0.32D, 0.012D);
                        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                x, y, z, 1, 0.22D, 0.35D, 0.22D, 0.008D);
                    }
                }
                return;
            }
            tickDeathReplay();
            return;
        }

        if (isWorkReplay() && vanishTick < 0 && entityData.get(FLARE_TICK) < 0) {
            tickWorkReplay();
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
                switch (getDeathCause()) {
                    case "fall" -> {
                        serverLevel.sendParticles(ParticleTypes.CLOUD, at.x, at.y - 0.5D, at.z, 20, 0.55D, 0.12D, 0.55D, 0.035D);
                        serverLevel.sendParticles(ParticleTypes.CRIT, at.x, at.y - 0.4D, at.z, 10, 0.3D, 0.15D, 0.3D, 0.08D);
                        serverLevel.playSound(null, blockPosition(), SoundEvents.GENERIC_SMALL_FALL, SoundSource.AMBIENT, 0.7F, 0.8F);
                    }
                    case "drown" -> {
                        serverLevel.sendParticles(ParticleTypes.BUBBLE, at.x, at.y, at.z, 28, 0.4D, 0.8D, 0.4D, 0.04D);
                        serverLevel.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 16, 0.35D, 0.5D, 0.35D, 0.06D);
                    }
                    case "lava", "hotFloor", "inFire", "onFire" -> {
                        serverLevel.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 24, 0.4D, 0.8D, 0.4D, 0.05D);
                        serverLevel.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 8, 0.3D, 0.4D, 0.3D, 0.02D);
                        serverLevel.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 16, 0.35D, 0.7D, 0.35D, 0.03D);
                    }
                    default -> { }
                }
            } else if (isGreenEcho()) {
                DustParticleOptions greenDust = new DustParticleOptions(new Vector3f(0.08F, 0.92F, 0.31F), 1.2F);
                serverLevel.sendParticles(greenDust, at.x, at.y, at.z, 24, 0.4D, 0.75D, 0.4D, 0.02D);
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y, at.z,
                        16, 0.35D, 0.75D, 0.35D, 0.015D);
                serverLevel.sendParticles(ParticleTypes.ENCHANT, at.x, at.y, at.z,
                        20, 0.35D, 0.8D, 0.35D, 0.08D);
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
            } else if (isGreenEcho()) {
                DustParticleOptions greenDust = new DustParticleOptions(new Vector3f(0.1F, 0.95F, 0.35F), 1.0F);
                serverLevel.sendParticles(greenDust, at.x, at.y, at.z, 22, 0.35D, 0.7D, 0.35D, 0.018D);
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y, at.z,
                        18, 0.35D, 0.7D, 0.35D, 0.01D);
                serverLevel.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z,
                        7, 0.2D, 0.55D, 0.2D, 0.02D);
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
                : (isDeathReplay() || isWorkReplay() || entityData.get(HAS_DESTINATION))
                    ? 1.0F
                    : smoothStep((160.0F - age) / 60.0F);
        float glow = getGlowIntensity(partialTick);
        float opacity = isDeathReplay() ? (glow > 0.0F ? 0.48F + 0.42F * glow : 0.48F)
                : isGreenEcho() ? (glow > 0.0F ? 0.55F + 0.35F * glow : 0.55F)
                : (glow > 0.0F ? 0.42F + 0.40F * glow : 0.42F);
        return Math.min(fadeIn, fadeOut) * opacity;
    }

    private static double square(double value) { return value * value; }

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
            saved.putBoolean("on_ground", frame.onGround());
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
                               float headYaw, boolean crouching, boolean sprinting, boolean onGround,
                               String mainHand, String offHand) { }
    private record WorkFrame(double actorX, double actorY, double actorZ, float yaw, String type,
                             BlockPos target, String itemId, String mainHand, String offHand) { }
    private record KillerFrame(long time, double x, double y, double z, float yaw, float pitch,
                               boolean aggressive, boolean swinging, int swingTime, float attackAnim,
                               String mainHand, String offHand, String head, String chest, String legs, String feet,
                               boolean baby) { }
}
