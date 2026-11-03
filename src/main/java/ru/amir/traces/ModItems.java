package ru.amir.traces;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TracesOfPresence.MOD_ID);

    public static final RegistryObject<Item> LIFE_ECHO =
            ITEMS.register("life_echo",
                    () -> new BlockItem(ModBlocks.LIFE_ECHO.get(), new Item.Properties()));
}
