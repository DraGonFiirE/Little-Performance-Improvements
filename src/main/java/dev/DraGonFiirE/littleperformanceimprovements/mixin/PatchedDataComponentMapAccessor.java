package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.util.Optional;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PatchedDataComponentMap.class)
public interface PatchedDataComponentMapAccessor {
   @Accessor("patch")
   Reference2ObjectMap<DataComponentType<?>, Optional<?>> littleperformanceimprovements$getPatch();
}
