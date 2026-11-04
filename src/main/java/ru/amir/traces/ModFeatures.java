package ru.amir.traces;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, TracesOfPresence.MOD_ID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> LIFE_ECHO_SITE =
            FEATURES.register("life_echo_site",
                    () -> new LifeEchoSiteFeature(NoneFeatureConfiguration.CODEC));

    private ModFeatures() {
    }
}
