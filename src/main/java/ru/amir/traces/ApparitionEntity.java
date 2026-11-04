package ru.amir.traces;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/** Harmless short-lived visual apparition. */
public final class ApparitionEntity extends Entity {
    public static final int LIFETIME_TICKS = 160;

    public ApparitionEntity(EntityType<? extends ApparitionEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount >= LIFETIME_TICKS) discard();
    }

    public float getFadeAlpha(float partialTick) {
        float age = tickCount + partialTick;
        float fadeIn = smoothStep(age / 20.0F);
        float fadeOut = smoothStep((LIFETIME_TICKS - age) / 60.0F);
        return Math.min(fadeIn, fadeOut) * 0.42F;
    }

    /** Glides sideways during seconds 3–5 of its lifespan. */
    public float getDriftOffset(float partialTick) {
        float age = tickCount + partialTick;
        float direction = (getId() & 1) == 0 ? 1.0F : -1.0F;
        return direction * 0.42F * smoothStep((age - 60.0F) / 40.0F);
    }

    private static float smoothStep(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    @Override protected void defineSynchedData() { }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
