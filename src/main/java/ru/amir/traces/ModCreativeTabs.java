package ru.amir.traces;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TracesOfPresence.MOD_ID);

    public static final RegistryObject<CreativeModeTab> TRACES_TAB =
            CREATIVE_MODE_TABS.register("traces_tab",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.traces"))
                            .icon(() -> new ItemStack(ModItems.LIFE_ECHO.get()))
                            .displayItems((parameters, output) -> {
                                output.accept(ModItems.LIFE_ECHO.get());
                            })
                            .build());
}
