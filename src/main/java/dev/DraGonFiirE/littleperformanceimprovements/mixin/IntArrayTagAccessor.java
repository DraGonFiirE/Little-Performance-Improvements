package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import net.minecraft.nbt.IntArrayTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IntArrayTag.class)
public interface IntArrayTagAccessor {
   @Accessor("data")
   int[] littleperformanceimprovements$getData();
}
