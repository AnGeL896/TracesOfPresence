package ru.amir.traces;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TracesOfPresence.MOD_ID)
public class TracesOfPresence {
    public static final String MOD_ID = "traces";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TracesOfPresence() {
        LOGGER.info("Traces of Presence has awakened.");
    }
}
