package ru.amir.traces;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TracesOfPresence.MOD_ID);

    public static final RegistryObject<EntityType<ApparitionEntity>> APPARITION = ENTITY_TYPES.register("apparition",
            () -> EntityType.Builder.<ApparitionEntity>of(ApparitionEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(32)
                    .updateInterval(2)
                    .build("apparition"));

    private ModEntities() { }
}
