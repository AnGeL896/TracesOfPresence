package ru.amir.traces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
    private static final double WALK_SPEED = 0.075D;

    private static final EntityDataAccessor<BlockPos> DESTINATION =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Boolean> HAS_DESTINATION =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WALKING =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FLARE_TICK =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VANISH_TICK =
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.INT);

    public ApparitionEntity(EntityType<? extends ApparitionEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DESTINATION, BlockPos.ZERO);
        entityData.define(HAS_DESTINATION, false);
        entityData.define(WALKING, false);
        entityData.define(FLARE_TICK, -1);
        entityData.define(VANISH_TICK, -1);
    }

    public void setDestination(BlockPos destination) {
        entityData.set(DESTINATION, destination.immutable());
        entityData.set(HAS_DESTINATION, true);
    }

    public boolean isWalking() {
        return entityData.get(WALKING);
    }

    private boolean isDissolving() {
        return entityData.get(VANISH_TICK) >= 0;
    }

    @Override
    public void tick() {
        super.tick();

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

    private void tickServer() {
        int vanishTick = entityData.get(VANISH_TICK);
        if (vanishTick >= 0) {
            entityData.set(WALKING, false);
            if (tickCount - vanishTick >= VANISH_DURATION_TICKS) discard();
            return;
        }

        int flareTick = entityData.get(FLARE_TICK);
        if (flareTick >= 0) {
            entityData.set(WALKING, false);
            if (tickCount - flareTick >= FLARE_DURATION_TICKS) beginDissolve();
            return;
        }

        if (tickCount >= MAX_AGE_TICKS) {
            beginFlare();
            return;
        }

        if (!entityData.get(HAS_DESTINATION) || tickCount < WALK_START_TICK) return;

        Vec3 destination = Vec3.atBottomCenterOf(entityData.get(DESTINATION));
        Vec3 offset = destination.subtract(position());
        double distance = offset.length();
        if (distance <= WALK_SPEED + 0.45D) {
            beginFlare();
            return;
        }

        entityData.set(WALKING, true);
        float yaw = (float) (Math.toDegrees(Math.atan2(-offset.x, offset.z)));
        setYRot(yaw);
        yRotO = yaw;
        Vec3 step = offset.normalize().scale(WALK_SPEED);
        setPos(position().add(step));
    }

    private void beginFlare() {
        if (entityData.get(FLARE_TICK) >= 0 || isDissolving()) return;
        entityData.set(FLARE_TICK, tickCount);
        entityData.set(WALKING, false);
        if (level() instanceof ServerLevel serverLevel) {
            Vec3 at = position().add(0.0D, 0.9D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z,
                    12, 0.34D, 0.75D, 0.34D, 0.018D);
            serverLevel.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z,
                    5, 0.22D, 0.65D, 0.22D, 0.025D);
        }
    }

    private void beginDissolve() {
        if (isDissolving()) return;
        entityData.set(VANISH_TICK, tickCount);
        entityData.set(WALKING, false);
        if (level() instanceof ServerLevel serverLevel) {
            Vec3 at = position().add(0.0D, 0.9D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 14, 0.32D, 0.65D, 0.32D, 0.018D);
            serverLevel.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 12, 0.24D, 0.62D, 0.24D, 0.014D);
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z,
                    18, 0.35D, 0.8D, 0.35D, 0.022D);
            serverLevel.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z,
                    8, 0.24D, 0.7D, 0.24D, 0.035D);
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
                : entityData.get(HAS_DESTINATION)
                    ? 1.0F
                    : smoothStep((160.0F - age) / 60.0F);
        float glow = getGlowIntensity(partialTick);
        float opacity = glow > 0.0F ? 0.42F + 0.40F * glow : 0.42F;
        return Math.min(fadeIn, fadeOut) * opacity;
    }

    private static float smoothStep(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
