package ru.amir.traces;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(TracesOfPresence.MOD_ID)
public class TracesOfPresence {
    public static final String MOD_ID = "traces";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TracesOfPresence() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);

        LOGGER.info("Traces of Presence has awakened.");
    }
}
