package ru.amir.traces;

import net.minecraftforge.common.ForgeConfigSpec;

/** Tunable encounter rates for blue and green memory echoes. */
public final class TracesConfig {
    public static final ForgeConfigSpec COMMON_SPEC;

    public static final ForgeConfigSpec.BooleanValue BLUE_ECHO_ENABLED;
    public static final ForgeConfigSpec.DoubleValue BLUE_CHANCE_PER_CHECK;
    public static final ForgeConfigSpec.IntValue BLUE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue GREEN_ECHO_ENABLED;
    public static final ForgeConfigSpec.DoubleValue GREEN_CHANCE_PER_CHECK;
    public static final ForgeConfigSpec.IntValue GREEN_COOLDOWN_TICKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("blue_echo");
        BLUE_ECHO_ENABLED = builder.comment("Enable the blue forest apparition.")
                .define("enabled", true);
        BLUE_CHANCE_PER_CHECK = builder.comment("Chance per five-second eligible check; raise this for testing.")
                .defineInRange("chance_per_check", 0.05D, 0.0D, 1.0D);
        BLUE_COOLDOWN_TICKS = builder.comment("Per-player cooldown in ticks (20 ticks = 1 second).")
                .defineInRange("cooldown_ticks", 1200, 0, 240000);
        builder.pop();

        builder.push("green_echo");
        GREEN_ECHO_ENABLED = builder.comment("Enable the green work-memory apparition.")
                .define("enabled", true);
        GREEN_CHANCE_PER_CHECK = builder.comment("Chance per two-second eligible check.")
                .defineInRange("chance_per_check", 0.08D, 0.0D, 1.0D);
        GREEN_COOLDOWN_TICKS = builder.comment("Per-player cooldown in ticks (20 ticks = 1 second).")
                .defineInRange("cooldown_ticks", 6000, 0, 240000);
        builder.pop();

        COMMON_SPEC = builder.build();
    }

    private TracesConfig() { }
}
