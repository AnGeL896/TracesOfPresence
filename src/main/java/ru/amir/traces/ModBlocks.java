
package ru.amir.traces;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, TracesOfPresence.MOD_ID);

    public static final RegistryObject<Block> LIFE_ECHO =
            BLOCKS.register("life_echo",
                    () -> new Block(BlockBehaviour.Properties.of()
                            .strength(1.5f)
                            .sound(SoundType.AMETHYST)
                            .lightLevel(state -> 8)
                            .noOcclusion()
                    ));
}
